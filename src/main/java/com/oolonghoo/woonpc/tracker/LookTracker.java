package com.oolonghoo.woonpc.tracker;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.config.ConfigLoader;
import com.oolonghoo.woonpc.npc.Npc;
import com.oolonghoo.woonpc.npc.NpcData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Map;

/**
 * 头部旋转追踪器
 * 让 NPC 的头部视角跟随附近玩家的位置
 * 
 */
public class LookTracker implements Runnable {

    private final WooNPC plugin;
    private BukkitTask task;
    private int defaultTurnToPlayerDistance;
    
    /**
     * 实体眼睛高度映射
     * 基于 Minecraft 默认实体眼睛高度
     */
    private static final Map<EntityType, Double> EYE_HEIGHTS = Map.ofEntries(
            Map.entry(EntityType.PLAYER, 1.62),
            Map.entry(EntityType.ZOMBIE, 1.74),
            Map.entry(EntityType.SKELETON, 1.74),
            Map.entry(EntityType.STRAY, 1.74),
            Map.entry(EntityType.HUSK, 1.74),
            Map.entry(EntityType.DROWNED, 1.74),
            Map.entry(EntityType.WITHER_SKELETON, 1.74),
            Map.entry(EntityType.CREEPER, 1.7),
            Map.entry(EntityType.ENDERMAN, 2.55),
            Map.entry(EntityType.SPIDER, 0.5),
            Map.entry(EntityType.CAVE_SPIDER, 0.5),
            Map.entry(EntityType.PIG, 0.6),
            Map.entry(EntityType.SHEEP, 0.65),
            Map.entry(EntityType.COW, 1.3),
            Map.entry(EntityType.MOOSHROOM, 1.3),
            Map.entry(EntityType.CHICKEN, 0.4),
            Map.entry(EntityType.HORSE, 1.52),
            Map.entry(EntityType.DONKEY, 1.52),
            Map.entry(EntityType.MULE, 1.52),
            Map.entry(EntityType.VILLAGER, 1.62),
            Map.entry(EntityType.ZOMBIE_VILLAGER, 1.62),
            Map.entry(EntityType.IRON_GOLEM, 2.7),
            Map.entry(EntityType.WOLF, 0.68),
            Map.entry(EntityType.CAT, 0.35),
            Map.entry(EntityType.OCELOT, 0.35),
            Map.entry(EntityType.RABBIT, 0.3),
            Map.entry(EntityType.BAT, 0.45),
            Map.entry(EntityType.SQUID, 0.4),
            Map.entry(EntityType.GLOW_SQUID, 0.4),
            Map.entry(EntityType.SILVERFISH, 0.13),
            Map.entry(EntityType.ENDERMITE, 0.13),
            Map.entry(EntityType.BLAZE, 1.7),
            Map.entry(EntityType.GHAST, 2.0),
            Map.entry(EntityType.SLIME, 0.5),
            Map.entry(EntityType.MAGMA_CUBE, 0.5),
            Map.entry(EntityType.WITCH, 1.62),
            Map.entry(EntityType.EVOKER, 1.62),
            Map.entry(EntityType.VINDICATOR, 1.62),
            Map.entry(EntityType.ILLUSIONER, 1.62),
            Map.entry(EntityType.PILLAGER, 1.62),
            Map.entry(EntityType.VEX, 0.8),
            Map.entry(EntityType.GUARDIAN, 0.425),
            Map.entry(EntityType.ELDER_GUARDIAN, 0.425),
            Map.entry(EntityType.SHULKER, 0.5),
            Map.entry(EntityType.PHANTOM, 0.5),
            Map.entry(EntityType.BEE, 0.3),
            Map.entry(EntityType.FOX, 0.4),
            Map.entry(EntityType.PANDA, 1.13),
            Map.entry(EntityType.STRIDER, 1.7),
            Map.entry(EntityType.HOGLIN, 1.4),
            Map.entry(EntityType.ZOGLIN, 1.4),
            Map.entry(EntityType.PIGLIN, 1.62),
            Map.entry(EntityType.PIGLIN_BRUTE, 1.62),
            Map.entry(EntityType.ZOMBIFIED_PIGLIN, 1.62),
            Map.entry(EntityType.AXOLOTL, 0.3),
            Map.entry(EntityType.GOAT, 0.9),
            Map.entry(EntityType.ALLAY, 0.6),
            Map.entry(EntityType.FROG, 0.25),
            Map.entry(EntityType.TADPOLE, 0.13),
            Map.entry(EntityType.WARDEN, 2.5),
            Map.entry(EntityType.CAMEL, 2.275),
            Map.entry(EntityType.SNIFFER, 1.0),
            Map.entry(EntityType.BREEZE, 1.4),
            Map.entry(EntityType.ARMADILLO, 0.26),
            Map.entry(EntityType.BOGGED, 1.74),
            Map.entry(EntityType.ARMOR_STAND, 1.975)
    );
    
