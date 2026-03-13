package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.action.NpcAction;
import com.oolonghoo.woonpc.util.ColorUtil;
import com.oolonghoo.woonpc.util.CommandSafety;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 控制台命令动作
 * 以控制台身份执行命令（带安全检查）
 * o
 */
public class ConsoleCommandAction extends NpcAction {
    
    public ConsoleCommandAction() {
        super("console_command", true);
    }
    
    @Override
    public void execute(@NotNull Player player, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        
        // 将颜色代码转换（用于命令中的消息参数）
        String command = ColorUtil.translate(value);
        
        // 支持占位符替换
        command = command.replace("{player}", player.getName());
        command = command.replace("{uuid}", player.getUniqueId().toString());
        command = command.replace("{world}", player.getWorld().getName());
        command = command.replace("{x}", String.valueOf(player.getLocation().getBlockX()));
        command = command.replace("{y}", String.valueOf(player.getLocation().getBlockY()));
        command = command.replace("{z}", String.valueOf(player.getLocation().getBlockZ()));
        
        // 安全检查：清理和验证命令
        String sanitizedCommand = CommandSafety.sanitizeCommand(command);
        if (sanitizedCommand == null) {
            Bukkit.getLogger().warning("[WooNPC] 玩家 " + player.getName() + " 尝试执行被禁止的命令：" + command);
            player.sendMessage("§c[系统] 该命令被禁止使用");
            return;
        }
        
        // 在主线程执行命令
        String finalCommand = sanitizedCommand;
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        } else {
            Bukkit.getScheduler().runTask(WooNPC.getInstance(), () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
            });
        }
    }
}
