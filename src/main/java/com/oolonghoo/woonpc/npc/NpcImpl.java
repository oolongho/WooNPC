package com.oolonghoo.woonpc.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.scores.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.oolonghoo.woonpc.util.ColorUtil;
import com.oolonghoo.woonpc.util.PlaceholderUtil;
import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.version.VersionAdapter;
import com.oolonghoo.woonpc.version.VersionAdapterFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 纯数据包 NPC 实现
 * <p>
 * 不创建真实实体对象，仅通过数据包模拟 NPC 行为。
 * 所有操作都通过 VersionAdapter 发送数据包完成。
 * </p>
 *
 * @author oolongho
 * @since 2.0.0
 */
public class NpcImpl extends Npc {
    
    // 实体 ID 生成器（使用负数避免与真实实体冲突）
    private static final AtomicInteger ENTITY_ID_GENERATOR = new AtomicInteger(Integer.MAX_VALUE - 100000);
    
    // 实体 ID（由服务端分配）
    private int entityId;
    
    // NPC UUID
    private final UUID uuid;
    
    // 本地名称（用于玩家列表）
    private final String localName;
    
    // GameProfile（用于玩家型 NPC）
    private GameProfile gameProfile;
    
    // 坐骑实体（用于坐姿）
    private Display.TextDisplay sittingVehicle;
    private int sittingVehicleId = -1;
    
    // 当前姿势
    private Pose currentPose = Pose.STANDING;
    
    // 当前朝向
    private float currentYaw = 0f;
    private float currentPitch = 0f;
    
    // 区块可见性缓存 (Player UUID -> Chunk Key -> Boolean)
    private final Map<UUID, Map<Long, Boolean>> chunkVisibilityCache = createLRUCache(500);
    
    // 缓存失效计数器
    private int cacheInvalidationCounter = 0;
    private static final int CACHE_INVALIDATION_INTERVAL = 100;
    
    // 实体数据访问器缓存（由 EntityMetadata 管理）
    // 注：反射缓存已移至 AbstractVersionAdapter
    
