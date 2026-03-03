package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.action.NpcAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

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
            String soundName = parts[0].toLowerCase(Locale.ROOT);
            
            float volume = 1.0f;
            float pitch = 1.0f;
            
            if (parts.length >= 2) {
                volume = Float.parseFloat(parts[1]);
            }
            
            if (parts.length >= 3) {
                pitch = Float.parseFloat(parts[2]);
            }
            
            // 尝试通过 Registry 获取声音 (1.20.6+ 推荐方式)
            Sound sound = Registry.SOUNDS.get(
                org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase(Locale.ROOT))
            );
            
            if (sound != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            } else {
                // 如果不是有效的注册声音，尝试使用自定义声音名称
                player.playSound(player.getLocation(), soundName, volume, pitch);
            }
            
        } catch (Exception e) {
            player.sendMessage(Component.text("无法播放声音: " + value, NamedTextColor.RED));
        }
    }
}
