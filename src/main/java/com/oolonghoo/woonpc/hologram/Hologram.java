package com.oolonghoo.woonpc.hologram;

import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全息文字系统
 * 使用 Marker Armor Stand 实现多行全息文字
 * 
 * @author oolongho
 */
public class Hologram {

    // 实体 ID 生成器
    private static final AtomicInteger ENTITY_ID_GENERATOR = new AtomicInteger(Integer.MAX_VALUE - 100000);
    
    // 每行高度间隔
    private static final double LINE_HEIGHT = 0.3;
    
    // 基础偏移（NPC 头顶上方）
    private static final double BASE_OFFSET = 2.0;
    
    // Armor Stand 元数据标志
    private static final byte FLAG_INVISIBLE = 0x20;
    private static final byte FLAG_SMALL = 0x01;
    private static final byte FLAG_NO_GRAVITY = 0x10;
    private static final byte FLAG_MARKER = 0x08;
    
    // 实体 ID 列表
    private final List<Integer> entityIds;
    
    // 实体 UUID 列表
    private final List<UUID> entityUUIDs;
    
    // 全息文字位置
    private Location location;
    
    // 文字内容
    private List<String> lines;
    
    // 已显示的玩家
    private final Set<UUID> shownToPlayers = ConcurrentHashMap.newKeySet();
    
    // 是否已移除
    private boolean removed = false;
    
    /**
     * 创建全息文字
     * 
     * @param location 位置
     * @param lines 文字内容
     */
    public Hologram(@NotNull Location location, @NotNull List<String> lines) {
        this.location = location.clone();
        this.lines = new ArrayList<>(lines);
        this.entityIds = new ArrayList<>();
        this.entityUUIDs = new ArrayList<>();
        
        // 为每行生成实体 ID 和 UUID
        for (int i = 0; i < lines.size(); i++) {
            entityIds.add(generateEntityId());
            entityUUIDs.add(UUID.randomUUID());
        }
    }
    
    /**
     * 创建单行全息文字
     * 
     * @param location 位置
     * @param line 文字内容
     */
    public Hologram(@NotNull Location location, @NotNull String line) {
        this(location, Collections.singletonList(line));
    }
    
    /**
     * 显示给玩家
     * 
     * @param player 目标玩家
     */
    public void showTo(@NotNull Player player) {
        if (removed) {
            return;
        }
        
        if (shownToPlayers.contains(player.getUniqueId())) {
            // 已经显示过，先移除再重新显示
            hideFrom(player);
        }
        
        net.minecraft.server.level.ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        
        for (int i = 0; i < lines.size(); i++) {
            int entityId = entityIds.get(i);
            UUID uuid = entityUUIDs.get(i);
            String line = lines.get(i);
            
            // 计算位置（从上往下排列）
            double yOffset = BASE_OFFSET + (lines.size() - 1 - i) * LINE_HEIGHT;
            Location lineLocation = location.clone().add(0, yOffset, 0);
            
            // 创建 Armor Stand 生成包
            ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
                    entityId,
                    uuid,
                    lineLocation.getX(),
                    lineLocation.getY(),
                    lineLocation.getZ(),
                    0f, // pitch
                    0f, // yaw
                    EntityType.ARMOR_STAND,
                    0,
                    Vec3.ZERO,
                    0f
            );
            packets.add(addEntityPacket);
            
            // 创建元数据包
            List<SynchedEntityData.DataValue<?>> dataValues = createArmorStandMetadata(line);
            ClientboundSetEntityDataPacket entityDataPacket = new ClientboundSetEntityDataPacket(entityId, dataValues);
            packets.add(entityDataPacket);
        }
        
