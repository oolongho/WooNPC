package com.oolonghoo.woonpc.config;

import com.oolonghoo.woonpc.WooNPC;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigLoader {

    private final WooNPC plugin;
    private FileConfiguration config;

    private boolean debug;
    private String language;
    private int autoSaveInterval;

    private int visibilityDistance;
    private int turnToPlayerDistance;
    private int removeFromPlayerlistDelay;
    private String defaultSkin;
    private boolean showInTabByDefault;
    private boolean turnToPlayerByDefault;

    private double hologramLineSpacing;
    private double hologramOffsetFromHead;

    private int skinCacheDuration;
    private List<String> skinApiPriority;
    private boolean enableSkinsRestorer;
    private boolean enableMojang;
    private boolean enableAshcon;
    private boolean enableMineTools;

    private int clickCooldown;
    private boolean allowLeftClick;
    private boolean allowRightClick;

    private int visibilityCheckInterval;
    private int lookUpdateInterval;
    private int maxNpcsPerWorld;
    private int placeholderRefreshInterval;

    private int skinCacheExpiryHours;
    private boolean skinFileCacheEnabled;

    private boolean visibilityCheckChunkLoaded;
    private boolean visibilityCacheEnabled;

    public ConfigLoader(WooNPC plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.debug = config.getBoolean("settings.debug", false);
        this.language = config.getString("settings.language", "zh-CN");
        this.autoSaveInterval = config.getInt("settings.auto-save-interval", 300);

        this.visibilityDistance = config.getInt("npc.visibility-distance", 50);
        this.turnToPlayerDistance = config.getInt("npc.turn-to-player-distance", 5);
        this.removeFromPlayerlistDelay = config.getInt("npc.remove-npcs-from-playerlist-delay", 20);
        this.defaultSkin = config.getString("npc.default-skin", "");
        this.showInTabByDefault = config.getBoolean("npc.show-in-tab-by-default", false);
        this.turnToPlayerByDefault = config.getBoolean("npc.turn-to-player-by-default", true);

        this.hologramLineSpacing = config.getDouble("hologram.line-spacing", 0.3);
        this.hologramOffsetFromHead = config.getDouble("hologram.offset-from-head", 0.5);

        this.skinCacheDuration = config.getInt("skin.cache-duration", 86400);
        this.skinApiPriority = config.getStringList("skin.api-priority");
        if (this.skinApiPriority.isEmpty()) {
            this.skinApiPriority = java.util.Arrays.asList("skinsrestorer", "mojang", "ashcon", "minetools");
        }
        this.enableSkinsRestorer = config.getBoolean("skin.enable.skinsrestorer", true);
        this.enableMojang = config.getBoolean("skin.enable.mojang", true);
        this.enableAshcon = config.getBoolean("skin.enable.ashcon", true);
        this.enableMineTools = config.getBoolean("skin.enable.minetools", true);

        this.clickCooldown = config.getInt("action.click-cooldown", 10);
        this.allowLeftClick = config.getBoolean("action.allow-left-click", true);
        this.allowRightClick = config.getBoolean("action.allow-right-click", true);

        this.visibilityCheckInterval = config.getInt("performance.visibility-check-interval", 20);
        this.lookUpdateInterval = config.getInt("performance.look-update-interval", 2);
        this.maxNpcsPerWorld = config.getInt("performance.max-npcs-per-world", 0);
        this.placeholderRefreshInterval = config.getInt("performance.placeholder-refresh-interval", 100);

        this.skinCacheExpiryHours = config.getInt("cache.skin.expiry-hours", 24);
        this.skinFileCacheEnabled = config.getBoolean("cache.skin.file-cache-enabled", true);

        this.visibilityCheckChunkLoaded = config.getBoolean("visibility.check-chunk-loaded", true);
        this.visibilityCacheEnabled = config.getBoolean("visibility.cache-enabled", true);
    }

    public boolean isDebug() { return debug; }
    public String getLanguage() { return language; }
    public int getAutoSaveInterval() { return autoSaveInterval; }
    public int getVisibilityDistance() { return visibilityDistance; }
    public int getTurnToPlayerDistance() { return turnToPlayerDistance; }
    public int getRemoveFromPlayerlistDelay() { return removeFromPlayerlistDelay; }
    public String getDefaultSkin() { return defaultSkin; }
    public boolean isShowInTabByDefault() { return showInTabByDefault; }
    public boolean isTurnToPlayerByDefault() { return turnToPlayerByDefault; }
    public double getHologramLineSpacing() { return hologramLineSpacing; }
    public double getHologramOffsetFromHead() { return hologramOffsetFromHead; }
    public int getSkinCacheDuration() { return skinCacheDuration; }
    public List<String> getSkinApiPriority() { return skinApiPriority; }
    public boolean isEnableSkinsRestorer() { return enableSkinsRestorer; }
    public boolean isEnableMojang() { return enableMojang; }
    public boolean isEnableAshcon() { return enableAshcon; }
    public boolean isEnableMineTools() { return enableMineTools; }
    public int getClickCooldown() { return clickCooldown; }
    public boolean isAllowLeftClick() { return allowLeftClick; }
    public boolean isAllowRightClick() { return allowRightClick; }
    public int getVisibilityCheckInterval() { return visibilityCheckInterval; }
    public int getLookUpdateInterval() { return lookUpdateInterval; }
    public int getMaxNpcsPerWorld() { return maxNpcsPerWorld; }
    public int getPlaceholderRefreshInterval() { return placeholderRefreshInterval; }
    public int getSkinCacheExpiryHours() { return skinCacheExpiryHours; }
    public boolean isSkinFileCacheEnabled() { return skinFileCacheEnabled; }
    public boolean isVisibilityCheckChunkLoaded() { return visibilityCheckChunkLoaded; }
    public boolean isVisibilityCacheEnabled() { return visibilityCacheEnabled; }
}
