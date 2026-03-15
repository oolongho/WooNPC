package com.oolonghoo.woonpc.version;

import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft 版本检测工具类
 * <p>
 * 提供版本检测和解析功能，支持 Minecraft 1.21.2 - 1.21.11 版本。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 获取当前版本
 * String version = VersionUtil.getMcVersion();
 *
 * // 检查是否支持
 * if (VersionUtil.isVersionSupported(version)) {
 *     VersionAdapter adapter = VersionAdapterFactory.getAdapter();
 * }
 *
 * // 版本比较
 * if (VersionUtil.isAtLeast(1, 21, 4)) {
 *     // 使用新版本特性
 * }
 * }</pre>
 *
 * @author oolongho
 * @since 1.0.0
 */
public final class VersionUtil {

    // 私有构造函数，防止实例化
    private VersionUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== 版本信息常量 ====================

    /**
     * 支持的最低版本
     */
    public static final String MIN_VERSION = "1.21.2";

    /**
     * 支持的最高版本
     */
    public static final String MAX_VERSION = "1.21.11";

    /**
     * 支持的版本列表
     */
    public static final List<String> SUPPORTED_VERSIONS = Arrays.asList(
            "1.21.2",
            "1.21.3",
            "1.21.4",
            "1.21.5",
            "1.21.6",
            "1.21.7",
            "1.21.8",
            "1.21.9",
            "1.21.10",
            "1.21.11"
    );

    // 版本解析缓存
    private static final String VERSION_STRING;
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    private static final int PATCH_VERSION;

    // 版本正则表达式
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    // 静态初始化块 - 解析版本信息
    static {
        String version = MIN_VERSION;
        try {
            // 优先使用 Bukkit 获取版本
            if (Bukkit.getBukkitVersion() != null) {
                Matcher matcher = VERSION_PATTERN.matcher(Bukkit.getBukkitVersion());
                if (matcher.find()) {
                    version = matcher.group(0);
                }
            }
        } catch (Exception e) {
            // 如果获取失败，使用默认值
        }

        VERSION_STRING = version;
        String[] parts = version.split("\\.");
        MAJOR_VERSION = parts.length > 0 ? Integer.parseInt(parts[0]) : 1;
        MINOR_VERSION = parts.length > 1 ? Integer.parseInt(parts[1]) : 21;
        PATCH_VERSION = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
    }

    // ==================== 基础版本信息 ====================

    /**
     * 获取当前 Minecraft 服务端版本字符串
     *
     * @return 版本字符串，例如 "1.21.11"
     */
    public static String getMcVersion() {
        return VERSION_STRING;
    }