    /**
     * 创建两级 LRU 缓存（外层 LRU，内层并发）
     */
    private static <K, V> Map<K, Map<Long, V>> createLRUCache(int maxSize) {
        return new java.util.LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<K, Map<Long, V>> eldest) {
                return size() > maxSize;
            }
        };
    }
    
    public NpcImpl(NpcData data) {
        super(data);
        this.localName = generateLocalName();
        this.uuid = UUID.randomUUID();
        
        // 注册可见性变化监听器
        addVisibilityChangeListener(this::onVisibilityChange);
    }
    
    /**
     * 可见性变化回调
     */
    @SuppressWarnings("java:S1172")
    private void onVisibilityChange(Npc _npc, Player _player, boolean visible, int visiblePlayerCount) {
        WooNPC plugin = WooNPC.getInstance();
        if (plugin == null) return;

        UUID npcId = UUID.fromString(data.getId());

        if (visible) {
            plugin.getNpcManager().addToTickableIndex(npcId);
        } else if (visiblePlayerCount == 0) {
            plugin.getNpcManager().removeFromTickableIndex(npcId);
        }
    }
    
    /**
     * 获取版本适配器
     */
    private VersionAdapter getAdapter() {
        return VersionAdapterFactory.getAdapter();
    }
    
    @Override
    public void create() {
        // 空值检查
        if (data == null) {
            throw new IllegalStateException("NPC data is null");
        }
        
        Location location = data.getLocation();
        if (location == null) {
            throw new IllegalStateException("NPC location is null for: " + data.getName());
        }
        
        org.bukkit.World bukkitWorld = location.getWorld();
        if (bukkitWorld == null) {
            throw new IllegalStateException("NPC world is null for: " + data.getName());
        }
        
        // 分配实体 ID（使用负数避免与真实实体冲突）
        this.entityId = ENTITY_ID_GENERATOR.decrementAndGet();
        
        // 创建 GameProfile（用于玩家型 NPC）
        this.gameProfile = new GameProfile(uuid, localName);
        
        // 设置初始朝向
        if (location != null) {
            this.currentYaw = location.getYaw();
            this.currentPitch = location.getPitch();
        }
        
        // 清除团队状态
        isTeamCreated.clear();
    }
    
    /**
     * 应用皮肤到 GameProfile
     * 使用 VersionAdapter 提供的方法
     */
    private GameProfile applySkinToProfile(GameProfile profile, String value, String signature) {
        VersionAdapter adapter = getAdapter();
        UUID profileId = adapter.getProfileId(profile);
        String profileName = adapter.getProfileName(profile);
        
        if (value == null || value.isEmpty()) {
            return new GameProfile(profileId != null ? profileId : uuid, profileName);
        }
        
        return adapter.createGameProfileWithSkin(
                profileId != null ? profileId : uuid,
                profileName,
                value,
                signature
        );
    }
    
    @Override
    public void spawn(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        // 检查世界
        if (!data.getLocation().getWorld().getName().equalsIgnoreCase(player.getWorld().getName())) {
            return;
        }
        
        VersionAdapter adapter = getAdapter();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        
        // 准备 GameProfile
        GameProfile spawnProfile = this.gameProfile;
        if (data.getType() == org.bukkit.entity.EntityType.PLAYER) {
            if (data.isSkinMirror()) {
                // 镜像皮肤 - 使用 VersionAdapter 获取属性
                PropertyMap viewerProperties = adapter.getProfileProperties(serverPlayer.getGameProfile());
                spawnProfile = adapter.createGameProfileWithSkin(uuid, localName, 
                        viewerProperties != null && !viewerProperties.isEmpty() 
                                ? viewerProperties.get("textures").stream().findFirst().map(Property::value).orElse(null)
                                : null,
                        viewerProperties != null && !viewerProperties.isEmpty()
                                ? viewerProperties.get("textures").stream().findFirst().map(Property::signature).orElse(null)
                                : null
                );
            } else if (data.getSkinValue() != null && !data.getSkinValue().isEmpty()) {
                // 自定义皮肤
                spawnProfile = applySkinToProfile(this.gameProfile, data.getSkinValue(), data.getSkinSignature());
            }
        }
        
        // 玩家型 NPC：先发送 PlayerInfoAdd
        if (data.getType() == org.bukkit.entity.EntityType.PLAYER) {
            Packet<? super ClientGamePacketListener> playerInfoPacket = createPlayerInfoAddPacket(spawnProfile);
            packets.add(playerInfoPacket);
        }
        
        // 发送 AddEntity 数据包
        Packet<? super ClientGamePacketListener> addEntityPacket = adapter.createAddEntityPacket(
                entityId,
                uuid,
                data.getLocation(),
                data.getType()
        );
        packets.add(addEntityPacket);
        
        // 标记为可见
        isVisibleForPlayer.put(player.getUniqueId(), true);
        
        // 如果不在 Tab 显示，延迟移除 PlayerInfo
        if (data.getType() == org.bukkit.entity.EntityType.PLAYER && !data.isShowInTab()) {
            org.bukkit.plugin.Plugin plugin = java.util.Objects.requireNonNull(
                    Bukkit.getPluginManager().getPlugin("WooNPC"), 
                    "WooNPC plugin instance cannot be null"
            );
            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> {
                        Packet<?> playerInfoRemovePacket = adapter.createPlayerInfoRemovePacket(uuid);
                        adapter.sendPacket(player, playerInfoRemovePacket);
                    },
                    1L
            );
        }
        
        // 发送数据包
        adapter.sendPackets(player, packets);
        
        // 更新实体数据
        update(player);
        
        // 触发可见性变化事件
        fireVisibilityChangeEvent(player, true);
    }
    
    /**
     * 创建玩家信息添加数据包
     */
    private Packet<? super ClientGamePacketListener> createPlayerInfoAddPacket(GameProfile profile) {
        EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
        actions.add(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);
        actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
        
        if (data.isShowInTab()) {
            actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED);
        }
        
        Component displayName = Component.literal(localName);
        ClientboundPlayerInfoUpdatePacket.Entry entry = createPlayerInfoEntry(profile, displayName);
        
        return new ClientboundPlayerInfoUpdatePacket(actions, List.of(entry));
    }
    
    /**
     * 创建玩家信息条目
     */
    private ClientboundPlayerInfoUpdatePacket.Entry createPlayerInfoEntry(GameProfile profile, Component displayName) {
        return new ClientboundPlayerInfoUpdatePacket.Entry(
                uuid,
                profile,
                data.isShowInTab(),
                0,
                net.minecraft.world.level.GameType.SURVIVAL,
                displayName,
                true,
                -1,
                null
        );
    }
    
    @Override
    public void remove(Player player) {
        VersionAdapter adapter = getAdapter();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        
        // 玩家型 NPC：移除 PlayerInfo
        if (data.getType() == org.bukkit.entity.EntityType.PLAYER) {
            packets.add(adapter.createPlayerInfoRemovePacket(uuid));
        }
        
        // 移除实体
        packets.add(adapter.createRemoveEntityPacket(entityId));
        
        // 移除坐骑
        if (sittingVehicleId >= 0) {
            packets.add(adapter.createRemoveEntityPacket(sittingVehicleId));
        }
        
        adapter.sendPackets(player, packets);
        
        boolean wasVisible = isVisibleForPlayer.getOrDefault(player.getUniqueId(), false);
        isVisibleForPlayer.put(player.getUniqueId(), false);
        
        if (wasVisible) {
            fireVisibilityChangeEvent(player, false);
        }
    }
    
    @Override
    public void lookAt(Player player, Location location) {
        VersionAdapter adapter = getAdapter();
        
        // 更新当前朝向
        this.currentYaw = location.getYaw();
        this.currentPitch = location.getPitch();
        
        // 发送传送数据包
        Packet<?> teleportPacket = adapter.createTeleportPacket(
                entityId,
                data.getLocation(),
                location.getYaw(),
                location.getPitch()
        );
        adapter.sendPacket(player, teleportPacket);
        
        // 发送头部旋转数据包
        Packet<?> rotateHeadPacket = adapter.createRotateHeadPacket(entityId, location.getYaw());
        adapter.sendPacket(player, rotateHeadPacket);
    }
    
    @Override
    public void update(Player player, boolean swingArm) {
        if (!isVisibleForPlayer.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        
        VersionAdapter adapter = getAdapter();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        
        // 处理显示名称
        String displayNameStr = PlaceholderUtil.setPlaceholder(player, data.getDisplayName());
        net.kyori.adventure.text.Component displayName = ColorUtil.toComponent(displayNameStr);
        Component vanillaComponent = PaperAdventure.asVanilla(displayName);
        
        // 处理团队（用于发光和名称标签）
        GlowingColor glowingColor = data.getGlowingColor();
        boolean isGlowing = data.isGlowing();
        boolean shouldCreateTeam = !glowingColor.isDisabled() || !data.getDisplayName().equalsIgnoreCase("<empty>");
        
        if (shouldCreateTeam) {
            String teamName = "npc-" + uuid.toString().substring(0, 8);
            String teamMember = data.getType() == org.bukkit.entity.EntityType.PLAYER 
                    ? localName 
                    : uuid.toString();
            
            org.bukkit.ChatColor teamColor = org.bukkit.ChatColor.WHITE;
            if (isGlowing && !glowingColor.isDisabled()) {
                // 使用 GlowingColor 的配置名称转换为 Bukkit ChatColor
                String configName = glowingColor.getConfigName();
                // 将配置名称转换为大写，并替换下划线（如 dark_blue -> DARK_BLUE）
                String chatColorName = configName.toUpperCase();
                try {
                    teamColor = org.bukkit.ChatColor.valueOf(chatColorName);
                } catch (IllegalArgumentException e) {
                    teamColor = org.bukkit.ChatColor.WHITE;
                }
            }
            
            Team.Visibility nameTagVisibility = data.getDisplayName().equalsIgnoreCase("<empty>") 
                    ? Team.Visibility.NEVER 
                    : Team.Visibility.ALWAYS;
            
            Packet<? super ClientGamePacketListener> teamPacket = adapter.createTeamPacket(
                    teamName,
                    teamMember,
                    teamColor,
                    !isTeamCreated.getOrDefault(player.getUniqueId(), false),
                    Team.CollisionRule.NEVER,
                    nameTagVisibility
            );
            packets.add(teamPacket);
            isTeamCreated.put(player.getUniqueId(), true);
        }
        
        // 玩家型 NPC：更新 PlayerInfo
        if (data.getType() == org.bukkit.entity.EntityType.PLAYER) {
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = 
                    EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
            actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
            
            if (data.isShowInTab()) {
                actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED);
            }
            
            GameProfile profile = this.gameProfile;
            if (data.isSkinMirror()) {
                ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
                PropertyMap viewerProperties = adapter.getProfileProperties(serverPlayer.getGameProfile());
                profile = adapter.createGameProfileWithSkin(uuid, localName,
                        viewerProperties != null && !viewerProperties.isEmpty()
                                ? viewerProperties.get("textures").stream().findFirst().map(Property::value).orElse(null)
                                : null,
                        viewerProperties != null && !viewerProperties.isEmpty()
                                ? viewerProperties.get("textures").stream().findFirst().map(Property::signature).orElse(null)
                                : null
                );
            }
            
            ClientboundPlayerInfoUpdatePacket.Entry entry = createPlayerInfoEntry(profile, vanillaComponent);
            packets.add(new ClientboundPlayerInfoUpdatePacket(actions, List.of(entry)));
        }
        
        // 发送装备
        if (data.getEquipment() != null && !data.getEquipment().isEmpty()) {
            Map<EquipmentSlot, org.bukkit.inventory.ItemStack> equipment = new HashMap<>();
            for (Map.Entry<NpcEquipmentSlot, org.bukkit.inventory.ItemStack> entry : data.getEquipment().entrySet()) {
                equipment.put(EquipmentSlot.byName(entry.getKey().getNmsName()), entry.getValue());
            }
            packets.add(adapter.createSetEquipmentPacket(entityId, equipment));
        }
        
        // 更新实体数据
        refreshEntityData(player);
        
        // 移动
        if (data.getLocation() != null) {
            move(player, swingArm);
        }
        
        // 坐姿
        if ("sitting".equals(data.getPose())) {
            setSitting(player);
        } else if (sittingVehicleId >= 0) {
            packets.add(adapter.createRemoveEntityPacket(sittingVehicleId));
            sittingVehicleId = -1;
        }
        
        // 发送数据包
        if (!packets.isEmpty()) {
            adapter.sendPackets(player, packets);
        }
    }
    
    /**
     * 更新实体数据
     * 使用 EntityMetadata 构建完整的实体元数据
     */
    private List<SynchedEntityData.DataValue<?>> buildEntityDataValues() {
        // 使用 EntityMetadata 构建元数据
        return EntityMetadata.buildMetadata(data, entityId);
    }
    
    @Override
    protected void refreshEntityData(Player player) {
        if (!isVisibleForPlayer.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        
        VersionAdapter adapter = getAdapter();
        
        // 获取实体数据项
        List<SynchedEntityData.DataValue<?>> dataItems = buildEntityDataValues();
        
        if (!dataItems.isEmpty()) {
            Packet<?> setEntityDataPacket = adapter.createSetEntityDataPacket(entityId, dataItems);
            adapter.sendPacket(player, setEntityDataPacket);
        }
    }
    
    @Override
    public void move(Player player, boolean swingArm) {
        VersionAdapter adapter = getAdapter();
        
        // 更新当前朝向
        if (data.getLocation() != null) {
            this.currentYaw = data.getLocation().getYaw();
            this.currentPitch = data.getLocation().getPitch();
        }
        
        // 发送传送数据包
        Packet<?> teleportPacket = adapter.createTeleportPacket(
                entityId,
                data.getLocation(),
                currentYaw,
                currentPitch
        );
        adapter.sendPacket(player, teleportPacket);
        
        // 发送头部旋转数据包
        Packet<?> rotateHeadPacket = adapter.createRotateHeadPacket(entityId, currentYaw);
        adapter.sendPacket(player, rotateHeadPacket);
        
        // 挥动手臂
        if (swingArm && data.getType() == org.bukkit.entity.EntityType.PLAYER) {
            Packet<?> animatePacket = adapter.createAnimatePacket(entityId, 0);
            adapter.sendPacket(player, animatePacket);
        }
    }
    
    /**
     * 设置坐姿
     */
    private void setSitting(Player player) {
        if (sittingVehicleId < 0) {
            // 创建坐骑实体 ID
            sittingVehicleId = ENTITY_ID_GENERATOR.decrementAndGet();
            
            sittingVehicle = new Display.TextDisplay(
                    EntityType.TEXT_DISPLAY, 
                    ((CraftWorld) data.getLocation().getWorld()).getHandle()
            );
        }
        
        VersionAdapter adapter = getAdapter();
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        // 发送坐骑实体
        ServerLevel level = adapter.getServerLevel(serverPlayer);
        if (level == null) return;
        
        ServerEntity serverEntity = new ServerEntity(
                level,
                sittingVehicle,
                0,
                false,
                FakeSynchronizer.INSTANCE,
                Set.of()
        );
        
        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(sittingVehicle, serverEntity);
        adapter.sendPacket(player, addEntityPacket);
        
        // 设置乘客
        ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(sittingVehicle);
        adapter.sendPacket(player, packet);
    }
    
    @Override
    public float getEyeHeight() {
        // 根据实体类型返回默认眼睛高度
        if (data.getType() == org.bukkit.entity.EntityType.PLAYER) {
            return 1.62f;
        }
        // 其他实体类型的默认高度
        return 1.0f;
    }
    
    @Override
    public int getEntityId() {
        return entityId;
    }
    
    public UUID getNpcUuid() {
        return uuid;
    }
    
    public void setEquipment(NpcEquipmentSlot slot, org.bukkit.inventory.ItemStack item) {
        data.addEquipment(slot, item);
        updateForAll();
    }
    
    public void removeEquipment(NpcEquipmentSlot slot) {
        data.removeEquipment(slot);
        updateForAll();
    }
    
    public void clearEquipment() {
        data.setEquipment(new java.util.HashMap<>());
        updateForAll();
    }
    
    public void setPose(NpcPose pose) {
        if (pose == NpcPose.SITTING) {
            setSittingState(true);
        } else {
            setSittingState(false);
        }
        
        data.setPose(pose.getConfigName());
        updateForAll();
    }
    
    private void setSittingState(boolean sitting) {
        if (!sitting && sittingVehicleId >= 0) {
            VersionAdapter adapter = getAdapter();
            for (Map.Entry<UUID, Boolean> entry : isVisibleForPlayer.entrySet()) {
                if (entry.getValue()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        adapter.sendPacket(player, adapter.createRemoveEntityPacket(sittingVehicleId));
                    }
                }
            }
            sittingVehicleId = -1;
            sittingVehicle = null;
        }
    }
    
    public NpcPose getPose() {
        return NpcPose.fromConfigName(data.getPose());
    }
    
    @Override
    protected boolean shouldBeVisible(Player player) {
        if (data.getLocation() == null) {
            return false;
        }
        
        if (player.getLocation().getWorld() != data.getLocation().getWorld()) {
            return false;
        }
        
        int visibilityDistance = data.getVisibilityDistance();
        if (visibilityDistance > 0) {
            double distanceSquared = data.getLocation().distanceSquared(player.getLocation());
            if (distanceSquared > visibilityDistance * visibilityDistance) {
                return false;
            }
        }
        
        int chunkX = ((int) data.getLocation().getX()) >> 4;
        int chunkZ = ((int) data.getLocation().getZ()) >> 4;

        return isChunkVisible(player, chunkX, chunkZ);
    }
    
    protected boolean isChunkVisible(Player player, int chunkX, int chunkZ) {
        if (player == null || data.getLocation() == null || data.getLocation().getWorld() == null) {
            return false;
        }
        
        org.bukkit.World world = data.getLocation().getWorld();
        
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return false;
        }
        
        return getAdapter().isChunkSentToClient(player, chunkX, chunkZ);
    }
    
    public void clearChunkVisibilityCache(UUID playerId) {
        chunkVisibilityCache.remove(playerId);
    }
    
    public void clearAllChunkVisibilityCache() {
        chunkVisibilityCache.clear();
    }

    /**
     * 清理指定玩家的所有缓存数据
     * 重写父类方法，额外清理 chunkVisibilityCache
     *
     * @param playerId 玩家 UUID
     */
    @Override
    public void cleanupPlayer(UUID playerId) {
        // 调用父类方法清理基础缓存
        super.cleanupPlayer(playerId);

        // 清理区块可见性缓存
        chunkVisibilityCache.remove(playerId);

        // 记录调试信息
        if (WooNPC.getInstance() != null && WooNPC.getInstance().getConfigLoader().isDebug()) {
            WooNPC.getInstance().getLogger().info(() -> "[NpcImpl] 已清理玩家缓存: " + playerId +
                    ", NPC: " + data.getName());
        }
    }
}
