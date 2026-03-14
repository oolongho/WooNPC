package com.oolonghoo.woonpc.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 颜色代码处理工具类
 * 支持 §、& 和 MiniMessage 格式
 * 
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
    
    private static final Pattern MINIMESSAGE_PATTERN = buildMiniMessagePattern();
    
    private ColorUtil() {
    }
    
    private static Pattern buildMiniMessagePattern() {
        String tagsRegex = String.join("|", MINIMESSAGE_TAGS);
        String regex = "<(?:(/?)(?:" + tagsRegex + "|#[0-9a-fA-F]{6})[^>]*|/?[a-z_]+:[^>]*)>";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
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
            } catch (Exception e) {
                // MiniMessage 解析失败，回退到传统格式
                Bukkit.getLogger().fine(() -> "[WooNPC] MiniMessage 解析失败：" + e.getMessage());
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
            } catch (Exception e) {
                // MiniMessage 解析失败，回退到传统格式
                Bukkit.getLogger().fine(() -> "[WooNPC] MiniMessage 解析失败：" + e.getMessage());
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
        
        // 先移除 MiniMessage 标签
        result = result.replaceAll("<[^>]+>", "");
        
        // 移除 § 和 & 颜色代码
        result = result.replaceAll("[§&][0-9a-fk-orA-FK-OR]", "");
        
        // 移除 hex 颜色代码格式 §x§R§R§G§G§B§B
        result = result.replaceAll("§x(?:§[0-9a-fA-F]){6}", "");
        result = result.replaceAll("&x(?:&[0-9a-fA-F]){6}", "");
        
        return result.trim();
    }
    
    /**
     * 生成用于 ID 的名称（剥离颜色代码，转小写）
     * 
     * @param name 原始名称
     * @return 可用作 ID 的名称
     */
    public static String toIdName(String name) {
        String stripped = stripColor(name);
        return stripped.toLowerCase().replaceAll("[^a-z0-9_]", "_");
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
