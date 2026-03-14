package com.oolonghoo.woonpc.util;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.config.ConfigLoader;

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
}
