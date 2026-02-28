package com.oolonghoo.woonpc.config;

import com.oolonghoo.woonpc.WooNPC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 配置加载器
 * 负责加载和管理插件配置
 * 
 * @author oolongho
 */
public class ConfigLoader {

    private final WooNPC plugin;
    private FileConfiguration config;

    // 设置
    private boolean debug;
    private String language;
    private int autoSaveInterval;

    // NPC 设置
    private int visibilityDistance;
    private int turnToPlayerDistance;
    private int removeFromPlayerlistDelay;
    private String defaultSkin;
    private boolean showInTabByDefault;
    private boolean turnToPlayerByDefault;

    // 全息文字设置
    private double hologramLineSpacing;
    private double hologramOffsetFromHead;

    // 皮肤设置
    private int skinCacheDuration;
    private boolean enableMineSkin;
    private String mineSkinApiKey;

    // 动作设置
    private int clickCooldown;
    private boolean allowLeftClick;
    private boolean allowRightClick;

    // 性能设置
    private int visibilityCheckInterval;
    private int lookUpdateInterval;
    private int maxNpcsPerWorld;

    public ConfigLoader(WooNPC plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载配置
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // 设置
        this.debug = config.getBoolean("settings.debug", false);
        this.language = config.getString("settings.language", "zh-CN");
        this.autoSaveInterval = config.getInt("settings.auto-save-interval", 300);

        // NPC 设置
        this.visibilityDistance = config.getInt("npc.visibility-distance", 50);
        this.turnToPlayerDistance = config.getInt("npc.turn-to-player-distance", 5);
        this.removeFromPlayerlistDelay = config.getInt("npc.remove-npcs-from-playerlist-delay", 20);
        this.defaultSkin = config.getString("npc.default-skin", "");
        this.showInTabByDefault = config.getBoolean("npc.show-in-tab-by-default", false);
        this.turnToPlayerByDefault = config.getBoolean("npc.turn-to-player-by-default", true);

        // 全息文字设置
        this.hologramLineSpacing = config.getDouble("hologram.line-spacing", 0.3);
        this.hologramOffsetFromHead = config.getDouble("hologram.offset-from-head", 0.5);

        // 皮肤设置
        this.skinCacheDuration = config.getInt("skin.cache-duration", 3600);
        this.enableMineSkin = config.getBoolean("skin.enable-mineskin", false);
        this.mineSkinApiKey = config.getString("skin.mineskin-api-key", "");

        // 动作设置
        this.clickCooldown = config.getInt("action.click-cooldown", 10);
        this.allowLeftClick = config.getBoolean("action.allow-left-click", true);
        this.allowRightClick = config.getBoolean("action.allow-right-click", true);

        // 性能设置
        this.visibilityCheckInterval = config.getInt("performance.visibility-check-interval", 20);
        this.lookUpdateInterval = config.getInt("performance.look-update-interval", 2);
        this.maxNpcsPerWorld = config.getInt("performance.max-npcs-per-world", 0);
    }

    // Getters

    public boolean isDebug() {
        return debug;
    }

    public String getLanguage() {
        return language;
    }

    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    public int getVisibilityDistance() {
        return visibilityDistance;
    }

    public int getTurnToPlayerDistance() {
        return turnToPlayerDistance;
    }

    public int getRemoveFromPlayerlistDelay() {
        return removeFromPlayerlistDelay;
    }

    public String getDefaultSkin() {
        return defaultSkin;
    }

    public boolean isShowInTabByDefault() {
        return showInTabByDefault;
    }

    public boolean isTurnToPlayerByDefault() {
        return turnToPlayerByDefault;
    }

    public double getHologramLineSpacing() {
        return hologramLineSpacing;
    }

    public double getHologramOffsetFromHead() {
        return hologramOffsetFromHead;
    }

    public int getSkinCacheDuration() {
        return skinCacheDuration;
    }

    public boolean isEnableMineSkin() {
        return enableMineSkin;
    }

    public String getMineSkinApiKey() {
        return mineSkinApiKey;
    }

    public int getClickCooldown() {
        return clickCooldown;
    }

    public boolean isAllowLeftClick() {
        return allowLeftClick;
    }

    public boolean isAllowRightClick() {
        return allowRightClick;
    }

    public int getVisibilityCheckInterval() {
        return visibilityCheckInterval;
    }

    public int getLookUpdateInterval() {
        return lookUpdateInterval;
    }

    public int getMaxNpcsPerWorld() {
        return maxNpcsPerWorld;
    }
}
