package com.oolonghoo.woonpc.util;

import org.bukkit.Bukkit;

/**
 * 命令安全工具类（轻量级）
 * 只进行基础的命令注入防护
 * o
 */
public final class CommandSafety {
    
    private CommandSafety() {
    }
    
    /**
     * 清理和验证命令
     * 只移除危险字符，不进行白名单限制
     * 
     * @param command 原始命令
     * @return 清理后的命令，如果危险则返回 null
     */
    public static String sanitizeCommand(String command) {
        if (command == null || command.isEmpty()) {
            return null;
        }
        
        // 移除开头的斜杠
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        
        // 移除前后空格
        command = command.trim();
        
        // 检查是否包含危险字符（命令注入）
        if (command.contains(";") || command.contains("|") || command.contains("&")) {
            Bukkit.getLogger().warning("[WooNPC] 检测到可能的命令注入尝试：" + command);
            return null;
        }
        
        // 检查是否包含换行符（防止多命令执行）
        if (command.contains("\n") || command.contains("\r")) {
            Bukkit.getLogger().warning("[WooNPC] 检测到多命令执行尝试：" + command);
            return null;
        }
        
        return command;
    }
}
