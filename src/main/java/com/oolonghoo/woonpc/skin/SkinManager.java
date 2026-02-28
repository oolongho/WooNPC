package com.oolonghoo.woonpc.skin;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * 皮肤管理器
 * 负责从 Mojang API 获取皮肤数据并缓存
 * 
 * @author oolongho
 */
public class SkinManager {
    
    // UUID 格式正则
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    // 无连字符 UUID 格式正则
    private static final Pattern UUID_NO_DASHES_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{32}$"
    );
    
    // 反射方法
    private static java.lang.reflect.Method gameProfilePropertiesMethod;
    
    static {
        try {
            gameProfilePropertiesMethod = GameProfile.class.getMethod("getProperties");
        } catch (NoSuchMethodException e) {
            try {
                gameProfilePropertiesMethod = GameProfile.class.getMethod("properties");
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private static PropertyMap getProfileProperties(GameProfile profile) {
        if (profile == null) return null;
        try {
            if (gameProfilePropertiesMethod != null) {
                return (PropertyMap) gameProfilePropertiesMethod.invoke(profile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // Mojang API 端点
    private static final String MOJANG_UUID_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_SESSION_API = "https://sessionserver.mojang.com/session/minecraft/profile/";
    
    // 缓存过期时间: 24 小时
    private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000L;
    
    // HTTP 客户端
    private final HttpClient httpClient;
    
    // 异步执行器
    private final ExecutorService executor;
    
    // 皮肤缓存 (identifier -> SkinData)
    private final Map<String, SkinData> skinCache = new ConcurrentHashMap<>();
    
    // UUID 缓存 (username -> UUID)
    private final Map<String, UUID> uuidCache = new ConcurrentHashMap<>();
    
    // 插件实例
    private final JavaPlugin plugin;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public SkinManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(
                3,
                r -> {
                    Thread thread = new Thread(r, "WooNPC-SkinManager");
                    thread.setDaemon(true);
                    return thread;
                }
        );
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(executor)
                .build();
    }
    
    /**
     * 关闭皮肤管理器
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // ==================== 公开 API ====================
    
    /**
     * 通过标识符获取皮肤 (同步)
     * 支持玩家名、UUID
     * 
     * @param identifier 标识符
     * @return 皮肤数据，如果获取失败返回 null
     */
    public SkinData getSkin(String identifier) {
        return getSkin(identifier, SkinData.SkinVariant.AUTO);
    }
    
    /**
     * 通过标识符获取皮肤 (同步)
     * 
     * @param identifier 标识符
     * @param variant    皮肤变体
     * @return 皮肤数据
     */
    public SkinData getSkin(String identifier, SkinData.SkinVariant variant) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        
        identifier = identifier.trim();
        
        // 检查缓存
        SkinData cached = getCachedSkin(identifier);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }
        
        // 判断标识符类型
        if (isUUID(identifier)) {
            return getSkinByUUID(parseUUID(identifier), variant).join();
        } else {
            return getSkinByName(identifier, variant).join();
        }
    }
    
    /**
     * 通过标识符获取皮肤 (异步)
     * 
     * @param identifier 标识符
     * @return 皮肤数据 CompletableFuture
     */
    public CompletableFuture<SkinData> getSkinAsync(String identifier) {
        return getSkinAsync(identifier, SkinData.SkinVariant.AUTO);
    }
    
    /**
     * 通过标识符获取皮肤 (异步)
     * 
     * @param identifier 标识符
     * @param variant    皮肤变体
     * @return 皮肤数据 CompletableFuture
     */
    public CompletableFuture<SkinData> getSkinAsync(String identifier, SkinData.SkinVariant variant) {
        return CompletableFuture.supplyAsync(() -> getSkin(identifier, variant), executor);
    }
    
    /**
     * 通过玩家名获取皮肤 (异步)
     * 
     * @param name   玩家名
     * @param variant 皮肤变体
     * @return 皮肤数据 CompletableFuture
     */
    public CompletableFuture<SkinData> getSkinByName(String name, SkinData.SkinVariant variant) {
        if (name == null || name.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        
        final String playerName = name.trim();
        
        SkinData cached = getCachedSkin(playerName);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // 先检查在线玩家
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            SkinData skinData = getPlayerSkin(onlinePlayer);
            if (skinData != null) {
                cacheSkin(playerName, skinData);
                return CompletableFuture.completedFuture(skinData);
            }
        }
        
        // 检查 SkinsRestorer 插件
        SkinData skinsRestorerSkin = getSkinFromSkinsRestorer(playerName);
        if (skinsRestorerSkin != null) {
            cacheSkin(playerName, skinsRestorerSkin);
            return CompletableFuture.completedFuture(skinsRestorerSkin);
        }
        
        return getUUIDFromName(playerName).thenCompose(uuid -> {
            if (uuid == null) {
                plugin.getLogger().warning("无法获取玩家 " + playerName + " 的 UUID");
                return CompletableFuture.completedFuture(null);
            }
            
            uuidCache.put(playerName.toLowerCase(), uuid);
            
            return getSkinByUUID(uuid, variant).thenApply(skin -> {
                if (skin != null) {
                    SkinData namedSkin = new SkinData(playerName, skin.getVariant(), 
                            skin.getTextureValue(), skin.getTextureSignature());
                    cacheSkin(playerName, namedSkin);
                }
                return skin;
            });
        });
    }
    
    /**
     * 从 SkinsRestorer 插件获取皮肤
     * 
     * @param playerName 玩家名
     * @return 皮肤数据，如果获取失败返回 null
     */
    private SkinData getSkinFromSkinsRestorer(String playerName) {
        try {
            // 检查 SkinsRestorer 是否安装
            org.bukkit.plugin.Plugin skinsRestorer = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
            if (skinsRestorer == null || !skinsRestorer.isEnabled()) {
                return null;
            }
            
            plugin.getLogger().info("检测到 SkinsRestorer 插件，尝试获取玩家 " + playerName + " 的皮肤");
            
            // 尝试使用反射调用 SkinsRestorer API
            // SkinsRestorerAPI.getSkinData(String playerName)
            Class<?> apiClass = Class.forName("net.skinsrestorer.api.SkinsRestorerAPI");
            Class<?> skinDataClass = Class.forName("net.skinsrestorer.api.property.SkinData");
            Class<?> propertyClass = Class.forName("net.skinsrestorer.api.property.SkinProperty");
            
            // 获取 API 实例
            java.lang.reflect.Method getApiMethod = apiClass.getMethod("getApi");
            Object apiInstance = getApiMethod.invoke(null);
            
            // 获取皮肤数据
            java.lang.reflect.Method getSkinMethod = apiClass.getMethod("getSkinData", String.class);
            Object skinData = getSkinMethod.invoke(apiInstance, playerName);
            
            if (skinData == null) {
                plugin.getLogger().warning("SkinsRestorer 没有玩家 " + playerName + " 的皮肤数据");
                return null;
            }
            
            // 获取皮肤属性
            java.lang.reflect.Method getPropertyMethod = skinDataClass.getMethod("getProperty");
            Object property = getPropertyMethod.invoke(skinData);
            
            if (property == null) {
                return null;
            }
            
            // 获取 value 和 signature
            java.lang.reflect.Method getValueMethod = propertyClass.getMethod("getValue");
            java.lang.reflect.Method getSignatureMethod = propertyClass.getMethod("getSignature");
            
            String value = (String) getValueMethod.invoke(property);
            String signature = (String) getSignatureMethod.invoke(property);
            
            if (value != null && !value.isEmpty()) {
                plugin.getLogger().info("成功从 SkinsRestorer 获取玩家 " + playerName + " 的皮肤");
                return new SkinData(playerName, SkinData.SkinVariant.AUTO, value, signature);
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("SkinsRestorer API 类未找到，可能版本不兼容");
        } catch (NoSuchMethodException e) {
            plugin.getLogger().warning("SkinsRestorer API 方法未找到，可能版本不兼容: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().warning("从 SkinsRestorer 获取皮肤失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 通过 UUID 获取皮肤 (异步)
     * 
     * @param uuid    玩家 UUID
     * @param variant 皮肤变体
     * @return 皮肤数据 CompletableFuture
     */
    public CompletableFuture<SkinData> getSkinByUUID(UUID uuid, SkinData.SkinVariant variant) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        String uuidString = uuid.toString();
        
        // 检查缓存
        SkinData cached = getCachedSkin(uuidString);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // 从 Mojang 获取
        return fetchSkinFromMojang(uuid, variant);
    }
    
    /**
     * 获取玩家皮肤 (用于镜像皮肤)
     * 直接从在线玩家的 GameProfile 获取皮肤数据
     * 
     * @param player 玩家
     * @return 皮肤数据
     */
    public SkinData getPlayerSkin(Player player) {
        if (player == null) {
            return null;
        }
        
        try {
            // 获取 NMS 玩家对象
            net.minecraft.server.level.ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
            GameProfile profile = serverPlayer.getGameProfile();
            
            // 使用反射获取纹理属性
            PropertyMap properties = getProfileProperties(profile);
            if (properties == null) {
                return null;
            }
            
            Collection<Property> textures = properties.get("textures");
            if (textures == null || textures.isEmpty()) {
                return null;
            }
            
            Property property = textures.iterator().next();
            String value = property.value();
            String signature = property.signature();
            
            if (value == null || value.isEmpty()) {
                return null;
            }
            
            return new SkinData(player.getName(), SkinData.SkinVariant.AUTO, value, signature);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "获取玩家 " + player.getName() + " 的皮肤失败", e);
            return null;
        }
    }
    
    /**
     * 获取玩家皮肤 (异步)
     * 
     * @param player 玩家
     * @return 皮肤数据 CompletableFuture
     */
    public CompletableFuture<SkinData> getPlayerSkinAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> getPlayerSkin(player), executor);
    }
    
    // ==================== 缓存操作 ====================
    
    /**
     * 缓存皮肤数据
     * 
     * @param identifier 标识符
     * @param skinData   皮肤数据
     */
    public void cacheSkin(String identifier, SkinData skinData) {
        if (identifier == null || skinData == null) {
            return;
        }
        skinCache.put(identifier.toLowerCase(), skinData);
    }
    
    /**
     * 获取缓存的皮肤数据
     * 
     * @param identifier 标识符
     * @return 皮肤数据，如果不存在或已过期返回 null
     */
    public SkinData getCachedSkin(String identifier) {
        if (identifier == null) {
            return null;
        }
        
        SkinData skinData = skinCache.get(identifier.toLowerCase());
        if (skinData == null) {
            return null;
        }
        
        // 检查是否过期
        if (skinData.isExpired()) {
            skinCache.remove(identifier.toLowerCase());
            return null;
        }
        
        return skinData;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        skinCache.clear();
        uuidCache.clear();
    }
    
    /**
     * 清除过期缓存
     */
    public void cleanExpiredCache() {
        skinCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * 获取缓存大小
     * 
     * @return 缓存中的皮肤数量
     */
    public int getCacheSize() {
        return skinCache.size();
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 从 Mojang API 获取皮肤
     * 
     * @param uuid    玩家 UUID
     * @param variant 皮肤变体
     * @return 皮肤数据 CompletableFuture
     */
    private CompletableFuture<SkinData> fetchSkinFromMojang(UUID uuid, SkinData.SkinVariant variant) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String uuidString = uuid.toString().replace("-", "");
                
                // 构建请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(MOJANG_SESSION_API + uuidString + "?unsigned=false"))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/json")
                        .build();
                
                // 发送请求
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                // 检查响应
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    plugin.getLogger().warning("从 Mojang API 获取皮肤失败 (UUID: " + uuid + ", 状态码: " + response.statusCode() + ")");
                    return null;
                }
                
                // 解析响应
                return parseSkinResponse(uuid.toString(), variant, response.body());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "从 Mojang API 获取皮肤失败 (UUID: " + uuid + ")", e);
                return null;
            }
        }, executor);
    }
    
    /**
     * 从玩家名获取 UUID
     * 
     * @param name 玩家名
     * @return UUID CompletableFuture
     */
    private CompletableFuture<UUID> getUUIDFromName(String name) {
        // 先检查缓存
        UUID cachedUuid = uuidCache.get(name.toLowerCase());
        if (cachedUuid != null) {
            return CompletableFuture.completedFuture(cachedUuid);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(MOJANG_UUID_API + name))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/json")
                        .build();
                
                // 发送请求
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                // 检查响应
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    plugin.getLogger().warning("从 Mojang API 获取 UUID 失败 (玩家名: " + name + ", 状态码: " + response.statusCode() + ")");
                    return null;
                }
                
                // 解析响应
                return parseUuidResponse(response.body());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "从 Mojang API 获取 UUID 失败 (玩家名: " + name + ")", e);
                return null;
            }
        }, executor);
    }
    
    /**
     * 解析 UUID API 响应
     * 
     * @param json JSON 响应
     * @return UUID
     */
    private UUID parseUuidResponse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            // 简单 JSON 解析 (不依赖外部库)
            // 格式: {"id":"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx","name":"PlayerName"}
            String idKey = "\"id\":\"";
            int idStart = json.indexOf(idKey);
            if (idStart == -1) {
                return null;
            }
            
            idStart += idKey.length();
            int idEnd = json.indexOf("\"", idStart);
            if (idEnd == -1) {
                return null;
            }
            
            String idString = json.substring(idStart, idEnd);
            return parseUUID(idString);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "解析 UUID 响应失败: " + json, e);
            return null;
        }
    }
    
    /**
     * 解析皮肤 API 响应
     * 
     * @param identifier 标识符
     * @param variant    皮肤变体
     * @param json       JSON 响应
     * @return 皮肤数据
     */
    private SkinData parseSkinResponse(String identifier, SkinData.SkinVariant variant, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            // 简单 JSON 解析
            // 格式: {"id":"...","name":"...","properties":[{"name":"textures","value":"...","signature":"..."}]}
            
            String valueKey = "\"value\":\"";
            String signatureKey = "\"signature\":\"";
            
            // 查找 value
            int valueStart = json.indexOf(valueKey);
            if (valueStart == -1) {
                return null;
            }
            valueStart += valueKey.length();
            int valueEnd = json.indexOf("\"", valueStart);
            if (valueEnd == -1) {
                return null;
            }
            String value = json.substring(valueStart, valueEnd);
            
            // 查找 signature
            int signatureStart = json.indexOf(signatureKey, valueEnd);
            if (signatureStart == -1) {
                return null;
            }
            signatureStart += signatureKey.length();
            int signatureEnd = json.indexOf("\"", signatureStart);
            if (signatureEnd == -1) {
                return null;
            }
            String signature = json.substring(signatureStart, signatureEnd);
            
            // 创建皮肤数据
            SkinData skinData = new SkinData(identifier, variant, value, signature);
            skinData.setCacheDuration(CACHE_DURATION_MS);
            
            // 缓存
            cacheSkin(identifier, skinData);
            
            return skinData;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "解析皮肤响应失败", e);
            return null;
        }
    }
    
    /**
     * 检查字符串是否为 UUID
     * 
     * @param str 字符串
     * @return 是否为 UUID
     */
    public static boolean isUUID(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return UUID_PATTERN.matcher(str).matches() || UUID_NO_DASHES_PATTERN.matcher(str).matches();
    }
    
    /**
     * 解析 UUID 字符串
     * 支持带连字符和不带连字符的格式
     * 
     * @param str UUID 字符串
     * @return UUID
     */
    public static UUID parseUUID(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        str = str.trim();
        
        // 如果没有连字符，添加连字符
        if (UUID_NO_DASHES_PATTERN.matcher(str).matches()) {
            str = str.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
            );
        }
        
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * 获取缓存的 UUID
     * 
     * @param name 玩家名
     * @return UUID，如果不存在返回 null
     */
    public UUID getCachedUUID(String name) {
        return uuidCache.get(name.toLowerCase());
    }
}
