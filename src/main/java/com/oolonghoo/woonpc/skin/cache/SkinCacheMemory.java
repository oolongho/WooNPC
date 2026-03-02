package com.oolonghoo.woonpc.skin.cache;

import com.oolonghoo.woonpc.skin.SkinData;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存皮肤缓存实现
 * 使用 ConcurrentHashMap 实现线程安全的内存缓存
 * 
 * @author oolongho
 */
public class SkinCacheMemory implements SkinCache {
    
    private final ConcurrentHashMap<String, SkinCacheData> cache;
    
    public SkinCacheMemory() {
        this.cache = new ConcurrentHashMap<>();
    }
    
    @Override
    public SkinData getSkin(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        
        String key = identifier.toLowerCase();
        SkinCacheData cacheData = cache.get(key);
        
        if (cacheData == null) {
            return null;
        }
        
        if (cacheData.isExpired()) {
            cache.remove(key);
            return null;
        }
        
        return cacheData.getSkinData();
    }
    
    @Override
    public void addSkin(String identifier, SkinData skinData) {
        if (identifier == null || identifier.isBlank() || skinData == null) {
            return;
        }
        
        String key = identifier.toLowerCase();
        SkinCacheData cacheData = new SkinCacheData(identifier, skinData);
        cache.put(key, cacheData);
    }
    
    @Override
    public void removeSkin(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        
        cache.remove(identifier.toLowerCase());
    }
    
    @Override
    public void clear() {
        cache.clear();
    }
    
    @Override
    public int size() {
        return cache.size();
    }
    
    @Override
    public void cleanExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
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
        
        String key = identifier.toLowerCase();
        SkinCacheData cacheData = cache.get(key);
        
        if (cacheData == null || cacheData.isExpired()) {
            return null;
        }
        
        return cacheData;
    }
}
