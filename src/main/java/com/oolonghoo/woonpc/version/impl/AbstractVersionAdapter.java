package com.oolonghoo.woonpc.version.impl;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import com.oolonghoo.woonpc.version.VersionAdapter;
import com.oolonghoo.woonpc.version.VersionUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 版本适配器抽象基类
 * <p>
 * 提供所有版本共用的实现，子类只需覆盖有差异的方法。
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public abstract class AbstractVersionAdapter implements VersionAdapter {

    // ==================== 反射缓存 ====================

    protected static Method gameProfileIdMethod;
    protected static Method gameProfileNameMethod;
    protected static Method gameProfilePropertiesMethod;
    protected static Constructor<?> gameProfileConstructorWithProps;
    protected static Constructor<?> propertyMapConstructor;
    protected static Method serverPlayerLevelMethod;

    // 区块可见性缓存
    protected final Map<UUID, Map<Long, Boolean>> chunkVisibilityCache = new ConcurrentHashMap<>();
    protected int cacheInvalidationCounter = 0;
    protected static final int CACHE_INVALIDATION_INTERVAL = 100;

    // 静态初始化块
    static {
        initReflection();
    }

    /**
     * 初始化反射方法缓存
     */
    protected static synchronized void initReflection() {
        // GameProfile.getId()
        try {
            gameProfileIdMethod = GameProfile.class.getMethod("getId");
        } catch (NoSuchMethodException e) {
            try {
                gameProfileIdMethod = GameProfile.class.getMethod("id");
            } catch (NoSuchMethodException ex) {
                Bukkit.getLogger().warning("[WooNPC] Failed to get GameProfile.getId method: " + ex.getMessage());
            }
        }

        // GameProfile.getName()
        try {
            gameProfileNameMethod = GameProfile.class.getMethod("getName");
        } catch (NoSuchMethodException e) {
            try {
                gameProfileNameMethod = GameProfile.class.getMethod("name");
            } catch (NoSuchMethodException ex) {
                Bukkit.getLogger().warning(() -> "[WooNPC] Failed to get GameProfile.getName method: " + ex.getMessage());
            }
        }

        // GameProfile.getProperties()
        try {
            gameProfilePropertiesMethod = GameProfile.class.getMethod("getProperties");
        } catch (NoSuchMethodException e) {
            try {
                gameProfilePropertiesMethod = GameProfile.class.getMethod("properties");
            } catch (NoSuchMethodException ex) {
                Bukkit.getLogger().warning("[WooNPC] Failed to get GameProfile.getProperties method: " + ex.getMessage());
            }
        }

        // GameProfile(UUID, String, PropertyMap)
        try {
            gameProfileConstructorWithProps = GameProfile.class.getConstructor(UUID.class, String.class, PropertyMap.class);
        } catch (NoSuchMethodException e) {
            gameProfileConstructorWithProps = null;
        }

        // PropertyMap(ImmutableMultimap)
        try {
            propertyMapConstructor = PropertyMap.class.getConstructor(ImmutableMultimap.class);
        } catch (NoSuchMethodException e) {
            propertyMapConstructor = null;
        }

        // ServerPlayer.serverLevel()
        try {
            serverPlayerLevelMethod = net.minecraft.server.level.ServerPlayer.class.getMethod("serverLevel");
        } catch (NoSuchMethodException e) {
            try {
                serverPlayerLevelMethod = net.minecraft.server.level.ServerPlayer.class.getMethod("level");
            } catch (NoSuchMethodException ex) {
                Bukkit.getLogger().warning("[WooNPC] Failed to get ServerPlayer.level method: " + ex.getMessage());
            }
        }
    }

    // ==================== 版本信息 ====================

    @Override
    @Nonnull
    public String getMinSupportedVersion() {
        return VersionUtil.MIN_VERSION;
    }

    @Override
    @Nonnull
    public String getMaxSupportedVersion() {
        return VersionUtil.MAX_VERSION;
    }

    // ==================== 数据包操作 ====================

    @Override
    public void sendPacket(@Nonnull Player player, @Nonnull Packet<?> packet) {
        net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
        runOnPlayerScheduler(player, () -> serverPlayer.connection.send(packet));
    }

    @Override
    public void sendPackets(@Nonnull Player player, @Nonnull List<Packet<? super ClientGamePacketListener>> packets) {
        if (packets.isEmpty()) {
            return;
        }
        if (packets.size() == 1) {
            sendPacket(player, packets.get(0));
            return;
        }
        sendPacket(player, createBundlePacket(packets));
    }

    @Override
    @Nonnull
    public Packet<?> createBundlePacket(@Nonnull List<Packet<? super ClientGamePacketListener>> packets) {
        return new ClientboundBundlePacket(packets);
    }

    // ==================== 区块检测 ====================

    @Override
    public boolean isChunkLoaded(@Nonnull Player player, int chunkX, int chunkZ) {
        org.bukkit.World world = player.getWorld();
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    @Override
    public boolean isChunkSentToClient(@Nonnull Player player, int chunkX, int chunkZ) {
        try {
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            long chunkKey = chunkX & 0xFFFFFFFFL | ((long) chunkZ & 0xFFFFFFFFL) << 32;

            UUID playerId = player.getUniqueId();
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

    /**
     * 使用 NMS 检查区块是否已发送给客户端
     */
    protected boolean checkChunkSentNMS(net.minecraft.server.level.ServerPlayer serverPlayer, int chunkX, int chunkZ) {
        try {
            net.minecraft.server.level.ServerLevel serverLevel = getServerLevel(serverPlayer);
            if (serverLevel == null) {
                return true;
            }

            Method getChunkProviderMethod = net.minecraft.server.level.ServerLevel.class.getMethod("getChunkProvider");
            Object chunkProvider = getChunkProviderMethod.invoke(serverLevel);

            if (chunkProvider == null) {
                return true;
            }

            Class<?> chunkProviderClass = chunkProvider.getClass();
            Field chunkMapField = findField(chunkProviderClass, "chunkMap");

            if (chunkMapField == null) {
                return true;
            }

            chunkMapField.setAccessible(true);
            Object chunkMap = chunkMapField.get(chunkProvider);

            if (chunkMap == null) {
                return true;
            }

            Method getPlayersMethod = findMethod(chunkMap.getClass(), "getPlayers", int.class, int.class, boolean.class);

            if (getPlayersMethod == null) {
                return true;
            }

            Object players = getPlayersMethod.invoke(chunkMap, chunkX, chunkZ, false);

            if (players instanceof List<?> playerList) {
                return playerList.contains(serverPlayer);
            }

            return true;
        } catch (Exception e) {
            return true;
        }
    }

    // ==================== 实体数据包创建 ====================

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createAddEntityPacket(int entityId, @Nonnull UUID uuid, @Nonnull Location location, @Nonnull org.bukkit.entity.EntityType type) {
        Optional<net.minecraft.core.Holder.Reference<EntityType<?>>> entityTypeReference =
                BuiltInRegistries.ENTITY_TYPE.get(CraftNamespacedKey.toMinecraft(type.getKey()));

        if (entityTypeReference.isEmpty()) {
            throw new IllegalArgumentException("Unknown entity type: " + type);
        }

        EntityType<?> nmsType = entityTypeReference.get().value();

        return new ClientboundAddEntityPacket(
                entityId,
                uuid,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw(),
                nmsType,
                0,
                Vec3.ZERO,
                location.getYaw()
        );
    }

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createRemoveEntityPacket(int... entityIds) {
        return new ClientboundRemoveEntitiesPacket(Arrays.stream(entityIds).toArray());
    }

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createRemoveEntityPacket(@Nonnull List<Integer> entityIds) {
        return new ClientboundRemoveEntitiesPacket(entityIds.stream().mapToInt(Integer::intValue).toArray());
    }

    // ==================== 玩家信息数据包 ====================

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createPlayerInfoAddPacket(@Nonnull GameProfile profile, @Nonnull String displayName, boolean showInTab) {
        EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
        actions.add(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);
        actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
        if (showInTab) {
            actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED);
        }

        Component displayComponent = Component.literal(displayName);
        UUID profileId = getProfileId(profile);

        ClientboundPlayerInfoUpdatePacket.Entry entry = createPlayerInfoEntry(
                profileId != null ? profileId : UUID.randomUUID(),
                profile,
                showInTab,
                displayComponent
        );

        return new ClientboundPlayerInfoUpdatePacket(actions, List.of(entry));
    }

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createPlayerInfoAddPacket(@Nonnull GameProfile profile, @Nonnull String displayName,
                                               boolean showInTab, @Nullable String skinValue, @Nullable String skinSignature) {
        UUID profileId = getProfileId(profile);
        String profileName = getProfileName(profile);
        GameProfile profileWithSkin = createGameProfileWithSkin(
                profileId != null ? profileId : UUID.randomUUID(),
                profileName,
                skinValue,
                skinSignature
        );

        return createPlayerInfoAddPacket(profileWithSkin, displayName, showInTab);
    }

    /**
     * 创建玩家信息条目（子类可覆盖以适应不同版本）
     */
    protected ClientboundPlayerInfoUpdatePacket.Entry createPlayerInfoEntry(UUID uuid, GameProfile profile,
                                                                            boolean showInTab, Component displayName) {
        return new ClientboundPlayerInfoUpdatePacket.Entry(
                uuid,
                profile,
                showInTab,
                0,
                net.minecraft.world.level.GameType.SURVIVAL,
                displayName,
                true,
                -1,
                null
        );
    }

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createPlayerInfoRemovePacket(@Nonnull UUID uuid) {
        return new ClientboundPlayerInfoRemovePacket(List.of(uuid));
    }

    // ==================== 位置和旋转数据包 ====================

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createTeleportPacket(int entityId, @Nonnull Location location, float yaw, float pitch) {
        return new ClientboundTeleportEntityPacket(
                entityId,
                new PositionMoveRotation(
                        new Vec3(location.getX(), location.getY(), location.getZ()),
                        Vec3.ZERO,
                        yaw,
                        pitch
                ),
                Set.of(),
                false
        );
    }

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createRotateHeadPacket(@Nonnull Entity entity, float yaw) {
        float angleMultiplier = 256f / 360f;
        return new ClientboundRotateHeadPacket(entity, (byte) (yaw * angleMultiplier));
    }

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createRotateHeadPacket(int entityId, float yaw) {
        float angleMultiplier = 256f / 360f;
        // ClientboundRotateHeadPacket 构造函数需要 Entity 对象
        // 我们使用反射创建空实例然后设置字段
        try {
            // 获取无参构造函数或使用 Unsafe 分配
            ClientboundRotateHeadPacket packet;
            try {
                // 尝试获取无参构造函数
                Constructor<ClientboundRotateHeadPacket> constructor = 
                        ClientboundRotateHeadPacket.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                packet = constructor.newInstance();
            } catch (NoSuchMethodException e) {
                // 使用 Unsafe 分配实例
                sun.misc.Unsafe unsafe = getUnsafe();
                packet = (ClientboundRotateHeadPacket) unsafe.allocateInstance(ClientboundRotateHeadPacket.class);
            }
            
            // 查找正确的字段名（可能是 id 或 entityId）
            Field idField = null;
            for (Field field : ClientboundRotateHeadPacket.class.getDeclaredFields()) {
                if (field.getType() == int.class) {
                    idField = field;
                    break;
                }
            }
            
            if (idField == null) {
                throw new NoSuchFieldException("Could not find entity id field in ClientboundRotateHeadPacket");
            }
            
            idField.setAccessible(true);
            idField.setInt(packet, entityId);
            
            // 查找正确的字段名（可能是 yHeadRot 或 yRot）
            Field yHeadRotField = null;
            for (Field field : ClientboundRotateHeadPacket.class.getDeclaredFields()) {
                if (field.getType() == byte.class) {
                    yHeadRotField = field;
                    break;
                }
            }
            
            if (yHeadRotField == null) {
                throw new NoSuchFieldException("Could not find yHeadRot field in ClientboundRotateHeadPacket");
            }
            
            yHeadRotField.setAccessible(true);
            yHeadRotField.setByte(packet, (byte) (yaw * angleMultiplier));
            
            return packet;
        } catch (ReflectiveOperationException e) {
            Bukkit.getLogger().warning(() -> "[WooNPC] Failed to create rotate head packet: " + e.getMessage());
            // 如果失败，返回一个空的传送数据包作为后备
            return createTeleportPacket(entityId, new org.bukkit.Location(
                    Bukkit.getWorlds().get(0), 0, 0, 0), yaw, 0);
        }
    }
    
    private sun.misc.Unsafe getUnsafe() throws ReflectiveOperationException {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (sun.misc.Unsafe) unsafeField.get(null);
    }

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createAnimatePacket(int entityId, int animationId) {
        // ClientboundAnimatePacket 需要 Entity 对象，我们使用反射
        try {
            ClientboundAnimatePacket packet = (ClientboundAnimatePacket) 
                    ClientboundAnimatePacket.class.getConstructors()[0].newInstance();
            
            // 设置 id 字段
            Field idField = ClientboundAnimatePacket.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.setInt(packet, entityId);
            
            // 设置 action 字段
            Field actionField = ClientboundAnimatePacket.class.getDeclaredField("action");
            actionField.setAccessible(true);
            actionField.setInt(packet, animationId);
            
            return packet;
        } catch (ReflectiveOperationException e) {
            Bukkit.getLogger().warning(() -> "[WooNPC] Failed to create animate packet: " + e.getMessage());
            throw new RuntimeException("Failed to create animate packet", e);
        }
    }

    // ==================== 装备数据包 ====================

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createSetEquipmentPacket(int entityId, @Nonnull Map<EquipmentSlot, org.bukkit.inventory.ItemStack> equipment) {
        List<Pair<EquipmentSlot, ItemStack>> equipmentList = new ArrayList<>();

        for (Map.Entry<EquipmentSlot, org.bukkit.inventory.ItemStack> entry : equipment.entrySet()) {
            ItemStack nmsItem = toNMSItemStack(entry.getValue());
            equipmentList.add(new Pair<>(entry.getKey(), nmsItem));
        }

        return new ClientboundSetEquipmentPacket(entityId, equipmentList);
    }

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createSetEquipmentPacketNMS(int entityId, @Nonnull Map<EquipmentSlot, ItemStack> equipment) {
        List<Pair<EquipmentSlot, ItemStack>> equipmentList = new ArrayList<>();

        for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
            equipmentList.add(new Pair<>(entry.getKey(), entry.getValue()));
        }

        return new ClientboundSetEquipmentPacket(entityId, equipmentList);
    }

    // ==================== 实体数据数据包 ====================

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createSetEntityDataPacket(int entityId, @Nonnull List<SynchedEntityData.DataValue<?>> dataItems) {
        return new ClientboundSetEntityDataPacket(entityId, dataItems);
    }

    // ==================== 团队数据包 ====================

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createTeamPacket(@Nonnull String teamName, @Nonnull String playerName,
                                      @Nonnull ChatColor color, boolean create) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
        team.getPlayers().clear();
        team.getPlayers().add(playerName);

        net.minecraft.ChatFormatting chatFormatting = convertToNMSChatFormatting(color);
        team.setColor(chatFormatting);

        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, create);
    }

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createTeamPacket(@Nonnull String teamName, @Nonnull String playerName,
                                      @Nonnull ChatColor color, boolean create,
                                      @Nullable Team.CollisionRule collisionRule,
                                      @Nullable Team.Visibility nameTagVisibility) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
        team.getPlayers().clear();
        team.getPlayers().add(playerName);

        net.minecraft.ChatFormatting chatFormatting = convertToNMSChatFormatting(color);
        team.setColor(chatFormatting);

        if (collisionRule != null) {
            team.setCollisionRule(collisionRule);
        }

        if (nameTagVisibility != null) {
            team.setNameTagVisibility(nameTagVisibility);
        }

        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, create);
    }

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createRemoveTeamPacket(@Nonnull String teamName) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
        return ClientboundSetPlayerTeamPacket.createRemovePacket(team);
    }

    // ==================== 动画数据包 ====================

    @Override
    @Nonnull
    public Packet<? super ClientGamePacketListener> createAnimatePacket(@Nonnull Entity entity, int animationId) {
        return new ClientboundAnimatePacket(entity, animationId);
    }

    // ==================== 实体操作 ====================

    @Override
    public int getEntityId(@Nonnull Entity entity) {
        return entity.getId();
    }

    @Override
    @Nonnull
    public net.minecraft.server.level.ServerPlayer getServerPlayer(@Nonnull Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    // ==================== 物品转换 ====================

    @Override
    @Nonnull
    public ItemStack toNMSItemStack(@Nonnull org.bukkit.inventory.ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    @Override
    @Nonnull
    public org.bukkit.inventory.ItemStack toBukkitItemStack(@Nonnull ItemStack itemStack) {
        return CraftItemStack.asBukkitCopy(itemStack);
    }

    // ==================== GameProfile 操作 ====================

    @Override
    @Nonnull
    public GameProfile createGameProfileWithSkin(@Nonnull UUID uuid, @Nonnull String name,
                                                 @Nullable String skinValue, @Nullable String skinSignature) {
        if (skinValue == null || skinValue.isEmpty()) {
            return new GameProfile(uuid, name);
        }

        try {
            if (propertyMapConstructor != null && gameProfileConstructorWithProps != null) {
                PropertyMap propertyMap = (PropertyMap) propertyMapConstructor.newInstance(
                        ImmutableMultimap.of("textures", new Property("textures", skinValue, skinSignature))
                );
                return (GameProfile) gameProfileConstructorWithProps.newInstance(uuid, name, propertyMap);
            }
        } catch (ReflectiveOperationException e) {
            Bukkit.getLogger().warning(() -> "[WooNPC] Failed to create GameProfile with skin: " + e.getMessage());
        }

        return new GameProfile(uuid, name);
    }

    @Override
    @Nullable
    public UUID getProfileId(@Nonnull GameProfile profile) {
        try {
            if (gameProfileIdMethod != null) {
                return (UUID) gameProfileIdMethod.invoke(profile);
            }
        } catch (ReflectiveOperationException e) {
            Bukkit.getLogger().warning(() -> "[WooNPC] Failed to get profile id: " + e.getMessage());
        }
        return null;
    }

    @Override
    @Nonnull
    public String getProfileName(@Nonnull GameProfile profile) {
        try {
            if (gameProfileNameMethod != null) {
                return (String) gameProfileNameMethod.invoke(profile);
            }
        } catch (ReflectiveOperationException e) {
            Bukkit.getLogger().warning(() -> "[WooNPC] Failed to get profile name: " + e.getMessage());
        }
        return "";
    }

    @Override
    @Nullable
    public PropertyMap getProfileProperties(@Nonnull GameProfile profile) {
        try {
            if (gameProfilePropertiesMethod != null) {
                return (PropertyMap) gameProfilePropertiesMethod.invoke(profile);
            }
        } catch (ReflectiveOperationException e) {
            Bukkit.getLogger().warning(() -> "[WooNPC] Failed to get profile properties: " + e.getMessage());
        }
        return null;
    }

    // ==================== 调度器支持 ====================

    @Override
    public void runOnPlayerScheduler(@Nonnull Player player, @Nonnull Runnable task) {
        if (VersionUtil.isFolia()) {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("WooNPC");
            if (plugin != null) {
                player.getScheduler().run(plugin, (t) -> task.run(), null);
                return;
            }
        }
        task.run();
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取 ServerLevel
     */
    @Override
    @Nullable
    public net.minecraft.server.level.ServerLevel getServerLevel(@Nonnull net.minecraft.server.level.ServerPlayer player) {
        try {
            if (serverPlayerLevelMethod != null) {
                return (net.minecraft.server.level.ServerLevel) serverPlayerLevelMethod.invoke(player);
            }
        } catch (ReflectiveOperationException e) {
            Bukkit.getLogger().warning(() -> "[WooNPC] Failed to get server level: " + e.getMessage());
        }
        return null;
    }

    /**
     * 查找字段
     */
    protected Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().contains(name) || name.contains(field.getName())) {
                    return field;
                }
            }
            // 尝试父类
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return findField(superClass, name);
            }
        }
        return null;
    }

    /**
     * 查找方法
     */
    protected Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == paramTypes.length) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * 使区块缓存失效
     */
    protected void invalidateChunkCache(UUID playerId) {
        Map<Long, Boolean> playerCache = chunkVisibilityCache.get(playerId);
        if (playerCache != null && playerCache.size() > 50) {
            playerCache.clear();
        }
    }

    /**
     * 清理指定玩家的区块可见性缓存
     * 在玩家退出服务器时调用，防止内存泄漏
     *
     * @param playerId 玩家 UUID
     */
    @Override
    public void cleanupPlayerCache(@Nonnull UUID playerId) {
        chunkVisibilityCache.remove(playerId);
    }

    /**
     * 转换 ChatColor 到 NMS ChatFormatting
     */
    @SuppressWarnings("deprecation")
    protected net.minecraft.ChatFormatting convertToNMSChatFormatting(ChatColor color) {
        if (color == null) {
            return net.minecraft.ChatFormatting.WHITE;
        }

        return switch (color) {
            case BLACK -> net.minecraft.ChatFormatting.BLACK;
            case DARK_BLUE -> net.minecraft.ChatFormatting.DARK_BLUE;
            case DARK_GREEN -> net.minecraft.ChatFormatting.DARK_GREEN;
            case DARK_AQUA -> net.minecraft.ChatFormatting.DARK_AQUA;
            case DARK_RED -> net.minecraft.ChatFormatting.DARK_RED;
            case DARK_PURPLE -> net.minecraft.ChatFormatting.DARK_PURPLE;
            case GOLD -> net.minecraft.ChatFormatting.GOLD;
            case GRAY -> net.minecraft.ChatFormatting.GRAY;
            case DARK_GRAY -> net.minecraft.ChatFormatting.DARK_GRAY;
            case BLUE -> net.minecraft.ChatFormatting.BLUE;
            case GREEN -> net.minecraft.ChatFormatting.GREEN;
            case AQUA -> net.minecraft.ChatFormatting.AQUA;
            case RED -> net.minecraft.ChatFormatting.RED;
            case LIGHT_PURPLE -> net.minecraft.ChatFormatting.LIGHT_PURPLE;
            case YELLOW -> net.minecraft.ChatFormatting.YELLOW;
            case WHITE -> net.minecraft.ChatFormatting.WHITE;
            default -> net.minecraft.ChatFormatting.WHITE;
        };
    }
}
