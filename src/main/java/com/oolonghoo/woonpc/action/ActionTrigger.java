package com.oolonghoo.woonpc.action;

/**
 * NPC 动作触发器枚举
 * 定义触发 NPC 动作的方式
 * 
 */
public enum ActionTrigger {
    
    /**
     * 左键点击触发
     */
    LEFT_CLICK,
    
    /**
     * 右键点击触发
     */
    RIGHT_CLICK,
    
    /**
     * 任意点击触发 (左键或右键)
     */
    ANY_CLICK,
    
    /**
     * 自定义触发 (通过 API 调用)
     */
    CUSTOM;
    
    /**
     * 根据名称获取触发器
     * 
     * @param name 触发器名称
     * @return 对应的触发器，如果不存在则返回 null
     */
    public static ActionTrigger getByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        for (ActionTrigger trigger : values()) {
            if (trigger.name().equalsIgnoreCase(name)) {
                return trigger;
            }
        }
        
        return null;
    }
}
