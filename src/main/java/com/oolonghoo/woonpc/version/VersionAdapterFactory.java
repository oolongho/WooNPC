package com.oolonghoo.woonpc.version;

import com.oolonghoo.woonpc.version.impl.VersionAdapter_1_21_11;
import com.oolonghoo.woonpc.version.impl.VersionAdapter_1_21_3;
import com.oolonghoo.woonpc.version.impl.VersionAdapter_1_21_4;
import com.oolonghoo.woonpc.version.impl.VersionAdapter_1_21_5;
import com.oolonghoo.woonpc.version.impl.VersionAdapter_1_21_6;
import com.oolonghoo.woonpc.version.impl.VersionAdapter_1_21_9;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 版本适配器工厂
 * <p>
 * 负责根据当前服务端版本创建对应的版本适配器实例。
 * 使用工厂模式和单例模式确保每个版本只有一个适配器实例。
 * </p>
 *
 * <h3>设计模式</h3>
 * <ul>
 *   <li>工厂模式：根据版本号创建对应的适配器</li>
 *   <li>单例模式：每个版本只创建一个适配器实例</li>
 *   <li>策略模式：不同版本使用不同的实现策略</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 获取当前版本的适配器
 * VersionAdapter adapter = VersionAdapterFactory.getAdapter();
 *
 * // 使用适配器
 * adapter.sendPacket(player, packet);
 *
 * // 检查是否支持当前版本
 * if (VersionAdapterFactory.isSupported()) {
 *     // 安全使用适配器
 * }
 * }</pre>
 *
 * @author oolongho
 * @since 1.0.0
 */
public final class VersionAdapterFactory {

    // 私有构造函数，防止实例化
    private VersionAdapterFactory() {
        throw new UnsupportedOperationException("Factory class cannot be instantiated");
    }

    // ==================== 适配器缓存 ====================

    /**
     * 适配器实例缓存
     * Key: 版本字符串, Value: 适配器实例
     */
    private static final Map<String, VersionAdapter> ADAPTER_CACHE = new ConcurrentHashMap<>();

    /**
     * 当前版本的适配器实例（延迟初始化）
     */
    private static volatile VersionAdapter currentAdapter;

    /**
     * 是否已初始化
     */
    private static volatile boolean initialized = false;

    // ==================== 版本映射 ====================

    /**
     * 版本到适配器供应商的映射
     * 使用 Supplier 延迟创建适配器实例
     */
    private static final Map<String, Supplier<VersionAdapter>> VERSION_SUPPLIERS = Map.of(
            "1.21.2", VersionAdapter_1_21_3::new,  // 1.21.2 和 1.21.3 使用相同的适配器
            "1.21.3", VersionAdapter_1_21_3::new,
            "1.21.4", VersionAdapter_1_21_4::new,
            "1.21.5", VersionAdapter_1_21_5::new,
            "1.21.6", VersionAdapter_1_21_6::new,  // 1.21.6, 1.21.7, 1.21.8 使用相同的适配器
            "1.21.7", VersionAdapter_1_21_6::new,
            "1.21.8", VersionAdapter_1_21_6::new,
            "1.21.9", VersionAdapter_1_21_9::new,  // 1.21.9 和 1.21.10 使用相同的适配器
            "1.21.10", VersionAdapter_1_21_9::new,
            "1.21.11", VersionAdapter_1_21_11::new
    );

    // ==================== 公共 API ====================

    /**
     * 获取当前服务端版本的适配器
     * <p>
     * 如果版本不支持，将抛出 {@link UnsupportedVersionException}
     * </p>
     *
     * @return 当前版本的适配器实例
     * @throws UnsupportedVersionException 如果当前版本不支持
     */
    @Nonnull
    public static VersionAdapter getAdapter() {
        if (currentAdapter != null) {
            return currentAdapter;
        }

        synchronized (VersionAdapterFactory.class) {
            if (currentAdapter != null) {
                return currentAdapter;
            }

            String version = VersionUtil.getMcVersion();
            currentAdapter = getAdapter(version);

            if (currentAdapter == null) {
                throw new UnsupportedVersionException(version);
            }

            initialized = true;
            return currentAdapter;
        }
    }

    /**
     * 获取指定版本的适配器
     *
     * @param version Minecraft 版本字符串（例如 "1.21.11"）
     * @return 对应版本的适配器实例，如果版本不支持则返回 null
     */
    @Nullable
    public static VersionAdapter getAdapter(@Nonnull String version) {
        // 检查缓存
        VersionAdapter cached = ADAPTER_CACHE.get(version);
        if (cached != null) {
            return cached;
        }

        // 获取供应商
        Supplier<VersionAdapter> supplier = VERSION_SUPPLIERS.get(version);
        if (supplier == null) {
            return null;
        }

        // 创建新实例并缓存
        VersionAdapter adapter = supplier.get();
        ADAPTER_CACHE.put(version, adapter);

        return adapter;
    }

    /**
     * 检查当前版本是否支持
     *
     * @return 如果当前版本支持则返回 true
     */
    public static boolean isSupported() {
        return VERSION_SUPPLIERS.containsKey(VersionUtil.getMcVersion());
    }

    /**
     * 检查指定版本是否支持
     *
     * @param version 版本字符串
     * @return 如果版本支持则返回 true
     */
    public static boolean isVersionSupported(@Nonnull String version) {
        return VERSION_SUPPLIERS.containsKey(version);
    }

    /**
     * 检查工厂是否已初始化
     *
     * @return 如果已初始化则返回 true
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 重置工厂状态（仅用于测试）
     * <p>
     * 此方法会清除缓存的适配器实例，仅应在测试环境中使用。
     * </p>
     */
    public static void reset() {
        synchronized (VersionAdapterFactory.class) {
            ADAPTER_CACHE.clear();
            currentAdapter = null;
            initialized = false;
        }
    }

    // ==================== 异常类 ====================

    /**
     * 不支持的版本异常
     * <p>
     * 当尝试在不支持的 Minecraft 版本上使用插件时抛出
     * </p>
     */
    public static class UnsupportedVersionException extends RuntimeException {

        private final String version;

        /**
         * 创建不支持的版本异常
         *
         * @param version 不支持的版本
         */
        public UnsupportedVersionException(String version) {
            super(String.format(
                    "Unsupported Minecraft version: %s. Supported versions: %s",
                    version,
                    String.join(", ", VersionUtil.getSupportedVersions())
            ));
            this.version = version;
        }

        /**
         * 获取不支持的版本
         *
         * @return 版本字符串
         */
        public String getVersion() {
            return version;
        }
    }

    // ==================== 调试信息 ====================

    /**
     * 获取工厂状态信息（用于调试）
     *
     * @return 状态信息字符串
     */
    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("VersionAdapterFactory Debug Info:\n");
        sb.append("  Current Version: ").append(VersionUtil.getMcVersion()).append("\n");
        sb.append("  Is Supported: ").append(isSupported()).append("\n");
        sb.append("  Is Initialized: ").append(initialized).append("\n");
        sb.append("  Cached Adapters: ").append(ADAPTER_CACHE.size()).append("\n");
        sb.append("  Supported Versions: ").append(String.join(", ", VersionUtil.getSupportedVersions())).append("\n");
        sb.append("  Server Software: ").append(VersionUtil.isPaper() ? "Paper" : "Spigot");
        if (VersionUtil.isFolia()) {
            sb.append(" (Folia)");
        }
        return sb.toString();
    }
}
