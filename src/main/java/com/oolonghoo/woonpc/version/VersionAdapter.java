package com.oolonghoo.woonpc.version;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 版本适配器接口
 * <p>
 * 定义所有 NMS 操作的抽象方法，为不同 Minecraft 版本提供统一的 API。
 * 支持 Minecraft 1.21.2 - 1.21.11 版本。
 * </p>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>接口隔离：只暴露必要的 NMS 操作</li>
 *   <li>版本无关：调用者无需关心具体版本实现</li>
 *   <li>线程安全：所有方法都应在正确的线程上下文中调用</li>
 * </ul>
 *
 * @author oolongho
 * @since 1.0.0
 */
public interface VersionAdapter {

    // ==================== 版本信息 ====================

    /**
     * 获取当前适配器支持的 Minecraft 版本号
     *
     * @return 版本字符串，格式为 "1.21.X"
     */
    @Nonnull
    String getMcVersion();

    /**
     * 获取支持的最低版本
     *
     * @return 最低版本字符串
     */
    @Nonnull
    default String getMinSupportedVersion() {
        return "1.21.2";
    }

    /**
     * 获取支持的最高版本
     *
     * @return 最高版本字符串
     */
    @Nonnull
    default String getMaxSupportedVersion() {
        return "1.21.11";
    }

    // ==================== 数据包操作 ====================

    /**
     * 向玩家发送数据包
     *
     * @param player 目标玩家
     * @param packet 要发送的数据包
     */
    void sendPacket(@Nonnull Player player, @Nonnull Packet<?> packet);

    /**
     * 向玩家发送多个数据包（批量发送）
     *
     * @param player  目标玩家
     * @param packets 数据包列表
     */
    void sendPackets(@Nonnull Player player, @Nonnull List<Packet<? super ClientGamePacketListener>> packets);

    /**
     * 创建批量数据包
     *
     * @param packets 数据包列表
     * @return 打包后的批量数据包
     */
    @Nonnull
    Packet<?> createBundlePacket(@Nonnull List<Packet<? super ClientGamePacketListener>> packets);

    // ==================== 区块检测 ====================

    /**
     * 检查指定区块是否已发送给玩家客户端
     *
     * @param player 目标玩家
     * @param chunkX 区块 X 坐标
     * @param chunkZ 区块 Z 坐标
     * @return 如果区块已发送则返回 true
     */
    boolean isChunkSentToClient(@Nonnull Player player, int chunkX, int chunkZ);

    /**
     * 检查区块是否已加载
     *
     * @param player 目标玩家（用于获取世界信息）
     * @param chunkX 区块 X 坐标
     * @param chunkZ 区块 Z 坐标
     * @return 如果区块已加载则返回 true
     */
    boolean isChunkLoaded(@Nonnull Player player, int chunkX, int chunkZ);

    // ==================== 实体数据包创建 ====================

