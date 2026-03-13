package com.oolonghoo.woonpc.skin.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.oolonghoo.woonpc.skin.SkinData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * 文件皮肤缓存实现
 * 使用 JSON 文件持久化皮肤缓存
 * 
 */
public class SkinCacheFile implements SkinCache {
    
    private final JavaPlugin plugin;
    private final File cacheDir;
    private final Gson gson;
    
    public SkinCacheFile(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cacheDir = new File(plugin.getDataFolder(), "skins");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                plugin.getLogger().warning("无法创建皮肤缓存目录: " + cacheDir.getAbsolutePath());
            }
        }
    }
    
    @Override
    public SkinData getSkin(String identifier) {
        SkinCacheData cacheData = getCacheData(identifier);
        return cacheData != null ? cacheData.getSkinData() : null;
    }
    
    /**
     * 获取缓存数据（包含元信息）
     * 
     * @param identifier 皮肤标识符
     * @return 缓存数据，如果不存在或已过期则返回 null
     */
    public SkinCacheData getCacheData(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        
        File cacheFile = getCacheFile(identifier);
        if (!cacheFile.exists()) {
            return null;
        }
        
        try {
            String json = readFile(cacheFile.toPath());
            if (json == null || json.isEmpty()) {
                return null;
            }
            
            CacheFileData fileData = gson.fromJson(json, CacheFileData.class);
            if (fileData == null) {
                return null;
            }
            
            SkinData skinData = fileData.toSkinData();
            SkinCacheData cacheData = new SkinCacheData(
                    fileData.identifier,
                    skinData,
                    fileData.timestamp,
                    fileData.expiryTime
            );
            
            if (cacheData.isExpired()) {
                deleteFile(cacheFile);
                return null;
            }
            
            return cacheData;
            
        } catch (JsonSyntaxException e) {
            plugin.getLogger().log(Level.WARNING, "解析皮肤缓存文件失败: " + cacheFile.getName(), e);
            deleteFile(cacheFile);
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "读取皮肤缓存文件失败: " + cacheFile.getName(), e);
            return null;
        }
    }
    
    @Override
    public void addSkin(String identifier, SkinData skinData) {
        if (identifier == null || identifier.isBlank() || skinData == null) {
            return;
        }
        
        File cacheFile = getCacheFile(identifier);
        
        try {
            CacheFileData fileData = new CacheFileData(identifier, skinData);
            String json = gson.toJson(fileData);
            writeFile(cacheFile.toPath(), json);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "写入皮肤缓存文件失败: " + cacheFile.getName(), e);
        }
    }
    
    @Override
    public void removeSkin(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        
        File cacheFile = getCacheFile(identifier);
        deleteFile(cacheFile);
    }
    
    @Override
    public void clear() {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                deleteFile(file);
            }
        }
    }
    
    @Override
    public int size() {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".json"));
        return files != null ? files.length : 0;
    }
    
    @Override
    public void cleanExpired() {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            try {
                String json = readFile(file.toPath());
                if (json == null || json.isEmpty()) {
                    deleteFile(file);
                    continue;
                }
                
                CacheFileData fileData = gson.fromJson(json, CacheFileData.class);
                if (fileData == null) {
                    deleteFile(file);
                    continue;
                }
                
                SkinCacheData cacheData = new SkinCacheData(
                        fileData.identifier,
                        fileData.toSkinData(),
                        fileData.timestamp,
                        fileData.expiryTime
                );
                
                if (cacheData.isExpired()) {
                    deleteFile(file);
                }
                
            } catch (Exception e) {
                deleteFile(file);
            }
        }
    }
    
    private File getCacheFile(String identifier) {
        String safeName = sanitizeFileName(identifier.toLowerCase());
        return new File(cacheDir, safeName + ".json");
    }
    
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
    
    private String readFile(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }
    
    private void writeFile(Path path, String content) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
    
    private void deleteFile(File file) {
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "删除皮肤缓存文件失败: " + file.getName(), e);
            }
        }
    }
    
    /**
     * 用于 JSON 序列化的内部数据类
     */
    private static class CacheFileData {
        String identifier;
        String variant;
        String textureValue;
        String textureSignature;
        long timestamp;
        long expiryTime;
        
        CacheFileData(String identifier, SkinData skinData) {
            this.identifier = identifier;
            this.variant = skinData.getVariant().name();
            this.textureValue = skinData.getTextureValue();
            this.textureSignature = skinData.getTextureSignature();
            this.timestamp = System.currentTimeMillis();
            this.expiryTime = skinData.getExpiryTime();
        }
        
        SkinData toSkinData() {
            SkinData skinData = new SkinData(
                    identifier,
                    SkinData.SkinVariant.valueOf(variant),
                    textureValue,
                    textureSignature
            );
            skinData.setExpiryTime(expiryTime);
            return skinData;
        }
    }
}
