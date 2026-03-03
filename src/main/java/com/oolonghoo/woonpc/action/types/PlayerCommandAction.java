package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.action.NpcAction;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家命令动作
 * 以玩家身份执行命令
 * 
 * @author oolongho
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
        
        // 支持颜色代码转换 (用于命令中的消息参数)
        String command = LegacyComponentSerializer.legacyAmpersand().serialize(
            LegacyComponentSerializer.legacyAmpersand().deserialize(value)
        );
        
        // 支持占位符替换
        command = command.replace("{player}", player.getName());
        
        // 移除开头的斜杠 (如果有)
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        
        // 在主线程执行命令
        String finalCommand = command;
        if (Bukkit.isPrimaryThread()) {
            player.performCommand(finalCommand);
        } else {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("WooNPC"), () -> {
                player.performCommand(finalCommand);
            });
        }
    }
}