    /**
     * 创建添加实体数据包
     *
     * @param entityId 实体 ID
     * @param uuid     实体 UUID
     * @param location 实体位置
     * @param type     实体类型
     * @return 添加实体数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createAddEntityPacket(int entityId, @Nonnull UUID uuid, @Nonnull Location location, @Nonnull EntityType type);

    /**
     * 创建移除实体数据包
     *
     * @param entityIds 要移除的实体 ID 数组
     * @return 移除实体数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createRemoveEntityPacket(int... entityIds);

    /**
     * 创建移除实体数据包
     *
     * @param entityIds 要移除的实体 ID 列表
     * @return 移除实体数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createRemoveEntityPacket(@Nonnull List<Integer> entityIds);

    // ==================== 玩家信息数据包 ====================

    /**
     * 创建玩家信息添加数据包
     *
     * @param profile     玩家资料
     * @param displayName 显示名称
     * @param showInTab   是否在 Tab 列表中显示
     * @return 玩家信息更新数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createPlayerInfoAddPacket(@Nonnull GameProfile profile, @Nonnull String displayName, boolean showInTab);

    /**
     * 创建玩家信息添加数据包（带皮肤）
     *
     * @param profile       玩家资料
     * @param displayName   显示名称
     * @param showInTab     是否在 Tab 列表中显示
     * @param skinValue     皮肤值
     * @param skinSignature 皮肤签名
     * @return 玩家信息更新数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createPlayerInfoAddPacket(@Nonnull GameProfile profile, @Nonnull String displayName,
                                        boolean showInTab, @Nullable String skinValue, @Nullable String skinSignature);

    /**
     * 创建玩家信息移除数据包
     *
     * @param uuid 要移除的玩家 UUID
     * @return 玩家信息移除数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createPlayerInfoRemovePacket(@Nonnull UUID uuid);

    // ==================== 位置和旋转数据包 ====================

    /**
     * 创建传送数据包
     *
     * @param entityId 实体 ID
     * @param location 目标位置
     * @param yaw      偏航角
     * @param pitch    俯仰角
     * @return 传送数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createTeleportPacket(int entityId, @Nonnull Location location, float yaw, float pitch);

    /**
     * 创建头部旋转数据包
     *
     * @param entity 实体对象
     * @param yaw    偏航角
     * @return 头部旋转数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createRotateHeadPacket(@Nonnull Entity entity, float yaw);

    /**
     * 创建头部旋转数据包（纯数据包模式，不需要实体对象）
     *
     * @param entityId 实体 ID
     * @param yaw      偏航角
     * @return 头部旋转数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createRotateHeadPacket(int entityId, float yaw);

    /**
     * 创建动画数据包（纯数据包模式，不需要实体对象）
     *
     * @param entityId    实体 ID
     * @param animationId 动画 ID
     * @return 动画数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createAnimatePacket(int entityId, int animationId);

    // ==================== 装备数据包 ====================

    /**
     * 创建装备数据包（Bukkit 物品）
     *
     * @param entityId  实体 ID
     * @param equipment 装备映射（槽位 -> 物品）
     * @return 装备数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createSetEquipmentPacket(int entityId, @Nonnull Map<EquipmentSlot, org.bukkit.inventory.ItemStack> equipment);

    /**
     * 创建装备数据包（NMS 物品）
     *
     * @param entityId  实体 ID
     * @param equipment 装备映射（槽位 -> NMS 物品）
     * @return 装备数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createSetEquipmentPacketNMS(int entityId, @Nonnull Map<EquipmentSlot, ItemStack> equipment);

    // ==================== 实体数据数据包 ====================

    /**
     * 创建实体数据更新数据包
     *
     * @param entityId  实体 ID
     * @param dataItems 数据项列表
     * @return 实体数据数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createSetEntityDataPacket(int entityId, @Nonnull List<SynchedEntityData.DataValue<?>> dataItems);

    // ==================== 团队数据包 ====================

    /**
     * 创建团队数据包
     *
     * @param teamName   团队名称
     * @param playerName 玩家名称
     * @param color      团队颜色
     * @param create     是否创建新团队
     * @return 团队数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createTeamPacket(@Nonnull String teamName, @Nonnull String playerName,
                               @Nonnull org.bukkit.ChatColor color, boolean create);

    /**
     * 创建团队数据包（带碰撞规则）
     *
     * @param teamName           团队名称
     * @param playerName         玩家名称
     * @param color              团队颜色
     * @param create             是否创建新团队
     * @param collisionRule      碰撞规则
     * @param nameTagVisibility  名称标签可见性
     * @return 团队数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createTeamPacket(@Nonnull String teamName, @Nonnull String playerName,
                               @Nonnull org.bukkit.ChatColor color, boolean create,
                               @Nullable net.minecraft.world.scores.Team.CollisionRule collisionRule,
                               @Nullable net.minecraft.world.scores.Team.Visibility nameTagVisibility);

    /**
     * 创建移除团队数据包
     *
     * @param teamName 团队名称
     * @return 移除团队数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createRemoveTeamPacket(@Nonnull String teamName);

    // ==================== 动画数据包 ====================

    /**
     * 创建动画数据包
     *
     * @param entity      实体对象
     * @param animationId 动画 ID
     * @return 动画数据包
     */
    @Nonnull
    Packet<? super ClientGamePacketListener> createAnimatePacket(@Nonnull Entity entity, int animationId);

