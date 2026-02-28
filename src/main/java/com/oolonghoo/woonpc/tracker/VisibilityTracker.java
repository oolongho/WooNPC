package com.oolonghoo.woonpc.tracker;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.config.ConfigLoader;
import com.oolonghoo.woonpc.npc.Npc;
import com.oolonghoo.woonpc.npc.NpcData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可见性追踪器
 * 定期检测玩家与 NPC 的距离，自动生成或移除 NPC
 * 
 * @author oolongho
 */
public class VisibilityTracker implements Runnable {

    private final WooNPC plugin;
    private BukkitTask task;
    
    // 默认可见距离
    private int defaultVisibilityDistance;
    private double defaultVisibilityDistanceSquared;
    
    // 加入延迟的玩家集合
    private final Set<UUID> joinDelayPlayers;
    
    // 玩家加入延迟时间（tick）
    private int joinDelay;
    
    public VisibilityTracker(WooNPC plugin) {
        this.plugin = plugin;
        this.joinDelayPlayers = ConcurrentHashMap.newKeySet();
    }

    /**
     * 启动追踪器
     */
    public void start() {
        ConfigLoader config = plugin.getConfigLoader();
        this.defaultVisibilityDistance = config.getVisibilityDistance();
        this.defaultVisibilityDistanceSquared = (double) defaultVisibilityDistance * defaultVisibilityDistance;
        this.joinDelay = 20; // 1 秒延迟
        
        int interval = config.getVisibilityCheckInterval();
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, interval, interval);
        
        plugin.getLogger().info("可见性追踪器已启动，检测间隔: " + interval + " tick，可见距离: " + defaultVisibilityDistance);
    }

    /**
     * 停止追踪器
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        joinDelayPlayers.clear();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 跳过正在加入延迟的玩家
            if (joinDelayPlayers.contains(player.getUniqueId())) {
                continue;
            }
            
            // 检查玩家是否有效
            if (!player.isOnline() || player.getWorld() == null) {
                continue;
            }
            
            // 更新所有 NPC 对该玩家的可见性
            for (Npc npc : plugin.getAllNpcs()) {
                checkAndUpdateVisibility(npc, player);
            }
        }
    }
    
    /**
     * 检查并更新 NPC 对玩家的可见性
     * 
     * @param npc NPC 对象
     * @param player 玩家
     */
    public void checkAndUpdateVisibility(@NotNull Npc npc, @NotNull Player player) {
        boolean shouldBeVisible = shouldBeVisible(npc, player);
        boolean wasVisible = npc.isShownFor(player);
        
        if (shouldBeVisible && !wasVisible) {
            // 应该可见但当前不可见，生成 NPC
            npc.spawn(player);
        } else if (!shouldBeVisible && wasVisible) {
            // 不应该可见但当前可见，移除 NPC
            npc.remove(player);
        }
    }
    
    /**
     * 检查 NPC 是否应该对玩家可见
     * 
     * @param npc NPC 对象
     * @param player 玩家
     * @return 是否应该可见
     */
    private boolean shouldBeVisible(@NotNull Npc npc, @NotNull Player player) {
        NpcData data = npc.getData();
        Location npcLocation = data.getLocation();
        
        // 检查位置是否有效
        if (npcLocation == null || npcLocation.getWorld() == null) {
            return false;
        }
        
        // 检查玩家位置是否有效
        Location playerLocation = player.getLocation();
        if (playerLocation.getWorld() == null) {
            return false;
        }
        
        // 检查是否在同一世界
        if (!npcLocation.getWorld().getName().equalsIgnoreCase(playerLocation.getWorld().getName())) {
            return false;
        }
        
        // 获取可见距离
        int visibilityDistance = data.getVisibilityDistance();
        double effectiveDistanceSquared;
        
        if (visibilityDistance > 0) {
            // 使用 NPC 特定的可见距离
            effectiveDistanceSquared = (double) visibilityDistance * visibilityDistance;
        } else {
            // 使用默认可见距离
            effectiveDistanceSquared = defaultVisibilityDistanceSquared;
        }
        
        // 计算距离平方
        double distanceSquared = npcLocation.distanceSquared(playerLocation);
        
        return distanceSquared <= effectiveDistanceSquared;
    }
    
    /**
     * 添加加入延迟的玩家
     * 玩家加入服务器后，延迟一段时间再开始可见性检测
     * 
     * @param playerId 玩家 UUID
     */
    public void addJoinDelayPlayer(@NotNull UUID playerId) {
        joinDelayPlayers.add(playerId);
        
        // 延迟后移除
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            joinDelayPlayers.remove(playerId);
        }, joinDelay);
    }
    
    /**
     * 移除加入延迟的玩家
     * 
     * @param playerId 玩家 UUID
     */
    public void removeJoinDelayPlayer(@NotNull UUID playerId) {
        joinDelayPlayers.remove(playerId);
    }
    
    /**
     * 检查玩家是否在加入延迟中
     * 
     * @param playerId 玩家 UUID
     * @return 是否在延迟中
     */
    public boolean isInJoinDelay(@NotNull UUID playerId) {
        return joinDelayPlayers.contains(playerId);
    }
    
    /**
     * 对所有玩家检查并更新 NPC 可见性
     * 
     * @param npc NPC 对象
     */
    public void checkAndUpdateVisibilityForAll(@NotNull Npc npc) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!joinDelayPlayers.contains(player.getUniqueId())) {
                checkAndUpdateVisibility(npc, player);
            }
        }
    }
    
    /**
     * 强制移除 NPC 对所有玩家的显示
     * 
     * @param npc NPC 对象
     */
    public void forceRemoveForAll(@NotNull Npc npc) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (npc.isShownFor(player)) {
                npc.remove(player);
            }
        }
    }
    
    /**
     * 获取默认可见距离
     * 
     * @return 默认可见距离
     */
    public int getDefaultVisibilityDistance() {
        return defaultVisibilityDistance;
    }
    
    /**
     * 获取加入延迟的玩家数量
     * 
     * @return 玩家数量
     */
    public int getJoinDelayPlayerCount() {
        return joinDelayPlayers.size();
    }
}