    /**
     * 获取主版本号
     *
     * @return 主版本号（例如 1）
     */
    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }

    /**
     * 获取次版本号
     *
     * @return 次版本号（例如 21）
     */
    public static int getMinorVersion() {
        return MINOR_VERSION;
    }

    /**
     * 获取补丁版本号
     *
     * @return 补丁版本号（例如 11）
     */
    public static int getPatchVersion() {
        return PATCH_VERSION;
    }

    // ==================== 版本检测 ====================

    /**
     * 检查指定版本是否在支持范围内
     *
     * @param version 版本字符串
     * @return 如果版本支持则返回 true
     */
    public static boolean isVersionSupported(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }

        // 提取版本号
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.find()) {
            return false;
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

            // 1.21.x 版本范围检查
            if (major != 1 || minor != 21) {
                return false;
            }

            return patch >= 2 && patch <= 11;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 检查当前版本是否在支持范围内
     *
     * @return 如果当前版本支持则返回 true
     */
    public static boolean isCurrentVersionSupported() {
        return isVersionSupported(VERSION_STRING);
    }

    /**
     * 获取支持的版本列表
     *
     * @return 支持的版本列表（不可变）
     */
    public static List<String> getSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }

    // ==================== 版本比较 ====================

    /**
     * 检查当前版本是否大于等于指定版本
     *
     * @param major 主版本号
     * @param minor 次版本号
     * @return 如果当前版本 >= 指定版本则返回 true
     */
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    /**
     * 检查当前版本是否大于等于指定版本
     *
     * @param major  主版本号
     * @param minor  次版本号
     * @param patch  补丁版本号
     * @return 如果当前版本 >= 指定版本则返回 true
     */
    public static boolean isAtLeast(int major, int minor, int patch) {
        if (MAJOR_VERSION > major) return true;
        if (MAJOR_VERSION < major) return false;
        if (MINOR_VERSION > minor) return true;
        if (MINOR_VERSION < minor) return false;
        return PATCH_VERSION >= patch;
    }

    /**
     * 检查当前版本是否等于指定版本
     *
     * @param major 主版本号
     * @param minor 次版本号
     * @return 如果当前版本 == 指定版本则返回 true
     */
    public static boolean isVersion(int major, int minor) {
        return MAJOR_VERSION == major && MINOR_VERSION == minor;
    }

    /**
     * 检查当前版本是否等于指定版本
     *
     * @param major 主版本号
     * @param minor 次版本号
     * @param patch 补丁版本号
     * @return 如果当前版本 == 指定版本则返回 true
     */
    public static boolean isVersion(int major, int minor, int patch) {
        return MAJOR_VERSION == major && MINOR_VERSION == minor && PATCH_VERSION == patch;
    }

    // ==================== 便捷版本检查 ====================

    /**
     * 检查是否是 1.21.2 版本
     */
    public static boolean is1_21_2() {
        return isVersion(1, 21, 2);
    }

    /**
     * 检查是否是 1.21.3 版本
     */
    public static boolean is1_21_3() {
        return isVersion(1, 21, 3);
    }

    /**
     * 检查是否是 1.21.4 或更高版本
     */
    public static boolean is1_21_4OrLater() {
        return isAtLeast(1, 21, 4);
    }

    /**
     * 检查是否是 1.21.5 或更高版本
     */
    public static boolean is1_21_5OrLater() {
        return isAtLeast(1, 21, 5);
    }

    /**
     * 检查是否是 1.21.6 或更高版本
     */
    public static boolean is1_21_6OrLater() {
        return isAtLeast(1, 21, 6);
    }

    /**
     * 检查是否是 1.21.9 或更高版本
     */
    public static boolean is1_21_9OrLater() {
        return isAtLeast(1, 21, 9);
    }

    /**
     * 检查是否是 1.21.11 版本
     */
    public static boolean is1_21_11() {
        return isVersion(1, 21, 11);
    }

    // ==================== NMS 版本信息 ====================

    /**
     * 获取 NMS 包版本字符串
     * <p>
     * 例如：v1_21_11_R1
     * </p>
     *
     * @return NMS 包版本字符串
     */
    public static String getNmsVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            return packageName.substring(packageName.lastIndexOf('.') + 1);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取简化的 NMS 版本号
     * <p>
     * 例如：1.21.11
     * </p>
     *
     * @return 简化的 NMS 版本号
     */
    public static String getSimpleNmsVersion() {
        String nmsVersion = getNmsVersion();
        // 从 v1_21_11_R1 提取 1.21.11
        Pattern pattern = Pattern.compile("v(\\d+)_(\\d+)_(\\d+)_R(\\d+)");
        Matcher matcher = pattern.matcher(nmsVersion);
        if (matcher.find()) {
            return matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
        }
        return "";
    }

    // ==================== 服务器软件检测 ====================

    /**
     * 检查是否使用 Paper 服务端
     *
     * @return 如果是 Paper 则返回 true
     */
    public static boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 检查是否使用 Folia 服务端
     *
     * @return 如果是 Folia 则返回 true
     */
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // ==================== 版本分类 ====================

    /**
     * 版本分类枚举
     */
    public enum VersionRange {
        /**
         * 1.21.2 - 1.21.3 (早期版本)
         */
        EARLY(2, 3, "1.21.2", "1.21.3"),

        /**
         * 1.21.4 - 1.21.5 (中期版本)
         */
        MIDDLE(4, 5, "1.21.4", "1.21.5"),

        /**
         * 1.21.6 - 1.21.8 (后期版本)
         */
        LATE(6, 8, "1.21.6", "1.21.8"),

        /**
         * 1.21.9 - 1.21.11 (最新版本)
         */
        LATEST(9, 11, "1.21.9", "1.21.11");

        private final int minPatch;
        private final int maxPatch;
        private final String minVersion;
        private final String maxVersion;

        VersionRange(int minPatch, int maxPatch, String minVersion, String maxVersion) {
            this.minPatch = minPatch;
            this.maxPatch = maxPatch;
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
        }

        /**
         * 获取版本范围的最小补丁版本
         */
        public int getMinPatch() {
            return minPatch;
        }

        /**
         * 获取版本范围的最大补丁版本
         */
        public int getMaxPatch() {
            return maxPatch;
        }

        /**
         * 获取版本范围的最小版本字符串
         */
        public String getMinVersion() {
            return minVersion;
        }

        /**
         * 获取版本范围的最大版本字符串
         */
        public String getMaxVersion() {
            return maxVersion;
        }

        /**
         * 检查当前版本是否在此范围内
         */
        public boolean isCurrentVersionInRange() {
            return PATCH_VERSION >= minPatch && PATCH_VERSION <= maxPatch;
        }

        /**
         * 根据补丁版本获取对应的版本分类
         *
         * @param patch 补丁版本
         * @return 版本分类
         */
        public static VersionRange fromPatch(int patch) {
            for (VersionRange range : values()) {
                if (patch >= range.minPatch && patch <= range.maxPatch) {
                    return range;
                }
            }
            return LATEST;
        }
    }

    /**
     * 获取当前版本所属的版本范围
     *
     * @return 版本范围枚举
     */
    public static VersionRange getVersionRange() {
        return VersionRange.fromPatch(PATCH_VERSION);
    }
}