    // ==================== 实体操作 ====================

    /**
     * 获取实体 ID
     *
     * @param entity 实体对象
     * @return 实体 ID
     */
    int getEntityId(@Nonnull Entity entity);

    /**
     * 获取实体的 NMS 对象
     *
     * @param player Bukkit 玩家对象
     * @return NMS ServerPlayer 对象
     */
    @Nonnull
    net.minecraft.server.level.ServerPlayer getServerPlayer(@Nonnull Player player);

    /**
     * 获取玩家所在的服务端世界
     *
     * @param player NMS ServerPlayer 对象
     * @return ServerLevel 对象，如果获取失败返回 null
     */
    @Nullable
    net.minecraft.server.level.ServerLevel getServerLevel(@Nonnull net.minecraft.server.level.ServerPlayer player);

    // ==================== 物品转换 ====================

    /**
     * 将 Bukkit 物品转换为 NMS 物品
     *
     * @param itemStack Bukkit 物品
     * @return NMS 物品
     */
    @Nonnull
    ItemStack toNMSItemStack(@Nonnull org.bukkit.inventory.ItemStack itemStack);

    /**
     * 将 NMS 物品转换为 Bukkit 物品
     *
     * @param itemStack NMS 物品
     * @return Bukkit 物品
     */
    @Nonnull
    org.bukkit.inventory.ItemStack toBukkitItemStack(@Nonnull ItemStack itemStack);

    // ==================== GameProfile 操作 ====================

    /**
     * 创建带皮肤的 GameProfile
     *
     * @param uuid         UUID
     * @param name         名称
     * @param skinValue    皮肤值
     * @param skinSignature 皮肤签名
     * @return GameProfile 对象
     */
    @Nonnull
    GameProfile createGameProfileWithSkin(@Nonnull UUID uuid, @Nonnull String name,
                                          @Nullable String skinValue, @Nullable String skinSignature);

    /**
     * 获取 GameProfile 的 UUID
     *
     * @param profile GameProfile 对象
     * @return UUID，如果获取失败返回 null
     */
    @Nullable
    UUID getProfileId(@Nonnull GameProfile profile);

    /**
     * 获取 GameProfile 的名称
     *
     * @param profile GameProfile 对象
     * @return 名称，如果获取失败返回空字符串
     */
    @Nonnull
    String getProfileName(@Nonnull GameProfile profile);

    /**
     * 获取 GameProfile 的属性
     *
     * @param profile GameProfile 对象
     * @return PropertyMap，如果获取失败返回 null
     */
    @Nullable
    PropertyMap getProfileProperties(@Nonnull GameProfile profile);

    // ==================== 调度器支持 ====================

    /**
     * 在玩家的调度器上运行任务（支持 Folia）
     *
     * @param player 玩家
     * @param task   任务
     */
    void runOnPlayerScheduler(@Nonnull Player player, @Nonnull Runnable task);

    // ==================== 缓存管理 ====================

    /**
     * 清理指定玩家的区块可见性缓存
     * 在玩家退出服务器时调用，防止内存泄漏
     *
     * @param playerId 玩家 UUID
     */
    void cleanupPlayerCache(@Nonnull UUID playerId);

    // ==================== 版本特性检测 ====================

    /**
     * 检查是否支持 Happy Ghast 实体
     * 仅在 1.21.6+ 版本可用
     *
     * @return 如果支持则返回 true
     */
    default boolean supportsHappyGhast() {
        return false;
    }

    /**
     * 检查是否支持新的 PropertyMap 构造方式
     * 1.21.11+ 版本使用新的 PropertyMap 构造函数
     *
     * @return 如果支持则返回 true
     */
    default boolean supportsNewPropertyMap() {
        return false;
    }

    /**
     * 检查是否需要使用 FakeSynchronizer
     * 1.21.9+ 和 1.21.11+ 版本需要
     *
     * @return 如果需要则返回 true
     */
    default boolean requiresFakeSynchronizer() {
        return false;
    }
}
