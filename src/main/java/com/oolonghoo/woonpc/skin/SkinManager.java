package com.oolonghoo.woonpc.skin;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.oolonghoo.woonpc.skin.cache.SkinCacheData;
import com.oolonghoo.woonpc.skin.cache.SkinCacheFile;
import com.oolonghoo.woonpc.skin.cache.SkinCacheMemory;
import com.oolonghoo.woonpc.skin.dto.AshconResponse;
import com.oolonghoo.woonpc.skin.dto.MineToolsProfileResponse;
import com.oolonghoo.woonpc.skin.dto.MineToolsUuidResponse;
import com.oolonghoo.woonpc.skin.dto.MojangSessionResponse;
import com.oolonghoo.woonpc.skin.dto.MojangUuidResponse;
import com.oolonghoo.woonpc.util.PlaceholderUtil;
import com.oolonghoo.woonpc.util.RateLimiter;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class SkinManager {
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    private static final Pattern UUID_NO_DASHES_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{32}$"
    );
    
    private static final String MOJANG_UUID_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_SESSION_API = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String ASHCON_API = "https://api.ashcon.app/mojang/v2/user/";
    private static final String MINE_TOOLS_API = "https://api.minetools.eu/uuid/";
    private static final String MINE_TOOLS_SESSION_API = "https://api.minetools.eu/profile/";
    
    private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000L;
    
    private final Gson gson = new Gson();
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final SkinCacheMemory memCache;
    private final SkinCacheFile fileCache;
    private final Map<String, UUID> uuidCache = createLRUCache(1000);
    private final JavaPlugin plugin;
    
    // 速率限制器：每个 API 每秒最多 2 个请求
    private final RateLimiter rateLimiter = RateLimiter.create(2, TimeUnit.SECONDS);
    
    private boolean useSkinsRestorer = true;
    private boolean useMojang = true;
    private boolean useAshcon = true;
    private boolean useMineTools = true;
    
    private Object skinsRestorerApi;
    private boolean skinsRestorerHooked = false;
    
    private static java.lang.reflect.Method gameProfilePropertiesMethod;
    
    static {
        try {
            gameProfilePropertiesMethod = com.mojang.authlib.GameProfile.class.getMethod("getProperties");
        } catch (NoSuchMethodException e) {
            try {
                gameProfilePropertiesMethod = com.mojang.authlib.GameProfile.class.getMethod("properties");
            } catch (NoSuchMethodException ex) {
                Bukkit.getLogger().warning("[WooNPC] 无法获取 GameProfile.getProperties 方法：" + ex.getMessage());
            }
        }
    }
    
    private static com.mojang.authlib.properties.PropertyMap getProfileProperties(com.mojang.authlib.GameProfile profile) {
        if (profile == null) return null;
        try {
            if (gameProfilePropertiesMethod != null) {
                return (com.mojang.authlib.properties.PropertyMap) gameProfilePropertiesMethod.invoke(profile);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[WooNPC] 获取 GameProfile 属性失败：" + e.getMessage());
        }
        return null;
    }
    
    /**
     * 创建 LRU 缓存（LinkedHashMap 实现）
     * @param maxSize 最大缓存数量
     * @return LRU 缓存 Map
     */
    private static <K, V> Map<K, V> createLRUCache(int maxSize) {
        return new java.util.LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        };
    }
    
    public SkinManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.memCache = new SkinCacheMemory();
        this.fileCache = new SkinCacheFile(plugin);
        this.executor = Executors.newFixedThreadPool(
                3,
                r -> {
                    Thread thread = new Thread(r, "WooNPC-SkinManager");
                    thread.setDaemon(true);
                    return thread;
                }
        );
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(executor)
                .build();
        
        hookSkinsRestorer();
    }
    
    private void hookSkinsRestorer() {
        if (!useSkinsRestorer) return;
        
        try {
            org.bukkit.plugin.Plugin srPlugin = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
            if (srPlugin == null || !srPlugin.isEnabled()) {
                plugin.getLogger().info("SkinsRestorer 未安装，跳过 Hook");
                return;
            }
            
            Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
            java.lang.reflect.Method getMethod = providerClass.getMethod("get");
            skinsRestorerApi = getMethod.invoke(null);
            
            if (skinsRestorerApi != null) {
                skinsRestorerHooked = true;
                plugin.getLogger().info("成功 Hook SkinsRestorer API");
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("SkinsRestorer API 类未找到，可能版本不兼容");
        } catch (Exception e) {
            plugin.getLogger().warning("Hook SkinsRestorer 失败: " + e.getMessage());
        }
    }
    
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
    
    public SkinData getSkin(String identifier) {
        return getSkin(identifier, SkinData.SkinVariant.AUTO);
    }
    
    public SkinData getSkin(String identifier, SkinData.SkinVariant variant) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        
        identifier = PlaceholderUtil.setPlaceholder(null, identifier.trim());
        
        SkinData cached = getCachedSkin(identifier);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }
        
        if (isUUID(identifier)) {
            return getSkinByUUID(parseUUID(identifier), variant).join();
        } else {
            return getSkinByName(identifier, variant).join();
        }
    }
    
    public CompletableFuture<SkinData> getSkinAsync(String identifier) {
        return getSkinAsync(identifier, SkinData.SkinVariant.AUTO);
    }
    
    /**
     * 异步获取皮肤数据
     * 
     * @param identifier 皮肤标识符（玩家名、UUID 等）
     * @param variant 皮肤变体（HALF 或 FULL）
     * @return CompletableFuture 包含皮肤数据，失败时返回 null
     * @throws IllegalArgumentException 当标识符无效时
     */
    public CompletableFuture<SkinData> getSkinAsync(String identifier, SkinData.SkinVariant variant) {
        final String finalIdentifier;
        if (identifier != null && !identifier.isBlank()) {
            finalIdentifier = PlaceholderUtil.setPlaceholder(null, identifier.trim());
        } else {
            finalIdentifier = identifier;
        }
        return CompletableFuture.supplyAsync(() -> getSkin(finalIdentifier, variant), executor);
    }
    
    /**
     * 通过玩家名称异步获取皮肤
     * 
     * @param name 玩家名称
     * @param variant 皮肤变体
     * @return CompletableFuture 包含皮肤数据
     * @throws IllegalArgumentException 当玩家名称无效时
     */
    public CompletableFuture<SkinData> getSkinByName(String name, SkinData.SkinVariant variant) {
        if (name == null || name.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        
        final String playerName = PlaceholderUtil.setPlaceholder(null, name.trim());
        
        SkinData cached = getCachedSkin(playerName);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // 1. 先检查在线玩家
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            SkinData skinData = getPlayerSkin(onlinePlayer);
            if (skinData != null) {
                cacheSkin(playerName, skinData);
                return CompletableFuture.completedFuture(skinData);
            }
        }
        
        // 2. 尝试 SkinsRestorer
        if (skinsRestorerHooked && useSkinsRestorer) {
            SkinData srSkin = getSkinFromSkinsRestorer(playerName);
            if (srSkin != null) {
                cacheSkin(playerName, srSkin);
                return CompletableFuture.completedFuture(srSkin);
            }
        }
        
        // 3. 尝试 Mojang API
        if (useMojang) {
            return getSkinFromMojang(playerName, variant).thenApply(skin -> {
                if (skin != null) {
                    cacheSkin(playerName, skin);
                    return skin;
                }
                // 4. Mojang 失败，尝试 Ashcon
                if (useAshcon) {
                    return fetchSkinFromAshcon(playerName, variant).join();
                }
                return null;
            });
        }
        
        // 5. 尝试 Ashcon
        if (useAshcon) {
            return fetchSkinFromAshcon(playerName, variant).thenApply(skin -> {
                if (skin != null) {
                    cacheSkin(playerName, skin);
                }
                return skin;
            });
        }
        
        // 6. 最后尝试 MineTools
        if (useMineTools) {
            return fetchSkinFromMineTools(playerName, variant).thenApply(skin -> {
                if (skin != null) {
                    cacheSkin(playerName, skin);
                }
                return skin;
            });
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    private SkinData getSkinFromSkinsRestorer(String playerName) {
        if (!skinsRestorerHooked || skinsRestorerApi == null) {
            return null;
        }
        
        try {
            Class<?> skinStorageClass = Class.forName("net.skinsrestorer.api.storage.SkinStorage");
            java.lang.reflect.Method getSkinStorageMethod = skinsRestorerApi.getClass().getMethod("getSkinStorage");
            Object skinStorage = getSkinStorageMethod.invoke(skinsRestorerApi);
            
            if (skinStorage == null) return null;
            
            java.lang.reflect.Method getPlayerSkinMethod = skinStorageClass.getMethod("getPlayerSkin", String.class, boolean.class);
            Optional<?> result = (Optional<?>) getPlayerSkinMethod.invoke(skinStorage, playerName, true);
            
            if (result.isPresent()) {
                Object skinDataResult = result.get();
                java.lang.reflect.Method getSkinPropertyMethod = skinDataResult.getClass().getMethod("getSkinProperty");
                Object skinProperty = getSkinPropertyMethod.invoke(skinDataResult);
                
                if (skinProperty != null) {
                    java.lang.reflect.Method getValueMethod = skinProperty.getClass().getMethod("getValue");
                    java.lang.reflect.Method getSignatureMethod = skinProperty.getClass().getMethod("getSignature");
                    
                    String value = (String) getValueMethod.invoke(skinProperty);
                    String signature = (String) getSignatureMethod.invoke(skinProperty);
                    
                    if (value != null && !value.isEmpty()) {
                        plugin.getLogger().info("从 SkinsRestorer 获取玩家 " + playerName + " 的皮肤成功");
                        return new SkinData(playerName, SkinData.SkinVariant.AUTO, value, signature);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("从 SkinsRestorer 获取皮肤失败: " + e.getMessage());
        }
        
        return null;
    }
    
    private CompletableFuture<SkinData> getSkinFromMojang(String playerName, SkinData.SkinVariant variant) {
        return getUUIDFromMojang(playerName).thenCompose(uuid -> {
            if (uuid == null) {
                return CompletableFuture.completedFuture(null);
            }
            uuidCache.put(playerName.toLowerCase(), uuid);
            return getSkinByUUID(uuid, variant);
        });
    }
    
    private CompletableFuture<SkinData> fetchSkinFromAshcon(String playerName, SkinData.SkinVariant variant) {
        return CompletableFuture.supplyAsync(() -> {
            // 速率限制检查
            if (!rateLimiter.tryAcquire("ashcon")) {
                long waitMs = rateLimiter.getRemainingWaitMs("ashcon");
                plugin.getLogger().fine("Ashcon API 速率限制，等待 " + waitMs + "ms");
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(ASHCON_API + playerName))
                        .GET()
                        .timeout(Duration.ofSeconds(15))
                        .header("Accept", "application/json")
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    plugin.getLogger().warning("Ashcon API 失败 (玩家: " + playerName + ", 状态码: " + response.statusCode() + ")");
                    return null;
                }
                
                return parseAshconResponse(playerName, variant, response.body());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ashcon API 失败 (玩家: " + playerName + ")", e);
                return null;
            }
        }, executor);
    }
    
    private CompletableFuture<SkinData> fetchSkinFromMineTools(String playerName, SkinData.SkinVariant variant) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 先获取 UUID
                HttpRequest uuidRequest = HttpRequest.newBuilder()
                        .uri(new URI(MINE_TOOLS_API + playerName))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .build();
                
                HttpResponse<String> uuidResponse = httpClient.send(uuidRequest, HttpResponse.BodyHandlers.ofString());
                if (uuidResponse.statusCode() < 200 || uuidResponse.statusCode() >= 300) {
                    return null;
                }
                
                UUID uuid = parseMineToolsUuidResponse(uuidResponse.body());
                if (uuid == null) return null;
                
                // 获取皮肤
                HttpRequest skinRequest = HttpRequest.newBuilder()
                        .uri(new URI(MINE_TOOLS_SESSION_API + uuid.toString().replace("-", "")))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .build();
                
                HttpResponse<String> skinResponse = httpClient.send(skinRequest, HttpResponse.BodyHandlers.ofString());
                if (skinResponse.statusCode() < 200 || skinResponse.statusCode() >= 300) {
                    return null;
                }
                
                return parseMineToolsSkinResponse(playerName, variant, skinResponse.body());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "MineTools API 失败 (玩家: " + playerName + ")", e);
                return null;
            }
        }, executor);
    }
    
    /**
     * 通过 UUID 异步获取皮肤
     * 
     * @param uuid 玩家 UUID
     * @param variant 皮肤变体
     * @return CompletableFuture 包含皮肤数据
     */
    public CompletableFuture<SkinData> getSkinByUUID(UUID uuid, SkinData.SkinVariant variant) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        String uuidString = uuid.toString();
        
        SkinData cached = getCachedSkin(uuidString);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return fetchSkinFromMojangSession(uuid, variant);
    }
    
    private CompletableFuture<UUID> getUUIDFromMojang(String name) {
        UUID cachedUuid = uuidCache.get(name.toLowerCase());
        if (cachedUuid != null) {
            return CompletableFuture.completedFuture(cachedUuid);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(MOJANG_UUID_API + name))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/json")
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    return null;
                }
                
                return parseUuidResponse(response.body());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Mojang UUID API 失败 (玩家名: " + name + ")", e);
                return null;
            }
        }, executor);
    }
    
    private CompletableFuture<SkinData> fetchSkinFromMojangSession(UUID uuid, SkinData.SkinVariant variant) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String uuidString = uuid.toString().replace("-", "");
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(MOJANG_SESSION_API + uuidString + "?unsigned=false"))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/json")
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    plugin.getLogger().warning("Mojang Session API 失败 (UUID: " + uuid + ", 状态码: " + response.statusCode() + ")");
                    return null;
                }
                
                return parseSkinResponse(uuid.toString(), variant, response.body());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Mojang Session API 失败 (UUID: " + uuid + ")", e);
                return null;
            }
        }, executor);
    }
    
    public SkinData getPlayerSkin(Player player) {
        if (player == null) {
            return null;
        }
        
        try {
            net.minecraft.server.level.ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
            com.mojang.authlib.GameProfile profile = serverPlayer.getGameProfile();
            
            com.mojang.authlib.properties.PropertyMap properties = getProfileProperties(profile);
            if (properties == null) {
                return null;
            }
            
            Collection<com.mojang.authlib.properties.Property> textures = properties.get("textures");
            if (textures == null || textures.isEmpty()) {
                return null;
            }
            
            com.mojang.authlib.properties.Property property = textures.iterator().next();
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
    
    public CompletableFuture<SkinData> getPlayerSkinAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> getPlayerSkin(player), executor);
    }
    
    // ==================== 缓存操作 ====================
    
    public void cacheSkin(String identifier, SkinData skinData) {
        if (identifier == null || skinData == null) {
            return;
        }
        memCache.addSkin(identifier.toLowerCase(), skinData);
        fileCache.addSkin(identifier.toLowerCase(), skinData);
    }
    
    public SkinData getCachedSkin(String identifier) {
        if (identifier == null) {
            return null;
        }
        
        String key = identifier.toLowerCase();
        
        SkinData memSkin = memCache.getSkin(key);
        if (memSkin != null) {
            return memSkin;
        }
        
        SkinCacheData fileCacheData = fileCache.getCacheData(key);
        if (fileCacheData != null && !fileCacheData.isExpired()) {
            memCache.addSkin(key, fileCacheData.getSkinData());
            return fileCacheData.getSkinData();
        }
        
        return null;
    }
    
    public void clearCache() {
        memCache.clear();
        fileCache.clear();
        uuidCache.clear();
    }
    
    public void cleanExpiredCache() {
        memCache.cleanExpired();
        fileCache.cleanExpired();
    }
    
    public int getCacheSize() {
        return memCache.size();
    }
    
    public int getFileCacheSize() {
        return fileCache.size();
    }
    
    public SkinCacheMemory getMemCache() {
        return memCache;
    }
    
    public SkinCacheFile getFileCache() {
        return fileCache;
    }
    
    // ==================== JSON 解析 ====================
    
    private UUID parseUuidResponse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            MojangUuidResponse response = gson.fromJson(json, MojangUuidResponse.class);
            if (response == null || response.getId() == null) {
                plugin.getLogger().warning("Mojang UUID API 响应格式异常: " + json);
                return null;
            }
            return parseUUID(response.getId());
        } catch (JsonSyntaxException e) {
            plugin.getLogger().log(Level.WARNING, "解析 Mojang UUID 响应失败: " + json, e);
            return null;
        }
    }
    
    private SkinData parseSkinResponse(String identifier, SkinData.SkinVariant variant, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            MojangSessionResponse response = gson.fromJson(json, MojangSessionResponse.class);
            if (response == null || response.getProperties() == null || response.getProperties().isEmpty()) {
                plugin.getLogger().warning("Mojang Session API 响应格式异常: " + json);
                return null;
            }
            
            for (MojangSessionResponse.Property property : response.getProperties()) {
                if ("textures".equals(property.getName())) {
                    if (property.getValue() == null || property.getValue().isEmpty() ||
                        property.getSignature() == null || property.getSignature().isEmpty()) {
                        plugin.getLogger().warning("Mojang Session API 响应中 textures 属性值无效: " + json);
                        return null;
                    }
                    SkinData skinData = new SkinData(identifier, variant, property.getValue(), property.getSignature());
                    skinData.setCacheDuration(CACHE_DURATION_MS);
                    cacheSkin(identifier, skinData);
                    return skinData;
                }
            }
            
            plugin.getLogger().warning("Mojang Session API 响应中未找到 textures 属性: " + json);
            return null;
        } catch (JsonSyntaxException e) {
            plugin.getLogger().log(Level.WARNING, "解析 Mojang Session 响应失败: " + json, e);
            return null;
        }
    }
    
    private SkinData parseAshconResponse(String playerName, SkinData.SkinVariant variant, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            AshconResponse response = gson.fromJson(json, AshconResponse.class);
            if (response == null || response.getTextures() == null || response.getTextures().getRaw() == null) {
                plugin.getLogger().warning("Ashcon API 响应格式异常: " + json);
                return null;
            }
            
            AshconResponse.RawTexture raw = response.getTextures().getRaw();
            if (raw.getValue() == null || raw.getValue().isEmpty() ||
                raw.getSignature() == null || raw.getSignature().isEmpty()) {
                plugin.getLogger().warning("Ashcon API 响应中纹理值无效: " + json);
                return null;
            }
            SkinData skinData = new SkinData(playerName, variant, raw.getValue(), raw.getSignature());
            skinData.setCacheDuration(CACHE_DURATION_MS);
            return skinData;
        } catch (JsonSyntaxException e) {
            plugin.getLogger().log(Level.WARNING, "解析 Ashcon 响应失败: " + json, e);
            return null;
        }
    }
    
    private UUID parseMineToolsUuidResponse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            MineToolsUuidResponse response = gson.fromJson(json, MineToolsUuidResponse.class);
            if (response == null || response.getId() == null) {
                plugin.getLogger().warning("MineTools UUID API 响应格式异常: " + json);
                return null;
            }
            return parseUUID(response.getId());
        } catch (JsonSyntaxException e) {
            plugin.getLogger().log(Level.WARNING, "解析 MineTools UUID 响应失败: " + json, e);
            return null;
        }
    }
    
    private SkinData parseMineToolsSkinResponse(String playerName, SkinData.SkinVariant variant, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            MineToolsProfileResponse response = gson.fromJson(json, MineToolsProfileResponse.class);
            if (response == null || response.getProperties() == null || response.getProperties().length == 0) {
                plugin.getLogger().warning("MineTools Profile API 响应格式异常: " + json);
                return null;
            }
            
            for (MineToolsProfileResponse.Property property : response.getProperties()) {
                if ("textures".equals(property.getName())) {
                    if (property.getValue() == null || property.getValue().isEmpty() ||
                        property.getSignature() == null || property.getSignature().isEmpty()) {
                        plugin.getLogger().warning("MineTools Profile API 响应中 textures 属性值无效: " + json);
                        return null;
                    }
                    SkinData skinData = new SkinData(playerName, variant, property.getValue(), property.getSignature());
                    skinData.setCacheDuration(CACHE_DURATION_MS);
                    return skinData;
                }
            }
            
            plugin.getLogger().warning("MineTools Profile API 响应中未找到 textures 属性: " + json);
            return null;
        } catch (JsonSyntaxException e) {
            plugin.getLogger().log(Level.WARNING, "解析 MineTools Profile 响应失败: " + json, e);
            return null;
        }
    }
    
    // ==================== 工具方法 ====================
    
    public static boolean isUUID(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return UUID_PATTERN.matcher(str).matches() || UUID_NO_DASHES_PATTERN.matcher(str).matches();
    }
    
    public static UUID parseUUID(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        str = str.trim();
        
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
    
    public UUID getCachedUUID(String name) {
        return uuidCache.get(name.toLowerCase());
    }
    
    // ==================== 配置方法 ====================
    
    public void setUseSkinsRestorer(boolean use) {
        this.useSkinsRestorer = use;
        if (use && !skinsRestorerHooked) {
            hookSkinsRestorer();
        }
    }
    
    public void setUseMojang(boolean use) {
        this.useMojang = use;
    }
    
    public void setUseAshcon(boolean use) {
        this.useAshcon = use;
    }
    
    public void setUseMineTools(boolean use) {
        this.useMineTools = use;
    }
    
    public boolean isSkinsRestorerHooked() {
        return skinsRestorerHooked;
    }
}