    public LookTracker(WooNPC plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 启动追踪器
     */
    public void start() {
        ConfigLoader config = plugin.getConfigLoader();
        this.defaultTurnToPlayerDistance = config.getTurnToPlayerDistance();
        
        int interval = config.getLookUpdateInterval();
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, interval, interval);
        
        plugin.getLogger().info(() -> "头部旋转追踪器已启动，检测间隔: " + interval + " tick");
    }
    
    /**
     * 停止追踪器
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
    
    @Override
    public void run() {
        Collection<Npc> npcs = plugin.getAllNpcs();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLocation = player.getLocation();
            
            for (Npc npc : npcs) {
                NpcData npcData = npc.getData();
                Location npcLocation = npcData.getLocation();
                
                // 检查位置是否有效
                if (npcLocation == null || npcLocation.getWorld() == null) {
                    continue;
                }
                
                // 检查是否在同一世界
                if (!npcLocation.getWorld().getName().equalsIgnoreCase(playerLocation.getWorld().getName())) {
                    continue;
                }
                
                // 检查 NPC 是否对玩家可见
                if (!npc.isShownFor(player)) {
                    continue;
                }
                
                // 计算距离平方（避免开方运算，提升性能）
                double distanceSquared = playerLocation.distanceSquared(npcLocation);
                if (Double.isNaN(distanceSquared)) {
                    continue;
                }
                
                // 获取 NPC 特定的转向距离或使用默认值
                int npcTurnDistance = npcData.getTurnToPlayerDistance();
                int effectiveTurnDistance = (npcTurnDistance == -1) ? defaultTurnToPlayerDistance : npcTurnDistance;
                double effectiveTurnDistanceSquared = (double) effectiveTurnDistance * effectiveTurnDistance;
                
                // 检查是否启用了转向玩家
                if (npcData.isTurnToPlayer() && distanceSquared < effectiveTurnDistanceSquared) {
                    // 更新 NPC 看向玩家
                    updateNpcLookAtPlayer(npc, player, npcData, npcLocation);
                    
                    // 更新看向状态
                    npc.getIsLookingAtPlayer().put(player.getUniqueId(), true);
                } else if (npcData.isTurnToPlayer()) {
                    // 超出范围，重置看向状态
                    npc.getIsLookingAtPlayer().put(player.getUniqueId(), false);
                }
            }
        }
    }
    
    /**
     * 更新 NPC 看向玩家
     * 
     * @param npc NPC 对象
     * @param player 玩家
     * @param npcData NPC 数据
     * @param npcLocation NPC 位置
     */
    private void updateNpcLookAtPlayer(Npc npc, Player player, NpcData npcData, Location npcLocation) {
        // 获取实体的基础眼睛高度
        double baseEyeHeight = getEyeHeight(npcData.getType());
        
        // 根据缩放调整眼睛高度
        float scale = npcData.getScale();
        double adjustedEyeHeight = baseEyeHeight * scale;
        
        // 计算 NPC 眼睛位置
        Location npcEyeLocation = npcLocation.clone();
        npcEyeLocation.setY(npcLocation.getY() + adjustedEyeHeight);
        
        // 获取玩家眼睛位置
        Location playerEyeLocation = player.getEyeLocation();
        
        // 计算从 NPC 眼睛到玩家眼睛的方向
        Location targetLocation = playerEyeLocation.clone();
        targetLocation.setDirection(targetLocation.toVector().subtract(npcEyeLocation.toVector()));
        
        // 让 NPC 看向目标位置
        npc.lookAt(player, targetLocation);
    }
    
    /**
     * 获取实体类型的眼睛高度
     * 
     * @param type 实体类型
     * @return 眼睛高度
     */
    public double getEyeHeight(EntityType type) {
        return EYE_HEIGHTS.getOrDefault(type, 1.62);
    }
    
    /**
     * 获取默认转向距离
     * 
     * @return 默认转向距离
     */
    public int getDefaultTurnToPlayerDistance() {
        return defaultTurnToPlayerDistance;
    }
}
