package com.oolonghoo.woonpc.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 颜色代码处理工具类
 * 支持 §、& 和 MiniMessage 格式
 * 
 * @author oolongho
 */
public final class ColorUtil {
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    private static final Set<String> MINIMESSAGE_TAGS = Set.of(
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
        "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
        "obfuscated", "bold", "strikethrough", "underlined", "italic", "reset",
        "br", "newline", "lang", "key", "translatable", "translate", "insertion",
        "click", "hover", "suggest_command", "run_command", "open_url", "copy_to_clipboard",
        "gradient", "rainbow", "font", "color", "hex"
    );
    
    private static final Pattern MINIMESSAGE_PATTERN = Pattern.compile(
        "<(?:(/?)(?:" +
        "black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|" +
        "dark_gray|blue|green|aqua|red|light_purple|yellow|white|" +
        "obfuscated|bold|strikethrough|underlined|italic|reset|" +
        "br|newline|" +
        "lang|key|translatable|translate|insertion|" +
        "click|hover|suggest_command|run_command|open_url|copy_to_clipboard|" +
        "gradient|rainbow|font|" +
        "color|#[0-9a-fA-F]{6}" +
        ")[^>]*|/?[a-z_]+:[^>]*)>",
        Pattern.CASE_INSENSITIVE
    );
    
    private ColorUtil() {
    }
    
    /**
     * 转换颜色代码
     * 支持 §、& 和 MiniMessage 格式
     * 
     * @param text 原始文本
     * @return 颜色化后的文本
     */
    public static String translate(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        if (containsMiniMessage(text)) {
            try {
                Component component = MINI_MESSAGE.deserialize(text);
                return LegacyComponentSerializer.legacySection().serialize(component);
            } catch (Exception ignored) {
            }
        }
        
        return LegacyComponentSerializer.legacySection().serialize(
            LegacyComponentSerializer.legacyAmpersand().deserialize(text)
        );
    }
    
    /**
     * 将文本转换为 Adventure Component
     * 支持 §、& 和 MiniMessage 格式
     * 
     * @param text 原始文本
     * @return Adventure Component
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        if (containsMiniMessage(text)) {
            try {
                return MINI_MESSAGE.deserialize(text);
            } catch (Exception ignored) {
            }
        }
        
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
    
    /**
     * 移除所有颜色代码
     * 
     * @param text 原始文本
     * @return 移除颜色后的文本
     */
    public static String stripColor(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        String result = text;
        
        if (containsMiniMessage(result)) {
            try {
                Component component = MINI_MESSAGE.deserialize(result);
                result = MINI_MESSAGE.serialize(component);
            } catch (Exception ignored) {
            }
        }
        
        result = LegacyComponentSerializer.legacySection().serialize(
            LegacyComponentSerializer.legacySection().deserialize(result)
        );
        
        return result.replaceAll("[§&][0-9a-fk-orA-FK-OR]", "")
                     .replaceAll("<[^>]+>", "");
    }
    
    /**
     * 检查文本是否包含 MiniMessage 格式
     * 使用正则表达式匹配有效的 MiniMessage 标签
     * 
     * @param text 文本
     * @return 是否包含 MiniMessage 格式
     */
    private static boolean containsMiniMessage(String text) {
        return MINIMESSAGE_PATTERN.matcher(text).find();
    }
}
