package com.oolonghoo.woonpc.util;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionUtil {
    
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    private static final int PATCH_VERSION;
    private static final String VERSION_STRING;
    
    static {
        String version = "1.21.11";
        try {
            String bukkitVersion = Bukkit.getBukkitVersion();
            Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
            Matcher matcher = pattern.matcher(bukkitVersion);
            if (matcher.find()) {
                version = matcher.group(0);
            }
        } catch (Exception e) {
        }
        
        VERSION_STRING = version;
        String[] parts = version.split("\\.");
        MAJOR_VERSION = parts.length > 0 ? Integer.parseInt(parts[0]) : 1;
        MINOR_VERSION = parts.length > 1 ? Integer.parseInt(parts[1]) : 21;
        PATCH_VERSION = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
    }
    
    private VersionUtil() {}
    
    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }
    
    public static int getMinorVersion() {
        return MINOR_VERSION;
    }
    
    public static int getPatchVersion() {
        return PATCH_VERSION;
    }
    
    public static String getVersionString() {
        return VERSION_STRING;
    }
    
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }
    
    public static boolean isAtLeast(int major, int minor, int patch) {
        if (MAJOR_VERSION > major) return true;
        if (MAJOR_VERSION < major) return false;
        if (MINOR_VERSION > minor) return true;
        if (MINOR_VERSION < minor) return false;
        return PATCH_VERSION >= patch;
    }
    
    public static boolean isVersion(int major, int minor) {
        return MAJOR_VERSION == major && MINOR_VERSION == minor;
    }
    
    public static boolean isVersion(int major, int minor, int patch) {
        return MAJOR_VERSION == major && MINOR_VERSION == minor && PATCH_VERSION == patch;
    }
    
    public static boolean is1_21_11OrLater() {
        return isAtLeast(1, 21, 11);
    }
    
    public static boolean is1_21_4OrLater() {
        return isAtLeast(1, 21, 4);
    }
    
    public static boolean is1_21_3OrEarlier() {
        return !isAtLeast(1, 21, 4);
    }
    
    public static String getNmsVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            return packageName.substring(packageName.lastIndexOf('.') + 1);
        } catch (Exception e) {
            return "";
        }
    }
    
    public static boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
