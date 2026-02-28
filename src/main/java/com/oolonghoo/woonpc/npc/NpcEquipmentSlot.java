package com.oolonghoo.woonpc.npc;

/**
 * NPC 装备槽位枚举
 * 对应 Minecraft 的装备槽位
 * 
 * @author oolongho
 */
public enum NpcEquipmentSlot {
    
    MAIN_HAND("mainhand", "mainhand"),
    OFF_HAND("offhand", "offhand"),
    HEAD("head", "head"),
    CHEST("chest", "chest"),
    LEGS("legs", "legs"),
    FEET("feet", "feet"),
    BODY("body", "body");
    
    private final String nmsName;
    private final String configName;
    
    NpcEquipmentSlot(String nmsName, String configName) {
        this.nmsName = nmsName;
        this.configName = configName;
    }
    
    /**
     * 获取 NMS (Net Minecraft Server) 格式的槽位名称
     * 
     * @return NMS 槽位名称
     */
    public String getNmsName() {
        return nmsName;
    }
    
    /**
     * 获取配置文件中使用的槽位名称
     * 
     * @return 配置槽位名称
     */
    public String getConfigName() {
        return configName;
    }
    
    /**
     * 根据配置名称获取装备槽位
     * 
     * @param configName 配置名称
     * @return 对应的装备槽位，如果不存在则返回 null
     */
    public static NpcEquipmentSlot fromConfigName(String configName) {
        for (NpcEquipmentSlot slot : values()) {
            if (slot.configName.equalsIgnoreCase(configName)) {
                return slot;
            }
        }
        return null;
    }
    
    /**
     * 根据索引获取装备槽位
     * 索引顺序: 0=MAIN_HAND, 1=OFF_HAND, 2=HEAD, 3=CHEST, 4=LEGS, 5=FEET, 6=BODY
     * 
     * @param index 索引值
     * @return 对应的装备槽位
     */
    public static NpcEquipmentSlot fromIndex(int index) {
        if (index < 0 || index >= values().length) {
            return null;
        }
        return values()[index];
    }
}
