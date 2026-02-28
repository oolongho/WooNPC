package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.action.NpcAction;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 消息动作
 * 向玩家发送消息
 * 
 * @author oolongho
 */
public class MessageAction extends NpcAction {
    
    public MessageAction() {
        super("message", true);
    }
    
    @Override
    public void execute(@NotNull Player player, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        
        // 支持颜色代码转换
        String message = ChatColor.translateAlternateColorCodes('&', value);
        
        // 支持占位符替换
        message = message.replace("{player}", player.getName());
        
        player.sendMessage(message);
    }
}
