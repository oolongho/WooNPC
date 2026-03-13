package com.oolonghoo.woonpc.skin.cache;

import com.oolonghoo.woonpc.skin.SkinData;

import java.util.Objects;

/**
 * 皮肤缓存数据包装类
 * 包含皮肤数据和缓存元信息
 * 
 */
public class SkinCacheData {
    
    private static final long DEFAULT_EXPIRY_MS = 24 * 60 * 60 * 1000L;
    
    private final SkinData skinData;
    private final long timestamp;
    private final String identifier;
    private final long expiryTime;
    
    public SkinCacheData(String identifier, SkinData skinData) {
        this.identifier = identifier;
        this.skinData = skinData;
        this.timestamp = System.currentTimeMillis();
        this.expiryTime = this.timestamp + DEFAULT_EXPIRY_MS;
    }
    
    public SkinCacheData(String identifier, SkinData skinData, long timestamp) {
        this.identifier = identifier;
        this.skinData = skinData;
        this.timestamp = timestamp;
        this.expiryTime = this.timestamp + DEFAULT_EXPIRY_MS;
    }
    
    public SkinCacheData(String identifier, SkinData skinData, long timestamp, long expiryTime) {
        this.identifier = identifier;
        this.skinData = skinData;
        this.timestamp = timestamp;
        this.expiryTime = expiryTime;
    }
    
    /**
     * 检查缓存是否过期
     * 
     * @return 是否过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
    
    public SkinData getSkinData() {
        return skinData;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public long getExpiryTime() {
        return expiryTime;
    }
    
    @Override
    public String toString() {
        return "SkinCacheData{" +
                "identifier='" + identifier + '\'' +
                ", timestamp=" + timestamp +
                ", expired=" + isExpired() +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkinCacheData that = (SkinCacheData) o;
        return timestamp == that.timestamp &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(skinData, that.skinData);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(identifier, skinData, timestamp);
    }
}
