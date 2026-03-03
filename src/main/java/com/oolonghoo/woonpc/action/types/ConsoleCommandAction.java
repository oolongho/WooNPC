package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.action.NpcAction;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 控制台命令动作
 * 以控制台身份执行命令
 * 
 * @author oolongho
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
        
        // 支持颜色代码转换 (用于命令中的消息参数)
        String command = LegacyComponentSerializer.legacyAmpersand().serialize(
            LegacyComponentSerializer.legacyAmpersand().deserialize(value)
        );
        
        // 支持占位符替换
        command = command.replace("{player}", player.getName());
        command = command.replace("{uuid}", player.getUniqueId().toString());
        command = command.replace("{world}", player.getWorld().getName());
        command = command.replace("{x}", String.valueOf(player.getLocation().getBlockX()));
        command = command.replace("{y}", String.valueOf(player.getLocation().getBlockY()));
        command = command.replace("{z}", String.valueOf(player.getLocation().getBlockZ()));
        
        // 移除开头的斜杠 (如果有)
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        
        // 在主线程执行命令
        String finalCommand = command;
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        } else {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("WooNPC"), () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
            });
        }
    }
}
