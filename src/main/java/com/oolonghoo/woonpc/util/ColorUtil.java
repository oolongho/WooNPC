package com.oolonghoo.woonpc.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * 颜色代码处理工具类
 * 支持 §、& 和 MiniMessage 格式
 * 
 * @author oolongho
 */
public final class ColorUtil {
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
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
     * 
     * @param text 文本
     * @return 是否包含 MiniMessage 格式
     */
    private static boolean containsMiniMessage(String text) {
        return text.contains("<") && text.contains(">");
    }
}
