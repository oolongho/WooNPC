package com.oolonghoo.woonpc.npc;

/**
 * NPC 姿势枚举
 * 定义 NPC 可以采用的不同姿势
 * 
 * @author oolongho
 */
public enum NpcPose {
    
    /**
     * 站立姿势 (默认)
     */
    STANDING("standing"),
    
    /**
     * 蹲下姿势
     */
    CROUCHING("crouching"),
    
    /**
     * 睡觉姿势
     */
    SLEEPING("sleeping"),
    
    /**
     * 游泳姿势
     */
    SWIMMING("swimming"),
    
    /**
     * 坐下姿势 (使用载具实现)
     */
    SITTING("sitting");
    
    private final String configName;
    
    NpcPose(String configName) {
        this.configName = configName;
    }
    
    /**
     * 获取配置文件中使用的姿势名称
     * 
     * @return 配置姿势名称
     */
    public String getConfigName() {
        return configName;
    }
    
    /**
     * 根据配置名称获取姿势
     * 
     * @param configName 配置名称
     * @return 对应的姿势，如果不存在则返回 STANDING
     */
    public static NpcPose fromConfigName(String configName) {
        if (configName == null || configName.isEmpty()) {
            return STANDING;
        }
        
        for (NpcPose pose : values()) {
            if (pose.configName.equalsIgnoreCase(configName)) {
                return pose;
            }
        }
        
        return STANDING;
    }
    
    /**
     * 根据名称获取姿势 (支持枚举名称和配置名称)
     * 
     * @param name 名称
     * @return 对应的姿势，如果不存在则返回 STANDING
     */
    public static NpcPose getByName(String name) {
        if (name == null || name.isEmpty()) {
            return STANDING;
        }
        
        // 先尝试枚举名称
        for (NpcPose pose : values()) {
            if (pose.name().equalsIgnoreCase(name)) {
                return pose;
            }
        }
        
        // 再尝试配置名称
        return fromConfigName(name);
    }
}
