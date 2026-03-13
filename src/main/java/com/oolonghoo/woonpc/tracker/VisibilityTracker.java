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
 * 优化:
 * - 按世界分组 NPC，避免跨世界检测
 * - 缓存玩家位置，减少 getLocation() 调用
 * - 批量处理可见性更新
 * 
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
    
    // 世界 -> NPC 列表缓存
    private final Map<String, List<Npc>> worldNpcCache = new ConcurrentHashMap<>();
    
    // 玩家位置缓存 (UUID -> Location)
    private final Map<UUID, Location> playerLocationCache = new ConcurrentHashMap<>();
    
    // 缓存更新计数器
    private int cacheUpdateCounter = 0;
    private static final int CACHE_UPDATE_INTERVAL = 20; // 每 20 次检测更新一次世界缓存

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
        
        // 初始化世界缓存
        rebuildWorldCache();
        
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
        playerLocationCache.clear();
        worldNpcCache.clear();
    }
    
    /**
     * 重建世界缓存
     */
    public void rebuildWorldCache() {
        worldNpcCache.clear();
        
        for (Npc npc : plugin.getAllNpcs()) {
            NpcData data = npc.getData();
            Location loc = data.getLocation();
            if (loc != null && loc.getWorld() != null) {
                String worldName = loc.getWorld().getName();
                worldNpcCache.computeIfAbsent(worldName, k -> new ArrayList<>()).add(npc);
            }
        }
    }
    
    /**
     * 添加 NPC 到世界缓存
     */
    public void addNpcToWorldCache(Npc npc) {
        NpcData data = npc.getData();
        Location loc = data.getLocation();
        if (loc != null && loc.getWorld() != null) {
            String worldName = loc.getWorld().getName();
            worldNpcCache.computeIfAbsent(worldName, k -> new ArrayList<>()).add(npc);
        }
    }
    
    /**
     * 从世界缓存移除 NPC
     */
    public void removeNpcFromWorldCache(Npc npc) {
        for (List<Npc> npcs : worldNpcCache.values()) {
            npcs.remove(npc);
        }
    }

    @Override
    public void run() {
        // 定期重建世界缓存
        cacheUpdateCounter++;
        if (cacheUpdateCounter >= CACHE_UPDATE_INTERVAL) {
            cacheUpdateCounter = 0;
            rebuildWorldCache();
        }
        
        // 在主线程获取玩家位置（线程安全）
        // 由于这是异步任务，需要通过调度器在主线程获取位置
        if (Bukkit.isPrimaryThread()) {
            updatePlayerLocations();
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                updatePlayerLocations();
            });
        }
        
        // 按世界处理
        for (Map.Entry<String, List<Npc>> entry : worldNpcCache.entrySet()) {
            String worldName = entry.getKey();
            List<Npc> npcs = entry.getValue();
            
            if (npcs.isEmpty()) continue;
            
            // 获取该世界的玩家
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;
            
            for (Player player : world.getPlayers()) {
                // 跳过正在加入延迟的玩家
                if (joinDelayPlayers.contains(player.getUniqueId())) {
                    continue;
                }
                
                // 检查玩家是否有效
                if (!player.isOnline()) {
                    continue;
                }
                
                // 使用缓存的位置
                Location playerLocation = playerLocationCache.get(player.getUniqueId());
                if (playerLocation == null) {
                    continue;
                }
                
                // 更新该世界中所有 NPC 对该玩家的可见性
                for (Npc npc : npcs) {
                    checkAndUpdateVisibility(npc, player, playerLocation);
                }
            }
        }
        
        // 清理离线玩家的位置缓存
        Set<UUID> onlinePlayers = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayers.add(player.getUniqueId());
        }
        playerLocationCache.keySet().retainAll(onlinePlayers);
    }
    
    /**
     * 更新玩家位置缓存（必须在主线程调用）
     */
    private void updatePlayerLocations() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != null && player.isOnline()) {
                playerLocationCache.put(player.getUniqueId(), player.getLocation().clone());
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
        Location playerLocation = playerLocationCache.get(player.getUniqueId());
        if (playerLocation == null) {
            playerLocation = player.getLocation();
        }
        checkAndUpdateVisibility(npc, player, playerLocation);
    }
    
    /**
     * 检查并更新 NPC 对玩家的可见性（使用缓存的位置）
     * 
     * @param npc NPC 对象
     * @param player 玩家
     * @param playerLocation 玩家位置（缓存）
     */
    private void checkAndUpdateVisibility(@NotNull Npc npc, @NotNull Player player, @NotNull Location playerLocation) {
        boolean shouldBeVisible = shouldBeVisible(npc, player, playerLocation);
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
    private boolean shouldBeVisible(@NotNull Npc npc, @NotNull Player player, @NotNull Location playerLocation) {
        NpcData data = npc.getData();
        Location npcLocation = data.getLocation();
        
        if (npcLocation == null || npcLocation.getWorld() == null) {
            return false;
        }
        
        if (playerLocation.getWorld() == null) {
            return false;
        }
        
        if (npcLocation.getWorld() != playerLocation.getWorld()) {
            return false;
        }
        
        int visibilityDistance = data.getVisibilityDistance();
        double effectiveDistanceSquared;
        
        if (visibilityDistance > 0) {
            effectiveDistanceSquared = (double) visibilityDistance * visibilityDistance;
        } else {
            effectiveDistanceSquared = defaultVisibilityDistanceSquared;
        }
        
        double distanceSquared = npcLocation.distanceSquared(playerLocation);
        
        if (distanceSquared > effectiveDistanceSquared) {
            return false;
        }
        
        int chunkX = ((int) npcLocation.getX()) >> 4;
        int chunkZ = ((int) npcLocation.getZ()) >> 4;
        
        if (!npcLocation.getWorld().isChunkLoaded(chunkX, chunkZ)) {
            return false;
        }
        
        return true;
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
        NpcData data = npc.getData();
        Location npcLocation = data.getLocation();
        
        if (npcLocation == null || npcLocation.getWorld() == null) {
            return;
        }
        
        World world = npcLocation.getWorld();
        for (Player player : world.getPlayers()) {
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
    
    /**
     * 获取世界缓存统计信息
     * 
     * @return 世界数量
     */
    public int getWorldCacheSize() {
        return worldNpcCache.size();
    }
}
