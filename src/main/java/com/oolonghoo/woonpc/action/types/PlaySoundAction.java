package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.action.NpcAction;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 播放声音动作
 * 向玩家播放音效
 * 
 * @author oolongho
 */
public class PlaySoundAction extends NpcAction {
    
    public PlaySoundAction() {
        super("play_sound", true);
    }
    
    @Override
    public void execute(@NotNull Player player, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        
        try {
            // 格式: sound_name [volume] [pitch]
            String[] parts = value.split(" ");
            String soundName = parts[0].toUpperCase();
            
            float volume = 1.0f;
            float pitch = 1.0f;
            
            if (parts.length >= 2) {
                volume = Float.parseFloat(parts[1]);
            }
            
            if (parts.length >= 3) {
                pitch = Float.parseFloat(parts[2]);
            }
            
            // 尝试获取声音
            Sound sound;
            try {
                sound = Sound.valueOf(soundName);
            } catch (IllegalArgumentException e) {
                // 如果不是有效的枚举值，尝试使用自定义声音名称
                player.playSound(player.getLocation(), soundName.toLowerCase(), volume, pitch);
                return;
            }
            
            player.playSound(player.getLocation(), sound, volume, pitch);
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "无法播放声音: " + value);
        }
    }
}
