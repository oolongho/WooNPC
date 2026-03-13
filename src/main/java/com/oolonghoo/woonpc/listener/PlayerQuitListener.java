package com.oolonghoo.woonpc.listener;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.npc.Npc;
import com.oolonghoo.woonpc.tracker.VisibilityTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家退出监听器
 * 当玩家退出服务器时，清理相关数据
 * 
 */
public class PlayerQuitListener implements Listener {

    private final WooNPC plugin;

    public PlayerQuitListener(WooNPC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 从可见性追踪器中移除
        VisibilityTracker visibilityTracker = plugin.getVisibilityTracker();
        if (visibilityTracker != null) {
            visibilityTracker.removeJoinDelayPlayer(player.getUniqueId());
        }
        
        // 从所有 NPC 的已生成列表中移除该玩家
        for (Npc npc : plugin.getAllNpcs()) {
            // 清理玩家的追踪状态
            npc.getIsVisibleForPlayer().remove(player.getUniqueId());
            npc.getIsTeamCreated().remove(player.getUniqueId());
            npc.getIsLookingAtPlayer().remove(player.getUniqueId());
            npc.getLastPlayerInteraction().remove(player.getUniqueId());
        }
    }
}
