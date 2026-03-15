package com.oolonghoo.woonpc.util;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.config.ConfigLoader;
import com.oolonghoo.woonpc.tracker.VisibilityTracker;

/**
 * 调试管理器
 * 用于输出调试信息
 *
 */
public class DebugManager {

    private final WooNPC plugin;

    public DebugManager(WooNPC plugin) {
        this.plugin = plugin;
    }

    /**
     * 输出调试信息
     *
     * @param message 调试消息
     */
    public void debug(String message) {
        if (isEnabled()) {
            plugin.getLogger().info(() -> "[DEBUG] " + message);
        }
    }

    /**
     * 输出调试信息（带格式）
     *
     * @param format 格式字符串
     * @param args 参数
     */
    public void debug(String format, Object... args) {
        if (isEnabled()) {
            plugin.getLogger().info(() -> "[DEBUG] " + String.format(format, args));
        }
    }

    /**
     * 输出警告信息
     *
     * @param message 警告消息
     */
    public void warn(String message) {
        plugin.getLogger().warning(() -> "[DEBUG] " + message);
    }

    /**
     * 输出错误信息
     *
     * @param message 错误消息
     * @param throwable 异常
     */
    public void error(String message, Throwable throwable) {
        plugin.getLogger().severe(() -> "[DEBUG] " + message);
        if (throwable != null && isEnabled()) {
            plugin.getLogger().severe(() -> "异常详情：" + throwable.getMessage());
            plugin.getLogger().severe("堆栈跟踪：");
            for (StackTraceElement element : throwable.getStackTrace()) {
                plugin.getLogger().severe(() -> "  at " + element.toString());
            }
        }
    }

    /**
     * 检查调试模式是否启用
     */
    public boolean isEnabled() {
        ConfigLoader config = plugin.getConfigLoader();
        return config != null && config.isDebug();
    }

    /**
     * 输出缓存统计信息
     * 用于监控内存使用情况
     */
    public void logCacheStats() {
        if (!isEnabled()) {
            return;
        }

        VisibilityTracker tracker = plugin.getVisibilityTracker();
        if (tracker == null) {
            debug("VisibilityTracker 未初始化");
            return;
        }

        debug("=== 缓存统计信息 ===");
        debug("世界缓存大小: %d", tracker.getWorldCacheSize());
        debug("玩家位置缓存大小: %d", tracker.getPlayerLocationCacheSize());
        debug("加入延迟玩家数量: %d", tracker.getJoinDelayPlayersSize());
        debug("总 NPC 数量: %d", plugin.getAllNpcs().size());
        debug("在线玩家数量: %d", plugin.getServer().getOnlinePlayers().size());
        debug("==================");
    }

    /**
     * 获取缓存统计摘要（用于命令输出）
     *
     * @return 缓存统计摘要字符串
     */
    public String getCacheStatsSummary() {
        VisibilityTracker tracker = plugin.getVisibilityTracker();
        if (tracker == null) {
            return "VisibilityTracker 未初始化";
        }

        return String.format(
                "缓存统计: 世界=%d, 玩家位置=%d, 加入延迟=%d, NPC=%d, 在线玩家=%d",
                tracker.getWorldCacheSize(),
                tracker.getPlayerLocationCacheSize(),
                tracker.getJoinDelayPlayersSize(),
                plugin.getAllNpcs().size(),
                plugin.getServer().getOnlinePlayers().size()
        );
    }
}
