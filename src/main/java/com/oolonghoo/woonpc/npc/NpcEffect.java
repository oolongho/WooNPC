package com.oolonghoo.woonpc.npc;

import java.util.Arrays;

public enum NpcEffect {
    ON_FIRE("on_fire", "着火"),
    INVISIBLE("invisible", "隐形"),
    SHAKING("shaking", "冷颤"),
    SILENT("silent", "静音");
    
    private final String name;
    private final String displayName;
    
    NpcEffect(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static NpcEffect getByName(String name) {
        for (NpcEffect effect : values()) {
            if (effect.name.equalsIgnoreCase(name)) {
                return effect;
            }
        }
        return null;
    }
    
    public static String[] getNames() {
        return Arrays.stream(values())
                .map(NpcEffect::getName)
                .toArray(String[]::new);
    }
}
