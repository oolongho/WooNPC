package com.oolonghoo.woonpc.npc;

import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NPC 发光颜色枚举
 * 对应 Minecraft Team 的颜色
 * 
 */
public enum GlowingColor {
    
    DISABLED("disabled", null),
    BLACK("black", NamedTextColor.BLACK),
    DARK_BLUE("dark_blue", NamedTextColor.DARK_BLUE),
    DARK_GREEN("dark_green", NamedTextColor.DARK_GREEN),
    DARK_AQUA("dark_aqua", NamedTextColor.DARK_AQUA),
    DARK_RED("dark_red", NamedTextColor.DARK_RED),
    DARK_PURPLE("dark_purple", NamedTextColor.DARK_PURPLE),
    GOLD("gold", NamedTextColor.GOLD),
    GRAY("gray", NamedTextColor.GRAY),
    DARK_GRAY("dark_gray", NamedTextColor.DARK_GRAY),
    BLUE("blue", NamedTextColor.BLUE),
    GREEN("green", NamedTextColor.GREEN),
    AQUA("aqua", NamedTextColor.AQUA),
    RED("red", NamedTextColor.RED),
    LIGHT_PURPLE("light_purple", NamedTextColor.LIGHT_PURPLE),
    YELLOW("yellow", NamedTextColor.YELLOW),
    WHITE("white", NamedTextColor.WHITE);
    
    private final String configName;
    private final NamedTextColor adventureColor;
    
    GlowingColor(String configName, NamedTextColor adventureColor) {
        this.configName = configName;
        this.adventureColor = adventureColor;
    }
    
    /**
     * 获取配置文件中使用的颜色名称
     * 
     * @return 配置颜色名称
     */
    public String getConfigName() {
        return configName;
    }
    
    /**
     * 获取 Adventure API 的颜色对象
     * 
     * @return Adventure NamedTextColor，如果是 DISABLED 则返回 null
     */
    @Nullable
    public NamedTextColor getAdventureColor() {
        return adventureColor;
    }
    
    /**
     * 检查是否禁用发光
     * 
     * @return 是否禁用
     */
    public boolean isDisabled() {
        return this == DISABLED;
    }
    
    /**
     * 根据配置名称获取发光颜色
     * 
     * @param configName 配置名称
     * @return 对应的发光颜色，如果不存在则返回 WHITE
     */
    @NotNull
    public static GlowingColor fromConfigName(String configName) {
        if (configName == null || configName.isEmpty()) {
            return DISABLED;
        }
        for (GlowingColor color : values()) {
            if (color.configName.equalsIgnoreCase(configName)) {
                return color;
            }
        }
        return WHITE;
    }
    
    /**
     * 根据 NamedTextColor 获取发光颜色
     * 
     * @param color Adventure 颜色
     * @return 对应的发光颜色，如果不存在则返回 WHITE
     */
    @NotNull
    public static GlowingColor fromAdventureColor(@Nullable NamedTextColor color) {
        if (color == null) {
            return DISABLED;
        }
        for (GlowingColor glowingColor : values()) {
            if (color.equals(glowingColor.adventureColor)) {
                return glowingColor;
            }
        }
        return WHITE;
    }
}
