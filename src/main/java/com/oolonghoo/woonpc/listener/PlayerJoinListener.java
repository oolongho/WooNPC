package com.oolonghoo.woonpc.listener;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.tracker.VisibilityTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 玩家加入监听器
 * 当玩家加入服务器时，为其生成可见范围内的 NPC
 * 
 * @author oolongho
 */
public class PlayerJoinListener implements Listener {

    private final WooNPC plugin;

    public PlayerJoinListener(WooNPC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 添加到加入延迟集合
        // 这可以防止玩家刚加入时出现 NPC 闪烁的问题
        VisibilityTracker visibilityTracker = plugin.getVisibilityTracker();
        if (visibilityTracker != null) {
            visibilityTracker.addJoinDelayPlayer(player.getUniqueId());
        }
    }
}
