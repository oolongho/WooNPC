package com.oolonghoo.woonpc.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlaceholderUtil {
    
    private static boolean placeholderApiEnabled = false;
    private static boolean checked = false;
    
    private PlaceholderUtil() {
    }
    
    private static void checkPlaceholderApi() {
        if (checked) {
            return;
        }
        checked = true;
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderApiEnabled = true;
        }
    }
    
    public static String setPlaceholder(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        checkPlaceholderApi();
        
        if (!placeholderApiEnabled) {
            return text;
        }
        
        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception e) {
            return text;
        }
    }
    
    public static String setPlaceholders(Player player, String text) {
        return setPlaceholder(player, text);
    }
    
    public static boolean isPlaceholderApiEnabled() {
        checkPlaceholderApi();
        return placeholderApiEnabled;
    }
    
    public static boolean containsPlaceholders(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.contains("%") && text.indexOf('%') != text.lastIndexOf('%');
    }
}
