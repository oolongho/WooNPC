package com.oolonghoo.woonpc.skin.cache;

import com.oolonghoo.woonpc.skin.SkinData;

/**
 * 皮肤缓存接口
 * 定义皮肤缓存的基本操作
 * 
 * @author oolongho
 */
public interface SkinCache {
    
    /**
     * 获取缓存的皮肤数据
     * 
     * @param identifier 皮肤标识符 (玩家名或 UUID)
     * @return 皮肤数据，如果不存在或已过期则返回 null
     */
    SkinData getSkin(String identifier);
    
    /**
     * 添加皮肤到缓存
     * 
     * @param identifier 皮肤标识符
     * @param skinData   皮肤数据
     */
    void addSkin(String identifier, SkinData skinData);
    
    /**
     * 从缓存移除皮肤
     * 
     * @param identifier 皮肤标识符
     */
    void removeSkin(String identifier);
    
    /**
     * 清空所有缓存
     */
    void clear();
    
    /**
     * 获取缓存大小
     * 
     * @return 缓存中的皮肤数量
     */
    int size();
    
    /**
     * 清理过期的缓存项
     */
    void cleanExpired();
}
