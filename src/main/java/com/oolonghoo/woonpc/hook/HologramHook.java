package com.oolonghoo.woonpc.hook;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramHook {
    
    private static final String HOLOGRAM_PREFIX = "woonpc_";
    
    public enum HologramPlugin {
        WOOHOLOGRAMS,
        DECENTHOLOGRAMS,
        FANCYHOLOGRAMS,
        NONE
    }
    
    private final WooNPC plugin;
    private HologramPlugin activePlugin = HologramPlugin.NONE;
    private final Map<String, String> npcHologramMap = new HashMap<>();
    
    // WooHolograms 反射缓存
    private Class<?> wooHologramsApiClass;
    private Method wooCreateHologramMethod;
    private Method wooGetHologramMethod;
    private Method wooDeleteHologramMethod;
    private Method wooSetLineMethod;
    private Method wooClearLinesMethod;
    private Method wooTeleportMethod;
    
    // DecentHolograms 反射缓存
    private Class<?> dhApiClass;
    private Method dhCreateHologramMethod;
    private Method dhGetHologramMethod;
    private Method dhRemoveHologramMethod;
    private Method dhAddLineMethod;
    private Method dhSetLinesMethod;
    private Method dhMoveHologramMethod;
    private Class<?> dhHologramClass;
    
    public HologramHook(WooNPC plugin) {
        this.plugin = plugin;
        detectHologramPlugin();
    }
    
    private void detectHologramPlugin() {
        // 优先级: WooHolograms > DecentHolograms > FancyHolograms
        if (tryHookWooHolograms()) {
            activePlugin = HologramPlugin.WOOHOLOGRAMS;
            plugin.getLogger().info("成功 Hook WooHolograms");
            return;
        }
        
        if (tryHookDecentHolograms()) {
            activePlugin = HologramPlugin.DECENTHOLOGRAMS;
            plugin.getLogger().info("成功 Hook DecentHolograms");
            return;
        }
        
        if (tryHookFancyHolograms()) {
            activePlugin = HologramPlugin.FANCYHOLOGRAMS;
            plugin.getLogger().info("成功 Hook FancyHolograms");
            return;
        }
        
        activePlugin = HologramPlugin.NONE;
        plugin.getLogger().info("未检测到全息图插件，全息图功能不可用。支持: WooHolograms, DecentHolograms, FancyHolograms");
    }
    
    private boolean tryHookWooHolograms() {
        Plugin wooHolograms = Bukkit.getPluginManager().getPlugin("WooHolograms");
        if (wooHolograms == null || !wooHolograms.isEnabled()) {
            return false;
        }
        
        try {
            wooHologramsApiClass = Class.forName("com.oolonghoo.holograms.api.WooHologramsAPI");
            Class<?> hologramClass = Class.forName("com.oolonghoo.holograms.hologram.Hologram");
            
            wooCreateHologramMethod = wooHologramsApiClass.getMethod("createHologram", String.class, Location.class);
            wooGetHologramMethod = wooHologramsApiClass.getMethod("getHologram", String.class);
            wooDeleteHologramMethod = wooHologramsApiClass.getMethod("deleteHologram", String.class);
            wooSetLineMethod = hologramClass.getMethod("setLine", int.class, String.class);
            wooClearLinesMethod = hologramClass.getMethod("clearLines");
            wooTeleportMethod = hologramClass.getMethod("teleport", Location.class);
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Hook WooHolograms 失败: " + e.getMessage());
            return false;
        }
    }
    
    private boolean tryHookDecentHolograms() {
        Plugin decentHolograms = Bukkit.getPluginManager().getPlugin("DecentHolograms");
        if (decentHolograms == null || !decentHolograms.isEnabled()) {
            return false;
        }
        
        try {
            dhApiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            dhHologramClass = Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram");
            
            dhCreateHologramMethod = dhApiClass.getMethod("createHologram", String.class, Location.class, boolean.class, List.class);
            dhGetHologramMethod = dhApiClass.getMethod("getHologram", String.class);
            dhRemoveHologramMethod = dhApiClass.getMethod("removeHologram", String.class);
            dhAddLineMethod = dhApiClass.getMethod("addHologramLine", dhHologramClass, String.class);
            dhSetLinesMethod = dhApiClass.getMethod("setHologramLines", dhHologramClass, List.class);
            dhMoveHologramMethod = dhApiClass.getMethod("moveHologram", String.class, Location.class);
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Hook DecentHolograms 失败: " + e.getMessage());
            return false;
        }
    }
    
    private boolean tryHookFancyHolograms() {
        Plugin fancyHolograms = Bukkit.getPluginManager().getPlugin("FancyHolograms");
        if (fancyHolograms == null || !fancyHolograms.isEnabled()) {
            return false;
        }
        
        try {
            // FancyHolograms API 暂不支持，需要查看其 API
            // TODO: 实现 FancyHolograms Hook
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Hook FancyHolograms 失败: " + e.getMessage());
            return false;
        }
    }
    
    public HologramPlugin getActivePlugin() {
        return activePlugin;
    }
    
    public boolean isHologramEnabled() {
        return activePlugin != HologramPlugin.NONE;
    }
    
    public void createHologram(Npc npc, Location location, List<String> lines) {
        if (activePlugin == HologramPlugin.NONE || lines == null || lines.isEmpty()) {
            return;
        }
        
        String npcId = npc.getData().getId();
        String hologramId = HOLOGRAM_PREFIX + npcId;
        
        deleteHologram(npcId);
        
        Location hologramLocation = location.clone().add(0, 2.5, 0);
        
        switch (activePlugin) {
            case WOOHOLOGRAMS:
                createWooHologram(hologramId, hologramLocation, lines, npcId);
                break;
            case DECENTHOLOGRAMS:
                createDecentHologram(hologramId, hologramLocation, lines, npcId);
                break;
            case FANCYHOLOGRAMS:
                // TODO: 实现 FancyHolograms
                break;
            default:
                break;
        }
    }
    
    private void createWooHologram(String hologramId, Location location, List<String> lines, String npcId) {
        try {
            Object hologramOpt = wooCreateHologramMethod.invoke(null, hologramId, location);
            if (hologramOpt != null) {
                Method isPresentMethod = hologramOpt.getClass().getMethod("isPresent");
                boolean isPresent = (boolean) isPresentMethod.invoke(hologramOpt);
                
                if (isPresent) {
                    Method getMethod = hologramOpt.getClass().getMethod("get");
                    Object hologram = getMethod.invoke(hologramOpt);
                    
                    for (int i = 0; i < lines.size(); i++) {
                        wooSetLineMethod.invoke(hologram, i, lines.get(i));
                    }
                    
                    npcHologramMap.put(npcId, hologramId);
                    plugin.getLogger().info("为 NPC " + npcId + " 创建了 WooHolograms 全息图");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("创建 WooHolograms 全息图失败: " + e.getMessage());
        }
    }
    
    private void createDecentHologram(String hologramId, Location location, List<String> lines, String npcId) {
        try {
            Object hologram = dhCreateHologramMethod.invoke(null, hologramId, location, false, lines);
            if (hologram != null) {
                npcHologramMap.put(npcId, hologramId);
                plugin.getLogger().info("为 NPC " + npcId + " 创建了 DecentHolograms 全息图");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("创建 DecentHolograms 全息图失败: " + e.getMessage());
        }
    }
    
    public void updateHologram(Npc npc, List<String> lines) {
        if (activePlugin == HologramPlugin.NONE || lines == null) {
            return;
        }
        
        String npcId = npc.getData().getId();
        String hologramId = npcHologramMap.get(npcId);
        
        if (hologramId == null) {
            return;
        }
        
        switch (activePlugin) {
            case WOOHOLOGRAMS:
                updateWooHologram(hologramId, lines);
                break;
            case DECENTHOLOGRAMS:
                updateDecentHologram(hologramId, lines);
                break;
            default:
                break;
        }
    }
    
    private void updateWooHologram(String hologramId, List<String> lines) {
        try {
            Object hologramOpt = wooGetHologramMethod.invoke(null, hologramId);
            if (hologramOpt != null) {
                Method isPresentMethod = hologramOpt.getClass().getMethod("isPresent");
                boolean isPresent = (boolean) isPresentMethod.invoke(hologramOpt);
                
                if (isPresent) {
                    Method getMethod = hologramOpt.getClass().getMethod("get");
                    Object hologram = getMethod.invoke(hologramOpt);
                    
                    wooClearLinesMethod.invoke(hologram);
                    for (int i = 0; i < lines.size(); i++) {
                        wooSetLineMethod.invoke(hologram, i, lines.get(i));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("更新 WooHolograms 全息图失败: " + e.getMessage());
        }
    }
    
    private void updateDecentHologram(String hologramId, List<String> lines) {
        try {
            Object hologram = dhGetHologramMethod.invoke(null, hologramId);
            if (hologram != null) {
                dhSetLinesMethod.invoke(null, hologram, lines);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("更新 DecentHolograms 全息图失败: " + e.getMessage());
        }
    }
    
    public void deleteHologram(String npcId) {
        if (activePlugin == HologramPlugin.NONE) {
            return;
        }
        
        String hologramId = npcHologramMap.remove(npcId);
        if (hologramId == null) {
            return;
        }
        
        switch (activePlugin) {
            case WOOHOLOGRAMS:
                try {
                    wooDeleteHologramMethod.invoke(null, hologramId);
                } catch (Exception e) {
                    // Ignore
                }
                break;
            case DECENTHOLOGRAMS:
                try {
                    dhRemoveHologramMethod.invoke(null, hologramId);
                } catch (Exception e) {
                    // Ignore
                }
                break;
            default:
                break;
        }
    }
    
    public void moveHologram(Npc npc, Location newLocation) {
        if (activePlugin == HologramPlugin.NONE) {
            return;
        }
        
        String npcId = npc.getData().getId();
        String hologramId = npcHologramMap.get(npcId);
        
        if (hologramId == null) {
            return;
        }
        
        Location hologramLocation = newLocation.clone().add(0, 2.5, 0);
        
        switch (activePlugin) {
            case WOOHOLOGRAMS:
                try {
                    Object hologramOpt = wooGetHologramMethod.invoke(null, hologramId);
                    if (hologramOpt != null) {
                        Method isPresentMethod = hologramOpt.getClass().getMethod("isPresent");
                        boolean isPresent = (boolean) isPresentMethod.invoke(hologramOpt);
                        
                        if (isPresent) {
                            Method getMethod = hologramOpt.getClass().getMethod("get");
                            Object hologram = getMethod.invoke(hologramOpt);
                            wooTeleportMethod.invoke(hologram, hologramLocation);
                        }
                    }
                } catch (Exception e) {
                    // Ignore
                }
                break;
            case DECENTHOLOGRAMS:
                try {
                    dhMoveHologramMethod.invoke(null, hologramId, hologramLocation);
                } catch (Exception e) {
                    // Ignore
                }
                break;
            default:
                break;
        }
    }
    
    public void shutdown() {
        if (activePlugin == HologramPlugin.NONE) {
            return;
        }
        
        for (String hologramId : new ArrayList<>(npcHologramMap.values())) {
            switch (activePlugin) {
                case WOOHOLOGRAMS:
                    try {
                        wooDeleteHologramMethod.invoke(null, hologramId);
                    } catch (Exception e) {
                        // Ignore
                    }
                    break;
                case DECENTHOLOGRAMS:
                    try {
                        dhRemoveHologramMethod.invoke(null, hologramId);
                    } catch (Exception e) {
                        // Ignore
                    }
                    break;
                default:
                    break;
            }
        }
        npcHologramMap.clear();
    }
}