        // 使用 BundlePacket 打包发送
        ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packets);
        runOnPlayerScheduler(player, () -> serverPlayer.connection.send(bundlePacket));
        
        shownToPlayers.add(player.getUniqueId());
    }
    
    /**
     * 从玩家隐藏
     * 
     * @param player 目标玩家
     */
    public void hideFrom(@NotNull Player player) {
        if (!shownToPlayers.contains(player.getUniqueId())) {
            return;
        }
        
        net.minecraft.server.level.ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        // 移除所有实体
        int[] ids = entityIds.stream().mapToInt(Integer::intValue).toArray();
        ClientboundRemoveEntitiesPacket removePacket = new ClientboundRemoveEntitiesPacket(ids);
        runOnPlayerScheduler(player, () -> serverPlayer.connection.send(removePacket));
        
        shownToPlayers.remove(player.getUniqueId());
    }
    
    /**
     * 更新文字内容
     * 
     * @param newLines 新的文字内容
     */
    public void updateLines(@NotNull List<String> newLines) {
        if (removed) {
            return;
        }
        
        // 如果行数变化，需要重新创建实体
        if (newLines.size() != lines.size()) {
            // 先对所有已显示的玩家隐藏
            Set<UUID> playersToShow = new HashSet<>(shownToPlayers);
            for (UUID playerId : playersToShow) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    hideFrom(player);
                }
            }
            
            // 清空旧实体
            entityIds.clear();
            entityUUIDs.clear();
            
            // 创建新实体
            for (int i = 0; i < newLines.size(); i++) {
                entityIds.add(generateEntityId());
                entityUUIDs.add(UUID.randomUUID());
            }
            
            // 更新文字
            this.lines = new ArrayList<>(newLines);
            
            // 重新显示给玩家
            for (UUID playerId : playersToShow) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    showTo(player);
                }
            }
        } else {
            // 行数相同，只更新元数据
            this.lines = new ArrayList<>(newLines);
            
            for (UUID playerId : new HashSet<>(shownToPlayers)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    updateMetadata(player);
                }
            }
        }
    }
    
    /**
     * 更新单个玩家的元数据
     * 
     * @param player 目标玩家
     */
    private void updateMetadata(@NotNull Player player) {
        if (!shownToPlayers.contains(player.getUniqueId())) {
            return;
        }
        
        net.minecraft.server.level.ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        
        for (int i = 0; i < lines.size(); i++) {
            int entityId = entityIds.get(i);
            String line = lines.get(i);
            
            List<SynchedEntityData.DataValue<?>> dataValues = createArmorStandMetadata(line);
            ClientboundSetEntityDataPacket entityDataPacket = new ClientboundSetEntityDataPacket(entityId, dataValues);
            packets.add(entityDataPacket);
        }
        
        ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packets);
        runOnPlayerScheduler(player, () -> serverPlayer.connection.send(bundlePacket));
    }
    
    /**
     * 设置位置
     * 
     * @param location 新位置
     */
    public void setLocation(@NotNull Location location) {
        this.location = location.clone();
        
        if (removed) {
            return;
        }
        
        // 更新所有已显示玩家的位置
        for (UUID playerId : new HashSet<>(shownToPlayers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                updatePosition(player);
            }
        }
    }
    
    /**
     * 更新单个玩家的位置
     * 
     * @param player 目标玩家
     */
    private void updatePosition(@NotNull Player player) {
        if (!shownToPlayers.contains(player.getUniqueId())) {
            return;
        }
        
        net.minecraft.server.level.ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        
        for (int i = 0; i < lines.size(); i++) {
            int entityId = entityIds.get(i);
            
            // 计算位置
            double yOffset = BASE_OFFSET + (lines.size() - 1 - i) * LINE_HEIGHT;
            Location lineLocation = location.clone().add(0, yOffset, 0);
            
            // 创建传送包
            ClientboundTeleportEntityPacket teleportPacket = new ClientboundTeleportEntityPacket(
                    entityId,
                    new PositionMoveRotation(
                            new Vec3(lineLocation.getX(), lineLocation.getY(), lineLocation.getZ()),
                            Vec3.ZERO,
                            0f,
                            0f
                    ),
                    Set.of(),
                    false
            );
            packets.add(teleportPacket);
        }
        
        ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packets);
        runOnPlayerScheduler(player, () -> serverPlayer.connection.send(bundlePacket));
    }
    
    /**
     * 移除所有
     */
    public void removeAll() {
        if (removed) {
            return;
        }
        
        removed = true;
        
        // 对所有已显示的玩家隐藏
        for (UUID playerId : new HashSet<>(shownToPlayers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                hideFrom(player);
            }
        }
        
        shownToPlayers.clear();
    }
    
    /**
     * 创建 Armor Stand 元数据
     * 
     * @param line 文字内容
     * @return 元数据列表
     */
    private List<SynchedEntityData.DataValue<?>> createArmorStandMetadata(String line) {
        List<SynchedEntityData.DataValue<?>> dataValues = new ArrayList<>();
        
        // Index 0: 实体状态标志 (隐形)
        dataValues.add(SynchedEntityData.DataValue.create(
                getSharedFlagsIdAccessor(),
                FLAG_INVISIBLE
        ));
        
        // Index 2: 自定义名称
        net.kyori.adventure.text.Component adventureComponent = 
                net.kyori.adventure.text.Component.text(line);
        net.minecraft.network.chat.Component vanillaComponent = 
                PaperAdventure.asVanilla(adventureComponent);
        dataValues.add(SynchedEntityData.DataValue.create(
                getCustomNameAccessor(),
                Optional.of(vanillaComponent)
        ));
        
        // Index 3: 名称可见
        dataValues.add(SynchedEntityData.DataValue.create(
                getCustomNameVisibleAccessor(),
                true
        ));
        
        // Index 15: Armor Stand 状态标志 (小型 + 无重力 + Marker)
        byte armorStandFlags = FLAG_SMALL | FLAG_NO_GRAVITY | FLAG_MARKER;
        dataValues.add(SynchedEntityData.DataValue.create(
                getArmorStandClientFlagsAccessor(),
                armorStandFlags
        ));
        
        return dataValues;
    }
    
    // ==================== 反射获取 EntityDataAccessor ====================
    
    private static EntityDataAccessor<Byte> SHARED_FLAGS_ID_ACCESSOR = null;
    private static EntityDataAccessor<Optional<net.minecraft.network.chat.Component>> CUSTOM_NAME_ACCESSOR = null;
    private static EntityDataAccessor<Boolean> CUSTOM_NAME_VISIBLE_ACCESSOR = null;
    private static EntityDataAccessor<Byte> ARMOR_STAND_CLIENT_FLAGS_ACCESSOR = null;
    
    /**
     * 获取 DATA_SHARED_FLAGS_ID 访问器
     */
    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Byte> getSharedFlagsIdAccessor() {
        if (SHARED_FLAGS_ID_ACCESSOR != null) {
            return SHARED_FLAGS_ID_ACCESSOR;
        }
        try {
            Field field = net.minecraft.world.entity.Entity.class.getDeclaredField("DATA_SHARED_FLAGS_ID");
            field.setAccessible(true);
            SHARED_FLAGS_ID_ACCESSOR = (EntityDataAccessor<Byte>) field.get(null);
            return SHARED_FLAGS_ID_ACCESSOR;
        } catch (Exception e) {
            throw new RuntimeException("无法获取 DATA_SHARED_FLAGS_ID", e);
        }
    }
    
    /**
     * 获取 DATA_CUSTOM_NAME 访问器
     */
    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Optional<net.minecraft.network.chat.Component>> getCustomNameAccessor() {
        if (CUSTOM_NAME_ACCESSOR != null) {
            return CUSTOM_NAME_ACCESSOR;
        }
        try {
            Field field = net.minecraft.world.entity.Entity.class.getDeclaredField("DATA_CUSTOM_NAME");
            field.setAccessible(true);
            CUSTOM_NAME_ACCESSOR = (EntityDataAccessor<Optional<net.minecraft.network.chat.Component>>) field.get(null);
            return CUSTOM_NAME_ACCESSOR;
        } catch (Exception e) {
            throw new RuntimeException("无法获取 DATA_CUSTOM_NAME", e);
        }
    }
    
    /**
     * 获取 DATA_CUSTOM_NAME_VISIBLE 访问器
     */
    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Boolean> getCustomNameVisibleAccessor() {
        if (CUSTOM_NAME_VISIBLE_ACCESSOR != null) {
            return CUSTOM_NAME_VISIBLE_ACCESSOR;
        }
        try {
            Field field = net.minecraft.world.entity.Entity.class.getDeclaredField("DATA_CUSTOM_NAME_VISIBLE");
            field.setAccessible(true);
            CUSTOM_NAME_VISIBLE_ACCESSOR = (EntityDataAccessor<Boolean>) field.get(null);
            return CUSTOM_NAME_VISIBLE_ACCESSOR;
        } catch (Exception e) {
            throw new RuntimeException("无法获取 DATA_CUSTOM_NAME_VISIBLE", e);
        }
    }
    
    /**
     * 获取 ArmorStand DATA_CLIENT_FLAGS 访问器
     */
    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Byte> getArmorStandClientFlagsAccessor() {
        if (ARMOR_STAND_CLIENT_FLAGS_ACCESSOR != null) {
            return ARMOR_STAND_CLIENT_FLAGS_ACCESSOR;
        }
        try {
            Field field = net.minecraft.world.entity.decoration.ArmorStand.class.getDeclaredField("DATA_CLIENT_FLAGS");
            field.setAccessible(true);
            ARMOR_STAND_CLIENT_FLAGS_ACCESSOR = (EntityDataAccessor<Byte>) field.get(null);
            return ARMOR_STAND_CLIENT_FLAGS_ACCESSOR;
        } catch (Exception e) {
            throw new RuntimeException("无法获取 DATA_CLIENT_FLAGS", e);
        }
    }
    
    /**
     * 生成实体 ID
     * 
     * @return 实体 ID
     */
    private int generateEntityId() {
        return ENTITY_ID_GENERATOR.decrementAndGet();
    }
    
    /**
     * 在玩家调度器上运行任务 (支持 Folia)
     * 
     * @param player 玩家
     * @param task 任务
     */
    private void runOnPlayerScheduler(Player player, Runnable task) {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            player.getScheduler().run(
                    Bukkit.getPluginManager().getPlugin("WooNPC"),
                    (t) -> task.run(),
                    null
            );
        } catch (ClassNotFoundException e) {
            // 不是 Folia，直接运行
            task.run();
        }
    }
    
    // ==================== Getters ====================
    
    /**
     * 获取位置
     * 
     * @return 位置
     */
    @NotNull
    public Location getLocation() {
        return location.clone();
    }
    
    /**
     * 获取文字内容
     * 
     * @return 文字内容列表
     */
    @NotNull
    public List<String> getLines() {
        return new ArrayList<>(lines);
    }
    
    /**
     * 获取实体 ID 列表
     * 
     * @return 实体 ID 列表
     */
    @NotNull
    public List<Integer> getEntityIds() {
        return new ArrayList<>(entityIds);
    }
    
    /**
     * 检查是否已移除
     * 
     * @return 是否已移除
     */
    public boolean isRemoved() {
        return removed;
    }
    
    /**
     * 检查是否对玩家显示
     * 
     * @param player 玩家
     * @return 是否显示
     */
    public boolean isShownTo(@NotNull Player player) {
        return shownToPlayers.contains(player.getUniqueId());
    }
    
    /**
     * 获取已显示的玩家数量
     * 
     * @return 玩家数量
     */
    public int getViewerCount() {
        return shownToPlayers.size();
    }
}
