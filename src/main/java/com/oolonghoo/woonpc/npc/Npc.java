package com.oolonghoo.woonpc.npc;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * NPC 抽象基类
 * 定义 NPC 的核心行为接口
 * 
 * @author oolongho
 */
public abstract class Npc {
    
    // 用于生成随机本地名称的字符集
    private static final char[] LOCAL_NAME_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r'
    };
    
    // 玩家状态追踪
    protected final Map<UUID, Boolean> isTeamCreated = new ConcurrentHashMap<>();
    protected final Map<UUID, Boolean> isVisibleForPlayer = new ConcurrentHashMap<>();
    protected final Map<UUID, Boolean> isLookingAtPlayer = new ConcurrentHashMap<>();
    protected final Map<UUID, Long> lastPlayerInteraction = new ConcurrentHashMap<>();
    
    // 可见性变化监听器
    private final List<VisibilityChangeListener> visibilityChangeListeners = new CopyOnWriteArrayList<>();
    
    // NPC 数据
    protected NpcData data;
    protected boolean saveToFile;
    
    /**
     * 可见性变化监听器接口
     */
    @FunctionalInterface
    public interface VisibilityChangeListener {
        /**
         * 当 NPC 对玩家的可见性发生变化时调用
         * 
         * @param npc NPC 对象
         * @param player 玩家
         * @param visible 是否可见
         * @param visiblePlayerCount 当前可见玩家数量
         */
        void onVisibilityChange(Npc npc, Player player, boolean visible, int visiblePlayerCount);
    }
    
    /**
     * 构造函数
     * 
     * @param data NPC 数据
     */
    public Npc(NpcData data) {
        this.data = data;
        this.saveToFile = true;
    }
    
    /**
     * 生成随机本地名称
     * 用于在玩家列表中显示的唯一标识
     * 
     * @return 带颜色代码的随机名称
     */
    protected String generateLocalName() {
        StringBuilder localName = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            localName.append('&').append(LOCAL_NAME_CHARS[random.nextInt(LOCAL_NAME_CHARS.length)]);
        }
        // 将 & 颜色代码转换为 § 颜色代码
        return LegacyComponentSerializer.legacySection().serialize(
            LegacyComponentSerializer.legacyAmpersand().deserialize(localName.toString())
        );
    }
    
    // ==================== 抽象方法 ====================
    
    /**
     * 创建 NPC 实体
     * 在服务端创建实际的实体对象
     */
    public abstract void create();
    
    /**
     * 为指定玩家生成 NPC
     * 发送创建数据包给玩家客户端
     * 
     * @param player 目标玩家
     */
    public abstract void spawn(Player player);
    
    /**
     * 为指定玩家移除 NPC
     * 发送移除数据包给玩家客户端
     * 
     * @param player 目标玩家
     */
    public abstract void remove(Player player);
    
    /**
     * 让 NPC 看向指定位置
     * 
     * @param player  目标玩家
     * @param location 看向的位置
     */
    public abstract void lookAt(Player player, Location location);
    
    /**
     * 更新 NPC 属性
     * 发送属性更新数据包给玩家客户端
     * 
     * @param player    目标玩家
     * @param swingArm  是否挥动手臂
     */
    public abstract void update(Player player, boolean swingArm);
    
    /**
     * 移动 NPC 到新位置
     * 
     * @param player   目标玩家
     * @param swingArm 是否挥动手臂
     */
    public abstract void move(Player player, boolean swingArm);
    
    /**
     * 刷新实体数据
     * 发送实体元数据更新
     * 
     * @param player 目标玩家
     */
    protected abstract void refreshEntityData(Player player);
    
    /**
     * 获取实体 ID
     * 
     * @return 实体 ID
     */
    public abstract int getEntityId();
    
    /**
     * 获取眼睛高度
     * 
     * @return 眼睛高度
     */
    public abstract float getEyeHeight();
    
    // ==================== 默认实现 ====================
    
    /**
     * 为所有在线玩家生成 NPC
     */
    public void spawnForAll() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            spawn(onlinePlayer);
        }
    }
    
    /**
     * 为所有在线玩家移除 NPC
     */
    public void removeForAll() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            remove(onlinePlayer);
        }
    }
    
    /**
     * 更新 NPC 属性 (使用默认挥臂设置)
     * 
     * @param player 目标玩家
     */
    public void update(Player player) {
        update(player, false);
    }
    
    /**
     * 为所有在线玩家更新 NPC
     * 
     * @param swingArm 是否挥动手臂
     */
    public void updateForAll(boolean swingArm) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            update(onlinePlayer, swingArm);
        }
    }
    
    /**
     * 为所有在线玩家更新 NPC (使用默认挥臂设置)
     */
    public void updateForAll() {
        updateForAll(false);
    }
    
    /**
     * 移动 NPC 到新位置 (使用默认挥臂设置)
     * 
     * @param player 目标玩家
     */
    public void move(Player player) {
        move(player, false);
    }
    
    /**
     * 为所有在线玩家移动 NPC
     * 
     * @param swingArm 是否挥动手臂
     */
    public void moveForAll(boolean swingArm) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            move(onlinePlayer, swingArm);
        }
    }
    
    /**
     * 为所有在线玩家移动 NPC (使用默认挥臂设置)
     */
    public void moveForAll() {
        moveForAll(false);
    }
    
    /**
     * 检查 NPC 是否应该对玩家可见
     * 
     * @param player 目标玩家
     * @return 是否应该可见
     */
    protected boolean shouldBeVisible(Player player) {
        // 检查位置是否有效
        if (data.getLocation() == null) {
            return false;
        }
        
        // 检查是否在同一世界
        if (player.getLocation().getWorld() != data.getLocation().getWorld()) {
            return false;
        }
        
        // 检查可见距离
        int visibilityDistance = data.getVisibilityDistance();
        if (visibilityDistance > 0) {
            double distanceSquared = data.getLocation().distanceSquared(player.getLocation());
            if (distanceSquared > visibilityDistance * visibilityDistance) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查并更新 NPC 对玩家的可见性
     * 
     * @param player 目标玩家
     */
    public void checkAndUpdateVisibility(Player player) {
        boolean shouldBeVisible = shouldBeVisible(player);
        boolean wasVisible = isVisibleForPlayer.getOrDefault(player.getUniqueId(), false);
        
        if (shouldBeVisible && !wasVisible) {
            spawn(player);
        } else if (!shouldBeVisible && wasVisible) {
            remove(player);
        }
    }
    
    /**
     * 检查并更新所有玩家的可见性
     */
    public void checkAndUpdateVisibilityForAll() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            checkAndUpdateVisibility(onlinePlayer);
        }
    }
    
    // ==================== Getters ====================
    
    /**
     * 获取 NPC 数据
     * 
     * @return NPC 数据
     */
    public NpcData getData() {
        return data;
    }
    
    /**
     * 获取 Team 创建状态映射
     * 
     * @return Team 创建状态
     */
    public Map<UUID, Boolean> getIsTeamCreated() {
        return isTeamCreated;
    }
    
    /**
     * 获取玩家可见性映射
     * 
     * @return 玩家可见性
     */
    public Map<UUID, Boolean> getIsVisibleForPlayer() {
        return isVisibleForPlayer;
    }
    
    /**
     * 检查 NPC 是否对玩家显示
     * 
     * @param player 目标玩家
     * @return 是否显示
     */
    public boolean isShownFor(Player player) {
        return isVisibleForPlayer.getOrDefault(player.getUniqueId(), false);
    }
    
    /**
     * 获取玩家看向状态映射
     * 
     * @return 玩家看向状态
     */
    public Map<UUID, Boolean> getIsLookingAtPlayer() {
        return isLookingAtPlayer;
    }
    
    /**
     * 获取玩家最后交互时间映射
     * 
     * @return 最后交互时间
     */
    public Map<UUID, Long> getLastPlayerInteraction() {
        return lastPlayerInteraction;
    }
    
    /**
     * 检查 NPC 数据是否已修改
     * 
     * @return 是否已修改
     */
    public boolean isDirty() {
        return data.isDirty();
    }
    
    /**
     * 设置 NPC 数据修改标记
     * 
     * @param dirty 是否已修改
     */
    public void setDirty(boolean dirty) {
        data.setDirty(dirty);
    }
    
    /**
     * 检查是否需要保存到文件
     * 
     * @return 是否需要保存
     */
    public boolean isSaveToFile() {
        return saveToFile;
    }
    
    /**
     * 设置是否保存到文件
     * 
     * @param saveToFile 是否保存
     */
    public void setSaveToFile(boolean saveToFile) {
        this.saveToFile = saveToFile;
    }
    
    /**
     * 获取 NPC 名称
     * 
     * @return NPC 名称
     */
    public String getName() {
        return data.getName();
    }
    
    /**
     * 获取 NPC 位置
     * 
     * @return NPC 位置
     */
    public Location getLocation() {
        return data.getLocation();
    }
    
    /**
     * 设置 NPC 位置
     * 
     * @param location 新位置
     */
    public void setLocation(Location location) {
        data.setLocation(location);
    }
    
    /**
     * 添加可见性变化监听器
     * 
     * @param listener 监听器
     */
    public void addVisibilityChangeListener(VisibilityChangeListener listener) {
        visibilityChangeListeners.add(listener);
    }
    
    /**
     * 移除可见性变化监听器
     * 
     * @param listener 监听器
     */
    public void removeVisibilityChangeListener(VisibilityChangeListener listener) {
        visibilityChangeListeners.remove(listener);
    }
    
    /**
     * 获取当前可见玩家数量
     * 
     * @return 可见玩家数量
     */
    public int getVisiblePlayerCount() {
        int count = 0;
        for (Boolean visible : isVisibleForPlayer.values()) {
            if (visible) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 检查是否有可见玩家
     * 
     * @return 是否有可见玩家
     */
    public boolean hasVisiblePlayers() {
        for (Boolean visible : isVisibleForPlayer.values()) {
            if (visible) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 触发可见性变化事件
     * 
     * @param player 玩家
     * @param visible 是否可见
     */
    protected void fireVisibilityChangeEvent(Player player, boolean visible) {
        int visibleCount = getVisiblePlayerCount();
        for (VisibilityChangeListener listener : visibilityChangeListeners) {
            try {
                listener.onVisibilityChange(this, player, visible, visibleCount);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[WooNPC] 可见性监听器执行异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 更新 NPC 属性
     */
    public void update() {
        updateForAll();
    }
}
