package com.oolonghoo.woonpc.skin;

import java.util.Objects;

/**
 * 皮肤数据类
 * 存储 NPC 皮肤的纹理值和签名信息
 * 
 */
public class SkinData {
    
    /**
     * 皮肤变体类型
     */
    public enum SkinVariant {
        AUTO,   // 自动检测
        SLIM    // 纤细手臂
    }
    
    // 标识符 (玩家名、UUID 或 URL)
    private final String identifier;
    
    // 皮肤变体
    private SkinVariant variant;
    
    // 皮肤纹理值 (Base64 编码)
    private String textureValue;
    
    // 皮肤签名 (用于验证)
    private String textureSignature;
    
    // 过期时间戳 (毫秒)
    private long expiryTime;
    
    /**
     * 完整构造函数
     * 
     * @param identifier      标识符
     * @param variant         皮肤变体
     * @param textureValue    纹理值
     * @param textureSignature 纹理签名
     */
    public SkinData(String identifier, SkinVariant variant, String textureValue, String textureSignature) {
        this.identifier = identifier;
        this.variant = variant;
        this.textureValue = textureValue;
        this.textureSignature = textureSignature;
        // 默认缓存 24 小时
        this.expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000L);
    }
    
    /**
     * 创建无纹理的皮肤数据
     * 
     * @param identifier 标识符
     * @param variant    皮肤变体
     */
    public SkinData(String identifier, SkinVariant variant) {
        this(identifier, variant, null, null);
    }
    
    /**
     * 创建默认皮肤数据
     * 
     * @param identifier 标识符
     */
    public SkinData(String identifier) {
        this(identifier, SkinVariant.AUTO, null, null);
    }
    
    /**
     * 检查是否有有效的纹理数据
     * 
     * @return 是否有纹理
     */
    public boolean hasTexture() {
        return textureValue != null 
                && !textureValue.isEmpty() 
                && textureSignature != null 
                && !textureSignature.isEmpty();
    }
    
    /**
     * 检查皮肤数据是否已过期
     * 
     * @return 是否过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
    
    // ==================== Getters ====================
    
    public String getIdentifier() {
        return identifier;
    }
    
    public SkinVariant getVariant() {
        return variant;
    }
    
    public String getTextureValue() {
        return textureValue;
    }
    
    public String getTextureSignature() {
        return textureSignature;
    }
    
    public long getExpiryTime() {
        return expiryTime;
    }
    
    // ==================== Setters ====================
    
    public void setVariant(SkinVariant variant) {
        this.variant = variant;
    }
    
    public void setTextureValue(String textureValue) {
        this.textureValue = textureValue;
    }
    
    public void setTextureSignature(String textureSignature) {
        this.textureSignature = textureSignature;
    }
    
    /**
     * 设置纹理数据
     * 
     * @param value     纹理值
     * @param signature 纹理签名
     */
    public void setTexture(String value, String signature) {
        this.textureValue = value;
        this.textureSignature = signature;
    }
    
    /**
     * 设置过期时间
     * 
     * @param expiryTime 过期时间戳 (毫秒)
     */
    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }
    
    /**
     * 设置缓存时长
     * 
     * @param durationMillis 缓存时长 (毫秒)
     */
    public void setCacheDuration(long durationMillis) {
        this.expiryTime = System.currentTimeMillis() + durationMillis;
    }
    
    /**
     * 创建副本
     * 
     * @return 皮肤数据副本
     */
    public SkinData copy() {
        SkinData copy = new SkinData(identifier, variant, textureValue, textureSignature);
        copy.expiryTime = this.expiryTime;
        return copy;
    }
    
    @Override
    public String toString() {
        return "SkinData{" +
                "identifier='" + identifier + '\'' +
                ", variant=" + variant +
                ", hasTexture=" + hasTexture() +
                ", expired=" + isExpired() +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkinData skinData = (SkinData) o;
        return Objects.equals(identifier, skinData.identifier) &&
                variant == skinData.variant &&
                Objects.equals(textureValue, skinData.textureValue) &&
                Objects.equals(textureSignature, skinData.textureSignature);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(identifier, variant, textureValue, textureSignature);
    }
}
