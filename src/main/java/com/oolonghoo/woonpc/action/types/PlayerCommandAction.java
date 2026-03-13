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
 * 玩家命令动作
 * 以玩家身份执行命令（带安全检查）
 * o
 */
public class PlayerCommandAction extends NpcAction {
    
    public PlayerCommandAction() {
        super("player_command", true);
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
        
        // 安全检查：清理和验证命令
        String sanitizedCommand = CommandSafety.sanitizeCommand(command);
        if (sanitizedCommand == null) {
            player.sendMessage("§c[系统] 该命令被禁止使用");
            return;
        }
        
        // 在主线程执行命令
        String finalCommand = sanitizedCommand;
        if (Bukkit.isPrimaryThread()) {
            player.performCommand(finalCommand);
        } else {
            Bukkit.getScheduler().runTask(WooNPC.getInstance(), () -> {
                player.performCommand(finalCommand);
            });
        }
    }
}
