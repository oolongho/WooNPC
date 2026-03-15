package com.oolonghoo.woonpc.listener;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.npc.Npc;
import com.oolonghoo.woonpc.tracker.VisibilityTracker;
import com.oolonghoo.woonpc.version.VersionAdapterFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 玩家退出监听器
 * 当玩家退出服务器时，清理相关数据，防止内存泄漏
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
        UUID playerId = player.getUniqueId();

        // 清理可见性追踪器中的所有玩家缓存
        VisibilityTracker visibilityTracker = plugin.getVisibilityTracker();
        if (visibilityTracker != null) {
            visibilityTracker.cleanupPlayer(playerId);
        }

        // 清理版本适配器中的区块可见性缓存
        try {
            VersionAdapterFactory.getAdapter().cleanupPlayerCache(playerId);
        } catch (Exception ignored) {
            // 版本适配器可能未初始化
        }

        // 清理所有 NPC 中该玩家的缓存数据
        int npcCount = 0;
        for (Npc npc : plugin.getAllNpcs()) {
            npc.cleanupPlayer(playerId);
            npcCount++;
        }

        // 记录调试信息
        if (plugin.getConfigLoader().isDebug()) {
            final int finalNpcCount = npcCount;
            plugin.getLogger().info(() -> "[PlayerQuitListener] 已清理玩家 " + player.getName() +
                    " 的缓存数据，涉及 " + finalNpcCount + " 个 NPC");
        }
    }
}
