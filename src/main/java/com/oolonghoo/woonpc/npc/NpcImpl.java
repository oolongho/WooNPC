package com.oolonghoo.woonpc.npc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.Optionull;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Player;

import com.oolonghoo.woonpc.util.PlaceholderUtil;
import com.oolonghoo.woonpc.WooNPC;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NpcImpl extends Npc {
    
    private Entity npc;
    private final String localName;
    private final UUID uuid;
    private Display.TextDisplay sittingVehicle;
    
    // 区块可见性缓存 (Player UUID -> Chunk Key -> Boolean)
    private final Map<UUID, Map<Long, Boolean>> chunkVisibilityCache = new ConcurrentHashMap<>();
    
    // 缓存失效计数器
    private int cacheInvalidationCounter = 0;
    private static final int CACHE_INVALIDATION_INTERVAL = 100; // 每 100 次检查清理一次缓存
    
    private static Method gameProfileNameMethod;
    private static Method gameProfileIdMethod;
    private static Method gameProfilePropertiesMethod;
    private static Constructor<?> gameProfileConstructorWithProps;
    private static Constructor<?> propertyMapConstructor;
    private static Method serverPlayerLevelMethod;
    private static Constructor<?> playerInfoEntryConstructor;
    private static boolean initialized = false;
    
    static {
        initReflection();
    }
    
    private static synchronized void initReflection() {
        if (initialized) return;
        initialized = true;
        
        try {
            gameProfileNameMethod = GameProfile.class.getMethod("getName");
        } catch (NoSuchMethodException e) {
            try {
                gameProfileNameMethod = GameProfile.class.getMethod("name");
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
        try {
            gameProfileIdMethod = GameProfile.class.getMethod("getId");
        } catch (NoSuchMethodException e) {
            try {
                gameProfileIdMethod = GameProfile.class.getMethod("id");
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
        try {
            gameProfilePropertiesMethod = GameProfile.class.getMethod("getProperties");
        } catch (NoSuchMethodException e) {
            try {
                gameProfilePropertiesMethod = GameProfile.class.getMethod("properties");
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
        try {
            gameProfileConstructorWithProps = GameProfile.class.getConstructor(UUID.class, String.class, PropertyMap.class);
        } catch (NoSuchMethodException e) {
            gameProfileConstructorWithProps = null;
        }
        try {
            propertyMapConstructor = PropertyMap.class.getConstructor(ImmutableMultimap.class);
        } catch (NoSuchMethodException e) {
            propertyMapConstructor = null;
        }
        try {
            serverPlayerLevelMethod = ServerPlayer.class.getMethod("serverLevel");
        } catch (NoSuchMethodException e) {
            try {
                serverPlayerLevelMethod = ServerPlayer.class.getMethod("level");
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
        try {
            playerInfoEntryConstructor = ClientboundPlayerInfoUpdatePacket.Entry.class.getConstructor(
                    UUID.class, GameProfile.class, boolean.class, int.class, 
                    net.minecraft.world.level.GameType.class, Component.class, 
                    boolean.class, int.class, net.minecraft.network.chat.RemoteChatSession.Data.class
            );
        } catch (NoSuchMethodException e) {
            playerInfoEntryConstructor = null;
            for (Constructor<?> constructor : ClientboundPlayerInfoUpdatePacket.Entry.class.getConstructors()) {
                if (constructor.getParameterCount() >= 6) {
                    playerInfoEntryConstructor = constructor;
                    break;
                }
            }
        }
    }
    
    private static String getProfileName(GameProfile profile) {
        if (profile == null) return "";
        try {
            if (gameProfileNameMethod != null) {
                return (String) gameProfileNameMethod.invoke(profile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private static UUID getProfileId(GameProfile profile) {
        if (profile == null) return null;
        try {
            if (gameProfileIdMethod != null) {
                return (UUID) gameProfileIdMethod.invoke(profile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static PropertyMap getProfileProperties(GameProfile profile) {
        if (profile == null) return createEmptyPropertyMap();
        try {
            if (gameProfilePropertiesMethod != null) {
                return (PropertyMap) gameProfilePropertiesMethod.invoke(profile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return createEmptyPropertyMap();
    }
    
    private static ServerLevel getServerLevel(ServerPlayer player) {
        if (player == null) return null;
        try {
            if (serverPlayerLevelMethod != null) {
                return (ServerLevel) serverPlayerLevelMethod.invoke(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static GameProfile createGameProfileWithProperties(UUID uuid, String name, PropertyMap properties) {
        try {
            if (gameProfileConstructorWithProps != null) {
                return (GameProfile) gameProfileConstructorWithProps.newInstance(uuid, name, properties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 无法创建带属性的 GameProfile，返回基本的 GameProfile
        return new GameProfile(uuid, name, properties);
    }
    
    private static PropertyMap createEmptyPropertyMap() {
        try {
            if (propertyMapConstructor != null) {
                return (PropertyMap) propertyMapConstructor.newInstance(ImmutableMultimap.of());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public NpcImpl(NpcData data) {
        super(data);
        this.localName = generateLocalName();
        this.uuid = UUID.randomUUID();
        
        // 注册可见性变化监听器，用于维护 tickable 索引
        addVisibilityChangeListener(this::onVisibilityChange);
    }
    
    /**
     * 可见性变化回调
     * 当有玩家变为可见时添加到 tickable 索引，当无可见玩家时从索引移除
     */
    private void onVisibilityChange(Npc npc, Player player, boolean visible, int visiblePlayerCount) {
        WooNPC plugin = WooNPC.getInstance();
        if (plugin == null) return;
        
        UUID npcId = UUID.fromString(data.getId());
        
        if (visible) {
            // 有玩家变为可见，添加到 tickable 索引
            plugin.getNpcManager().addToTickableIndex(npcId);
        } else if (visiblePlayerCount == 0) {
            // 无可见玩家，从 tickable 索引移除
            plugin.getNpcManager().removeFromTickableIndex(npcId);
        }
    }
    
    @Override
    public void create() {
        MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel serverLevel = ((CraftWorld) data.getLocation().getWorld()).getHandle();
        GameProfile gameProfile = new GameProfile(uuid, localName);
        
        if (data.getType() == org.bukkit.entity.EntityType.PLAYER) {
            npc = new ServerPlayer(minecraftServer, serverLevel, 
                    new GameProfile(uuid, ""), ClientInformation.createDefault());
            ((ServerPlayer) npc).gameProfile = gameProfile;
        } else {
            Optional<Holder.Reference<EntityType<?>>> entityTypeReference = 
                    BuiltInRegistries.ENTITY_TYPE.get(CraftNamespacedKey.toMinecraft(data.getType().getKey()));
            if (entityTypeReference.isPresent()) {
                EntityType<?> nmsType = entityTypeReference.get().value();
                try {
                    EntityType.EntityFactory<?> factory = getFactory(nmsType);
                    if (factory != null) {
                        npc = createEntityFromFactory(factory, nmsType, serverLevel);
                    } else {
                        npc = nmsType.create(serverLevel, EntitySpawnReason.COMMAND);
                    }
                } catch (Exception e) {
                    npc = nmsType.create(serverLevel, EntitySpawnReason.COMMAND);
                }
                isTeamCreated.clear();
            }
        }
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private net.minecraft.world.entity.Entity createEntityFromFactory(EntityType.EntityFactory<?> factory, EntityType<?> type, ServerLevel level) {
        return ((EntityType.EntityFactory) factory).create(type, level);
    }
    
    private EntityType.EntityFactory<?> getFactory(EntityType<?> entityType) {
        try {
            Field factoryField = EntityType.class.getDeclaredField("factory");
            factoryField.setAccessible(true);
            return (EntityType.EntityFactory<?>) factoryField.get(entityType);
        } catch (Exception e) {
            return null;
        }
    }
    
    private void applySkin(ServerPlayer npcPlayer, String value, String signature) {
        try {
            if (value == null || value.isEmpty()) {
                // 清除皮肤：创建空的 PropertyMap
                PropertyMap propertyMap = createEmptyPropertyMap();
                if (propertyMap != null) {
                    npcPlayer.gameProfile = new GameProfile(uuid, localName, propertyMap);
                } else {
                    // 如果无法创建空的 PropertyMap，使用基本的 GameProfile
                    npcPlayer.gameProfile = new GameProfile(uuid, localName);
                }
            } else {
                // 设置皮肤
                PropertyMap propertyMap = new PropertyMap(
                        ImmutableMultimap.of(
                                "textures",
                                new Property("textures", value, signature)
                        )
                );
                npcPlayer.gameProfile = new GameProfile(uuid, localName, propertyMap);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[WooNPC] 设置皮肤失败: " + e.getMessage());
        }
    }
    
    @Override
    public void spawn(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        if (npc == null) {
            return;
        }
        
        ServerLevel playerLevel = getServerLevel(serverPlayer);
        if (playerLevel == null) {
            return;
        }
        
        if (!data.getLocation().getWorld().getName().equalsIgnoreCase(playerLevel.getWorld().getName())) {
            return;
        }
        
        if (npc instanceof ServerPlayer npcPlayer) {
            if (data.isSkinMirror()) {
                PropertyMap viewerProperties = getProfileProperties(serverPlayer.getGameProfile());
                String profileName = getProfileName(npcPlayer.getGameProfile());
                UUID profileId = getProfileId(npcPlayer.getGameProfile());
                GameProfile mirroredProfile = createGameProfileWithProperties(
                        profileId != null ? profileId : uuid, profileName, viewerProperties);
                npcPlayer.gameProfile = mirroredProfile;
            } else if (data.getSkinValue() != null && !data.getSkinValue().isEmpty()) {
                applySkin(npcPlayer, data.getSkinValue(), data.getSkinSignature());
            }
        }
        
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        
        if (npc instanceof ServerPlayer npcPlayer) {
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
            actions.add(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);
            actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
            
            if (data.isShowInTab()) {
                actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED);
            }
            
            ClientboundPlayerInfoUpdatePacket playerInfoPacket = 
                    new ClientboundPlayerInfoUpdatePacket(actions, getEntry(npcPlayer, serverPlayer));
            packets.add(playerInfoPacket);
            
            npc.setPos(data.getLocation().x(), data.getLocation().y(), data.getLocation().z());
        }
        
        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
                npc.getId(),
                npc.getUUID(),
                data.getLocation().x(),
                data.getLocation().y(),
                data.getLocation().z(),
                data.getLocation().getPitch(),
                data.getLocation().getYaw(),
                npc.getType(),
                0,
                Vec3.ZERO,
                data.getLocation().getYaw()
        );
        packets.add(addEntityPacket);
        
        isVisibleForPlayer.put(player.getUniqueId(), true);
        
        if (!data.isShowInTab() && npc instanceof ServerPlayer) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(
                    Bukkit.getPluginManager().getPlugin("WooNPC"),
                    () -> {
                        ClientboundPlayerInfoRemovePacket playerInfoRemovePacket = 
                                new ClientboundPlayerInfoRemovePacket(List.of(npc.getUUID()));
                        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                                () -> serverPlayer.connection.send(playerInfoRemovePacket));
                    },
                    1L
            );
        }
        
        ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packets);
        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), () -> serverPlayer.connection.send(bundlePacket));
        
        update(player);
        
        // 触发可见性变化事件（玩家变为可见）
        fireVisibilityChangeEvent(player, true);
    }
    
    @Override
    public void remove(Player player) {
        if (npc == null) {
            return;
        }
        
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        if (npc instanceof ServerPlayer) {
            ClientboundPlayerInfoRemovePacket playerInfoRemovePacket = 
                    new ClientboundPlayerInfoRemovePacket(List.of(npc.getUUID()));
            runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                    () -> serverPlayer.connection.send(playerInfoRemovePacket));
        }
        
        ClientboundRemoveEntitiesPacket removeEntitiesPacket = new ClientboundRemoveEntitiesPacket(npc.getId());
        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                () -> serverPlayer.connection.send(removeEntitiesPacket));
        
        if (sittingVehicle != null) {
            ClientboundRemoveEntitiesPacket removeSittingVehiclePacket = 
                    new ClientboundRemoveEntitiesPacket(sittingVehicle.getId());
            runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                    () -> serverPlayer.connection.send(removeSittingVehiclePacket));
        }
        
        boolean wasVisible = isVisibleForPlayer.getOrDefault(serverPlayer.getUUID(), false);
        isVisibleForPlayer.put(serverPlayer.getUUID(), false);
        
        // 触发可见性变化事件（玩家变为不可见）
        if (wasVisible) {
            fireVisibilityChangeEvent(serverPlayer.getBukkitEntity(), false);
        }
    }
    
    @Override
    public void lookAt(Player player, Location location) {
        if (npc == null) {
            return;
        }
        
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        npc.setRot(location.getYaw(), location.getPitch());
        npc.setYHeadRot(location.getYaw());
        npc.setXRot(location.getPitch());
        npc.setYRot(location.getYaw());
        
        ClientboundTeleportEntityPacket teleportEntityPacket = new ClientboundTeleportEntityPacket(
                npc.getId(),
                new PositionMoveRotation(
                        new Vec3(data.getLocation().getX(), data.getLocation().getY(), data.getLocation().getZ()),
                        Vec3.ZERO,
                        location.getYaw(),
                        location.getPitch()
                ),
                Set.of(),
                false
        );
        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                () -> serverPlayer.connection.send(teleportEntityPacket));
        
        float angleMultiplier = 256f / 360f;
        ClientboundRotateHeadPacket rotateHeadPacket = 
                new ClientboundRotateHeadPacket(npc, (byte) (location.getYaw() * angleMultiplier));
        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                () -> serverPlayer.connection.send(rotateHeadPacket));
    }
    
    @Override
    public void update(Player player, boolean swingArm) {
        if (npc == null) {
            return;
        }
        
        if (!isVisibleForPlayer.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        
        String displayNameStr = PlaceholderUtil.setPlaceholder(player, data.getDisplayName());
        net.kyori.adventure.text.Component displayName;
        if (displayNameStr.contains("&") || displayNameStr.contains("§")) {
            displayName = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(displayNameStr);
        } else {
            displayName = net.kyori.adventure.text.Component.text(displayNameStr);
        }
        Component vanillaComponent = PaperAdventure.asVanilla(displayName);
        
        if (!(npc instanceof ServerPlayer)) {
            npc.setCustomName(vanillaComponent);
            npc.setCustomNameVisible(true);
        } else {
            npc.setCustomName(null);
            npc.setCustomNameVisible(false);
        }
        
        GlowingColor glowingColor = data.getGlowingColor();
        boolean isGlowing = data.isGlowing();
        boolean shouldCreateTeam = !glowingColor.isDisabled() || !data.getDisplayName().equalsIgnoreCase("<empty>");
        
        if (shouldCreateTeam) {
            PlayerTeam team = new PlayerTeam(new Scoreboard(), "npc-" + localName);
            team.getPlayers().clear();
            team.getPlayers().add(npc instanceof ServerPlayer npcPlayer 
                    ? getProfileName(npcPlayer.getGameProfile())
                    : npc.getStringUUID());
            
            // 只有在发光启用时才设置团队颜色
            if (isGlowing && !glowingColor.isDisabled() && glowingColor.getAdventureColor() != null) {
                team.setColor(PaperAdventure.asVanilla(glowingColor.getAdventureColor()));
            } else {
                team.setColor(net.minecraft.ChatFormatting.WHITE);
            }
            
            if (data.getDisplayName().equalsIgnoreCase("<empty>")) {
                team.setNameTagVisibility(Team.Visibility.NEVER);
                npc.setCustomName(null);
                npc.setCustomNameVisible(false);
            } else {
                team.setNameTagVisibility(Team.Visibility.ALWAYS);
            }
            
            team.setCollisionRule(Team.CollisionRule.NEVER);
            
            if (npc instanceof ServerPlayer npcPlayer) {
                team.setPlayerPrefix(vanillaComponent);
                npcPlayer.listName = vanillaComponent;
            }
            
            boolean isTeamCreatedForPlayer = this.isTeamCreated.getOrDefault(player.getUniqueId(), false);
            packets.add(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, !isTeamCreatedForPlayer));
            isTeamCreated.put(player.getUniqueId(), true);
        }
        
        if (npc instanceof ServerPlayer npcPlayer) {
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = 
                    EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
            actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
            
            if (data.isShowInTab()) {
                actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED);
            }
            
            ClientboundPlayerInfoUpdatePacket playerInfoPacket = 
                    new ClientboundPlayerInfoUpdatePacket(actions, getEntry(npcPlayer, serverPlayer));
            packets.add(playerInfoPacket);
        }
        
        npc.setGlowingTag(data.isGlowing() && !glowingColor.isDisabled());
        
        List<Pair<EquipmentSlot, ItemStack>> equipmentList = new ArrayList<>();
        if (data.getEquipment() != null) {
            for (Map.Entry<NpcEquipmentSlot, org.bukkit.inventory.ItemStack> entry : data.getEquipment().entrySet()) {
                equipmentList.add(new Pair<>(
                        EquipmentSlot.byName(entry.getKey().getNmsName()),
                        CraftItemStack.asNMSCopy(entry.getValue())
                ));
            }
        }
        
        if (!equipmentList.isEmpty()) {
            ClientboundSetEquipmentPacket setEquipmentPacket = 
                    new ClientboundSetEquipmentPacket(npc.getId(), equipmentList);
            packets.add(setEquipmentPacket);
        }
        
        if (npc instanceof ServerPlayer) {
            npc.getEntityData().set(
                    net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION, 
                    (byte) (0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40)
            );
        }
        
        refreshEntityData(player);
        
        if (data.getLocation() != null) {
            move(player, swingArm);
        }
        
        if ("sitting".equals(data.getPose())) {
            setSitting(serverPlayer);
        } else if (sittingVehicle != null) {
            ClientboundRemoveEntitiesPacket removeSittingVehiclePacket = 
                    new ClientboundRemoveEntitiesPacket(sittingVehicle.getId());
            packets.add(removeSittingVehiclePacket);
        }
        
        if (npc instanceof LivingEntity) {
            // Apply scale attribute
            applyScale();
        }
        
        // Apply effects
        applyEffects();
        
        ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packets);
        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), () -> serverPlayer.connection.send(bundlePacket));
    }
    
    @Override
    protected void refreshEntityData(Player player) {
        if (!isVisibleForPlayer.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        SynchedEntityData.DataItem<?>[] itemsById = getItemsById(npc.getEntityData());
        List<SynchedEntityData.DataValue<?>> entityData = new ArrayList<>();
        for (SynchedEntityData.DataItem<?> dataItem : itemsById) {
            entityData.add(dataItem.value());
        }
        
        ClientboundSetEntityDataPacket setEntityDataPacket = 
                new ClientboundSetEntityDataPacket(npc.getId(), entityData);
        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                () -> serverPlayer.connection.send(setEntityDataPacket));
    }
    
    private SynchedEntityData.DataItem<?>[] getItemsById(SynchedEntityData synchedEntityData) {
        try {
            Field itemsByIdField = SynchedEntityData.class.getDeclaredField("itemsById");
            itemsByIdField.setAccessible(true);
            return (SynchedEntityData.DataItem<?>[]) itemsByIdField.get(synchedEntityData);
        } catch (Exception e) {
            return new SynchedEntityData.DataItem<?>[0];
        }
    }
    
    @Override
    public void move(Player player, boolean swingArm) {
        if (npc == null) {
            return;
        }
        
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        npc.setPosRaw(data.getLocation().x(), data.getLocation().y(), data.getLocation().z());
        npc.setRot(data.getLocation().getYaw(), data.getLocation().getPitch());
        npc.setYHeadRot(data.getLocation().getYaw());
        npc.setXRot(data.getLocation().getPitch());
        npc.setYRot(data.getLocation().getYaw());
        
        ClientboundTeleportEntityPacket teleportEntityPacket = new ClientboundTeleportEntityPacket(
                npc.getId(),
                new PositionMoveRotation(
                        new Vec3(data.getLocation().getX(), data.getLocation().getY(), data.getLocation().getZ()),
                        Vec3.ZERO,
                        data.getLocation().getYaw(),
                        data.getLocation().getPitch()
                ),
                Set.of(),
                false
        );
        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                () -> serverPlayer.connection.send(teleportEntityPacket));
        
        float angleMultiplier = 256f / 360f;
        ClientboundRotateHeadPacket rotateHeadPacket = 
                new ClientboundRotateHeadPacket(npc, (byte) (data.getLocation().getYaw() * angleMultiplier));
        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                () -> serverPlayer.connection.send(rotateHeadPacket));
        
        if (swingArm && npc instanceof ServerPlayer) {
            ClientboundAnimatePacket animatePacket = new ClientboundAnimatePacket(npc, 0);
            runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                    () -> serverPlayer.connection.send(animatePacket));
        }
    }
    
    private ClientboundPlayerInfoUpdatePacket.Entry getEntry(ServerPlayer npcPlayer, ServerPlayer viewer) {
        GameProfile profile = npcPlayer.getGameProfile();
        
        if (data.isSkinMirror()) {
            String profileName = getProfileName(profile);
            UUID profileId = getProfileId(profile);
            PropertyMap viewerProperties = getProfileProperties(viewer.getGameProfile());
            profile = createGameProfileWithProperties(profileId != null ? profileId : uuid, profileName, viewerProperties);
        }
        
        return createPlayerInfoEntry(npcPlayer, profile);
    }
    
    private ClientboundPlayerInfoUpdatePacket.Entry createPlayerInfoEntry(ServerPlayer npcPlayer, GameProfile profile) {
        try {
            return new ClientboundPlayerInfoUpdatePacket.Entry(
                    npcPlayer.getUUID(),
                    profile,
                    data.isShowInTab(),
                    0,
                    npcPlayer.gameMode.getGameModeForPlayer(),
                    npcPlayer.getTabListDisplayName(),
                    true,
                    -1,
                    Optionull.map(npcPlayer.getChatSession(), RemoteChatSession::asData)
            );
        } catch (Exception e) {
            return createPlayerInfoEntryFallback(npcPlayer, profile);
        }
    }
    
    private ClientboundPlayerInfoUpdatePacket.Entry createPlayerInfoEntryFallback(ServerPlayer npcPlayer, GameProfile profile) {
        try {
            if (playerInfoEntryConstructor != null) {
                Class<?>[] paramTypes = playerInfoEntryConstructor.getParameterTypes();
                Object[] args = new Object[paramTypes.length];
                args[0] = npcPlayer.getUUID();
                args[1] = profile;
                args[2] = data.isShowInTab();
                args[3] = 0;
                args[4] = npcPlayer.gameMode.getGameModeForPlayer();
                args[5] = npcPlayer.getTabListDisplayName();
                if (paramTypes.length > 6) {
                    args[6] = true;
                }
                if (paramTypes.length > 7) {
                    args[7] = -1;
                }
                if (paramTypes.length > 8) {
                    args[8] = Optionull.map(npcPlayer.getChatSession(), RemoteChatSession::asData);
                }
                return (ClientboundPlayerInfoUpdatePacket.Entry) playerInfoEntryConstructor.newInstance(args);
            }
        } catch (Exception ex) {
        }
        return null;
    }
    
    private void setSitting(ServerPlayer serverPlayer) {
        if (npc == null) {
            return;
        }
        
        if (sittingVehicle == null) {
            sittingVehicle = new Display.TextDisplay(
                    EntityType.TEXT_DISPLAY, 
                    ((CraftWorld) data.getLocation().getWorld()).getHandle()
            );
        }
        
        sittingVehicle.setPos(data.getLocation().x(), data.getLocation().y(), data.getLocation().z());
        
        ServerLevel level = getServerLevel(serverPlayer);
        if (level == null) {
            return;
        }
        
        ServerEntity serverEntity = new ServerEntity(
                level,
                sittingVehicle,
                0,
                false,
                FakeSynchronizer.INSTANCE,
                Set.of()
        );
        
        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(sittingVehicle, serverEntity);
        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                () -> serverPlayer.connection.send(addEntityPacket));
        
        sittingVehicle.passengers = ImmutableList.of(npc);
        
        ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(sittingVehicle);
        runOnPlayerScheduler(serverPlayer.getBukkitEntity(), 
                () -> serverPlayer.connection.send(packet));
    }
    
    @Override
    public float getEyeHeight() {
        return npc != null ? npc.getEyeHeight() : 1.62f;
    }
    
    @Override
    public int getEntityId() {
        return npc != null ? npc.getId() : -1;
    }
    
    public Entity getNmsEntity() {
        return npc;
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
    
    private static EntityDataAccessor<Pose> DATA_POSE_ACCESSOR = null;
    
    public void setPose(NpcPose pose) {
        if (npc == null) {
            return;
        }
        
        if (pose == NpcPose.SITTING) {
            setSittingState(true);
        } else {
            setSittingState(false);
            setEntityPose(Pose.valueOf(pose.name()));
        }
        
        data.setPose(pose.getConfigName());
        updateForAll();
    }
    
    private void setEntityPose(Pose pose) {
        if (npc == null) {
            return;
        }
        
        try {
            if (DATA_POSE_ACCESSOR == null) {
                Field dataPoseField = Entity.class.getDeclaredField("DATA_POSE");
                dataPoseField.setAccessible(true);
                @SuppressWarnings("unchecked")
                EntityDataAccessor<Pose> accessor = (EntityDataAccessor<Pose>) dataPoseField.get(null);
                DATA_POSE_ACCESSOR = accessor;
            }
            
            npc.getEntityData().set(DATA_POSE_ACCESSOR, pose);
        } catch (Exception e) {
            try {
                npc.setPose(pose);
            } catch (Exception ignored) {
            }
        }
    }
    
    private void setSittingState(boolean sitting) {
        if (!sitting) {
            if (sittingVehicle != null) {
                for (Map.Entry<UUID, Boolean> entry : isVisibleForPlayer.entrySet()) {
                    if (entry.getValue()) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
                            ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
                            ClientboundRemoveEntitiesPacket removePacket = 
                                    new ClientboundRemoveEntitiesPacket(sittingVehicle.getId());
                            runOnPlayerScheduler(player, 
                                    () -> serverPlayer.connection.send(removePacket));
                        }
                    }
                }
                sittingVehicle = null;
            }
        }
    }
    
    public NpcPose getPose() {
        return NpcPose.fromConfigName(data.getPose());
    }
    
    private void applyScale() {
        if (!(npc instanceof LivingEntity)) {
            return;
        }
        
        float scale = data.getScale();
        if (scale == 1.0f) {
            return;
        }
        
        try {
            Holder.Reference<net.minecraft.world.entity.ai.attributes.Attribute> scaleAttribute = 
                    BuiltInRegistries.ATTRIBUTE.get(Identifier.parse("minecraft:scale")).get();
            
            if (scaleAttribute != null) {
                net.minecraft.world.entity.ai.attributes.AttributeInstance attributeInstance = 
                        new net.minecraft.world.entity.ai.attributes.AttributeInstance(scaleAttribute, (a) -> {});
                attributeInstance.setBaseValue(scale);
                
                ClientboundUpdateAttributesPacket updateAttributesPacket = 
                        new ClientboundUpdateAttributesPacket(npc.getId(), List.of(attributeInstance));
                
                for (Map.Entry<UUID, Boolean> entry : isVisibleForPlayer.entrySet()) {
                    if (entry.getValue()) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
                            ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
                            runOnPlayerScheduler(player, 
                                    () -> serverPlayer.connection.send(updateAttributesPacket));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[WooNPC] Failed to apply scale: " + e.getMessage());
        }
    }
    
    private void applyEffects() {
        Set<NpcEffect> effects = data.getEffects();
        if (effects.isEmpty()) {
            return;
        }
        
        for (NpcEffect effect : effects) {
            switch (effect) {
                case ON_FIRE:
                    npc.setSharedFlagOnFire(true);
                    break;
                case INVISIBLE:
                    npc.setInvisible(true);
                    break;
                case SHAKING:
                    npc.setTicksFrozen(npc.getTicksRequiredToFreeze());
                    break;
                case SILENT:
                    npc.setSilent(true);
                    break;
            }
        }
    }
    
    private void runOnPlayerScheduler(Player player, Runnable task) {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            player.getScheduler().run(
                    Bukkit.getPluginManager().getPlugin("WooNPC"),
                    (t) -> task.run(),
                    null
            );
        } catch (ClassNotFoundException e) {
            task.run();
        }
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
        
        if (!isChunkVisible(player, chunkX, chunkZ)) {
            return false;
        }
        
        return true;
    }
    
    protected boolean isChunkVisible(Player player, int chunkX, int chunkZ) {
        if (player == null || data.getLocation() == null || data.getLocation().getWorld() == null) {
            return false;
        }
        
        org.bukkit.World world = data.getLocation().getWorld();
        
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return false;
        }
        
        try {
            ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
            return isChunkSentToClient(serverPlayer, chunkX, chunkZ);
        } catch (Exception e) {
            return true;
        }
    }
    
    private boolean isChunkSentToClient(ServerPlayer serverPlayer, int chunkX, int chunkZ) {
        try {
            long chunkKey = chunkX & 0xFFFFFFFFL | ((long) chunkZ & 0xFFFFFFFFL) << 32;
            
            UUID playerId = serverPlayer.getUUID();
            Map<Long, Boolean> playerCache = chunkVisibilityCache.get(playerId);
            
            if (playerCache != null) {
                Boolean cached = playerCache.get(chunkKey);
                if (cached != null) {
                    return cached;
                }
            }
            
            boolean isVisible = checkChunkSentNMS(serverPlayer, chunkX, chunkZ);
            
            chunkVisibilityCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                    .put(chunkKey, isVisible);
            
            cacheInvalidationCounter++;
            if (cacheInvalidationCounter >= CACHE_INVALIDATION_INTERVAL) {
                cacheInvalidationCounter = 0;
                invalidateChunkCache(playerId);
            }
            
            return isVisible;
        } catch (Exception e) {
            return true;
        }
    }
    
    private boolean checkChunkSentNMS(ServerPlayer serverPlayer, int chunkX, int chunkZ) {
        try {
            net.minecraft.server.level.ServerLevel serverLevel = getServerLevel(serverPlayer);
            if (serverLevel == null) {
                return true;
            }
            
            java.lang.reflect.Method getChunkProviderMethod = net.minecraft.server.level.ServerLevel.class.getMethod("getChunkProvider");
            Object chunkProvider = getChunkProviderMethod.invoke(serverLevel);
            
            if (chunkProvider == null) {
                return true;
            }
            
            Class<?> chunkProviderClass = chunkProvider.getClass();
            java.lang.reflect.Field chunkMapField = null;
            
            try {
                chunkMapField = chunkProviderClass.getDeclaredField("chunkMap");
            } catch (NoSuchFieldException e) {
                for (java.lang.reflect.Field field : chunkProviderClass.getDeclaredFields()) {
                    if (field.getType().getName().contains("ChunkMap")) {
                        chunkMapField = field;
                        break;
                    }
                }
            }
            
            if (chunkMapField == null) {
                return true;
            }
            
            chunkMapField.setAccessible(true);
            Object chunkMap = chunkMapField.get(chunkProvider);
            
            if (chunkMap == null) {
                return true;
            }
            
            Class<?> chunkMapClass = chunkMap.getClass();
            java.lang.reflect.Method getPlayersMethod = null;
            
            try {
                getPlayersMethod = chunkMapClass.getMethod("getPlayers", int.class, int.class, boolean.class);
            } catch (NoSuchMethodException e) {
                for (java.lang.reflect.Method method : chunkMapClass.getMethods()) {
                    if (method.getName().equals("getPlayers") && method.getParameterCount() == 3) {
                        getPlayersMethod = method;
                        break;
                    }
                }
            }
            
            if (getPlayersMethod == null) {
                return true;
            }
            
            Object players = getPlayersMethod.invoke(chunkMap, chunkX, chunkZ, false);
            
            if (players instanceof java.util.List<?> playerList) {
                return playerList.contains(serverPlayer);
            }
            
            return true;
        } catch (Exception e) {
            return true;
        }
    }
    
    private void invalidateChunkCache(UUID playerId) {
        Map<Long, Boolean> playerCache = chunkVisibilityCache.get(playerId);
        if (playerCache != null && playerCache.size() > 50) {
            playerCache.clear();
        }
    }
    
    public void clearChunkVisibilityCache(UUID playerId) {
        chunkVisibilityCache.remove(playerId);
    }
    
    public void clearAllChunkVisibilityCache() {
        chunkVisibilityCache.clear();
    }

}
