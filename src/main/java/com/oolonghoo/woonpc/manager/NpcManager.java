package com.oolonghoo.woonpc.manager;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.action.ActionManager;
import com.oolonghoo.woonpc.npc.Npc;
import com.oolonghoo.woonpc.npc.NpcData;
import com.oolonghoo.woonpc.npc.NpcImpl;
import com.oolonghoo.woonpc.util.DebugManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NPC管理器
 * 负责NPC的创建、删除、查询和持久化
 * 
 * @author oolongho
 */
public class NpcManager {

    private final WooNPC plugin;
    private final DebugManager debug;
    
    // NPC存储
    private final Map<String, Npc> npcsByName = new ConcurrentHashMap<>();
    private final Map<UUID, Npc> npcsById = new ConcurrentHashMap<>();
    
    // Tickable NPC 索引 (有可见玩家的 NPC)
    private final Set<UUID> tickableNpcs = ConcurrentHashMap.newKeySet();
    
    // 是否有 tickable NPC 的状态标志
    private final AtomicBoolean hasTickableNpcs = new AtomicBoolean(false);
    
    // 数据文件
    private File dataFile;
    private FileConfiguration dataConfig;

    public NpcManager(WooNPC plugin) {
        this.plugin = plugin;
        this.debug = plugin.getDebugManager();
    }

    /**
     * 从文件加载所有NPC
     */
    public void loadNpcs() {
        // 初始化数据文件
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        dataFile = new File(dataFolder, "npcs.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建NPC数据文件: " + e.getMessage());
                return;
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // 清空现有NPC
        npcsByName.clear();
        npcsById.clear();
        
        // 加载NPC
        ConfigurationSection npcsSection = dataConfig.getConfigurationSection("npcs");
        if (npcsSection == null) {
            return;
        }
        
        for (String name : npcsSection.getKeys(false)) {
            ConfigurationSection npcSection = npcsSection.getConfigurationSection(name);
            if (npcSection != null) {
                try {
                    NpcData data = NpcData.fromConfig(npcSection);
                    Npc npc = new NpcImpl(data);
                    npc.create();
                    npcsByName.put(name.toLowerCase(), npc);
                    npcsById.put(UUID.fromString(data.getId()), npc);
                    
                    // 从原始配置加载动作
                    loadActionsFromConfig(data, npcSection);
                    
                    // 同步动作到 ActionManager
                    ActionManager actionManager = plugin.getActionManager();
                    String npcId = data.getId();
                    for (Map.Entry<com.oolonghoo.woonpc.action.ActionTrigger, List<com.oolonghoo.woonpc.action.NpcAction.NpcActionData>> entry : data.getActions().entrySet()) {
                        actionManager.setNpcActions(npcId, entry.getKey(), entry.getValue());
                    }
                    
                    debug.debug("加载NPC: " + name);
                } catch (Exception e) {
                    plugin.getLogger().warning("加载NPC " + name + " 失败: " + e.getMessage());
                }
            }
        }
        
        plugin.getLogger().info("已加载 " + npcsByName.size() + " 个NPC");
    }

    /**
     * 保存所有NPC到文件 (增量保存)
     * 只保存有变更的NPC
     */
    public void saveNpcs() {
        saveNpcs(false);
    }
    
    /**
     * 保存NPC到文件
     * 
     * @param force true=强制保存所有NPC; false=只保存脏标记为true的NPC
     * @return 实际保存的NPC数量
     */
    public int saveNpcs(boolean force) {
        if (dataConfig == null) {
            return 0;
        }
        
        int savedCount = 0;
        
        if (force) {
            // 强制保存：清空旧数据，保存所有NPC
            dataConfig.set("npcs", null);
            
            for (Map.Entry<String, Npc> entry : npcsByName.entrySet()) {
                String name = entry.getKey();
                Npc npc = entry.getValue();
                ConfigurationSection npcSection = dataConfig.createSection("npcs." + name);
                npc.getData().saveToConfig(npcSection);
                npc.getData().setDirty(false);
                savedCount++;
            }
        } else {
            // 增量保存：只保存有变更的NPC
            boolean hasChanges = false;
            
            for (Map.Entry<String, Npc> entry : npcsByName.entrySet()) {
                Npc npc = entry.getValue();
                if (npc.getData().isDirty()) {
                    hasChanges = true;
                    break;
                }
            }
            
            if (!hasChanges) {
                debug.debug("没有需要保存的NPC变更");
                return 0;
            }
            
            // 需要保存时，重建整个配置
            dataConfig.set("npcs", null);
            
            for (Map.Entry<String, Npc> entry : npcsByName.entrySet()) {
                String name = entry.getKey();
                Npc npc = entry.getValue();
                ConfigurationSection npcSection = dataConfig.createSection("npcs." + name);
                npc.getData().saveToConfig(npcSection);
                
                if (npc.getData().isDirty()) {
                    npc.getData().setDirty(false);
                    savedCount++;
                }
            }
        }
        
        // 写入文件
        try {
            dataConfig.save(dataFile);
            debug.debug("保存了 " + savedCount + " 个NPC (force=" + force + ")");
        } catch (IOException e) {
            plugin.getLogger().severe("保存NPC数据失败: " + e.getMessage());
        }
        
        return savedCount;
    }
    
    /**
     * 保存所有NPC到文件 (强制保存)
     */
    public void saveAllNpcs() {
        saveNpcs(true);
    }

    /**
     * 创建NPC
     * 
     * @param name NPC名称
     * @param location 位置
     * @return 创建的NPC，如果已存在则返回null
     */
    public Npc createNpc(String name, Location location) {
        return createNpc(name, location, org.bukkit.entity.EntityType.PLAYER);
    }
    
    /**
     * 创建NPC
     * 
     * @param name NPC名称
     * @param location 位置
     * @param entityType 实体类型
     * @return 创建的NPC，如果已存在则返回null
     */
    public Npc createNpc(String name, Location location, org.bukkit.entity.EntityType entityType) {
        if (npcsByName.containsKey(name.toLowerCase())) {
            return null;
        }
        
        NpcData data = new NpcData(name, null, location);
        data.setType(entityType);
        Npc npc = new NpcImpl(data);
        npc.create();
        
        npcsByName.put(name.toLowerCase(), npc);
        npcsById.put(UUID.fromString(data.getId()), npc);
        
        // 更新世界缓存
        plugin.getVisibilityTracker().addNpcToWorldCache(npc);
        
        // 立即保存
        saveNpcs();
        
        debug.debug("创建NPC: " + name + " (类型: " + entityType.name() + ")");
        return npc;
    }

    /**
     * 删除NPC
     * 
     * @param name NPC名称
     * @return 是否成功删除
     */
    public boolean removeNpc(String name) {
        Npc npc = npcsByName.remove(name.toLowerCase());
        if (npc == null) {
            return false;
        }
        
        UUID npcUuid = UUID.fromString(npc.getData().getId());
        npcsById.remove(npcUuid);
        
        // 从 tickable 索引移除
        removeFromTickableIndex(npcUuid);
        
        // 从世界缓存移除
        plugin.getVisibilityTracker().removeNpcFromWorldCache(npc);
        
        // 移除所有玩家的NPC
        npc.removeForAll();
        
        // 立即保存
        saveNpcs();
        
        debug.debug("删除NPC: " + name);
        return true;
    }

    /**
     * 根据名称获取NPC
     * 
     * @param name NPC名称
     * @return NPC对象，不存在则返回null
     */
    public Npc getNpc(String name) {
        return npcsByName.get(name.toLowerCase());
    }

    /**
     * 根据ID获取NPC
     * 
     * @param id NPC唯一ID
     * @return NPC对象，不存在则返回null
     */
    public Npc getNpc(UUID id) {
        return npcsById.get(id);
    }

    /**
     * 获取所有NPC
     * 
     * @return NPC集合
     */
    public Collection<Npc> getAllNpcs() {
        return Collections.unmodifiableCollection(npcsByName.values());
    }

    /**
     * 获取指定世界的NPC
     * 
     * @param world 世界
     * @return NPC列表
     */
    public List<Npc> getNpcsInWorld(World world) {
        List<Npc> result = new ArrayList<>();
        String worldName = world.getName();
        
        for (Npc npc : npcsByName.values()) {
            Location loc = npc.getData().getLocation();
            if (loc != null && loc.getWorld() != null && 
                loc.getWorld().getName().equals(worldName)) {
                result.add(npc);
            }
        }
        
        return result;
    }

    /**
     * 检查NPC是否存在
     * 
     * @param name NPC名称
     * @return 是否存在
     */
    public boolean exists(String name) {
        return npcsByName.containsKey(name.toLowerCase());
    }

    /**
     * 获取NPC数量
     * 
     * @return NPC数量
     */
    public int getNpcCount() {
        return npcsByName.size();
    }
    
    /**
     * 将 NPC 添加到 tickable 索引
     * 当 NPC 有玩家变为可见时调用
     * 
     * @param npcId NPC 的 UUID
     */
    public void addToTickableIndex(UUID npcId) {
        if (tickableNpcs.add(npcId)) {
            hasTickableNpcs.set(true);
            debug.debug("NPC " + npcId + " 添加到 tickable 索引");
        }
    }
    
    /**
     * 从 tickable 索引移除 NPC
     * 当 NPC 没有可见玩家时调用
     * 
     * @param npcId NPC 的 UUID
     */
    public void removeFromTickableIndex(UUID npcId) {
        if (tickableNpcs.remove(npcId)) {
            if (tickableNpcs.isEmpty()) {
                hasTickableNpcs.set(false);
            }
            debug.debug("NPC " + npcId + " 从 tickable 索引移除");
        }
    }
    
    /**
     * 检查是否有 tickable NPC
     * 
     * @return 是否有需要 tick 的 NPC
     */
    public boolean hasTickableNpcs() {
        return hasTickableNpcs.get();
    }
    
    /**
     * 获取 tickable NPC 数量
     * 
     * @return tickable NPC 数量
     */
    public int getTickableNpcCount() {
        return tickableNpcs.size();
    }
    
    /**
     * 获取 tickable NPC ID 集合
     * 
     * @return tickable NPC ID 集合（只读）
     */
    public Set<UUID> getTickableNpcIds() {
        return Collections.unmodifiableSet(tickableNpcs);
    }
    
    private void loadActionsFromConfig(NpcData data, ConfigurationSection npcSection) {
        ConfigurationSection actionsSection = npcSection.getConfigurationSection("actions");
        if (actionsSection == null) {
            plugin.getLogger().info("No actions section found for NPC " + data.getName());
            return;
        }
        
        plugin.getLogger().info("Loading actions for NPC " + data.getName());
        
        ActionManager actionManager = plugin.getActionManager();
        
        for (String triggerName : actionsSection.getKeys(false)) {
            ConfigurationSection triggerSection = actionsSection.getConfigurationSection(triggerName);
            if (triggerSection == null) {
                continue;
            }
            
            try {
                com.oolonghoo.woonpc.action.ActionTrigger trigger = 
                    com.oolonghoo.woonpc.action.ActionTrigger.valueOf(triggerName.toUpperCase());
                
                List<com.oolonghoo.woonpc.action.NpcAction.NpcActionData> actionList = new ArrayList<>();
                
                for (String indexStr : triggerSection.getKeys(false)) {
                    ConfigurationSection actionSection = triggerSection.getConfigurationSection(indexStr);
                    if (actionSection == null) {
                        continue;
                    }
                    
                    String actionType = actionSection.getString("type", "");
                    String value = actionSection.getString("value", "");
                    int order = actionSection.getInt("order", 0);
                    
                    com.oolonghoo.woonpc.action.NpcAction action = actionManager.getAction(actionType);
                    if (action != null) {
                        actionList.add(new com.oolonghoo.woonpc.action.NpcAction.NpcActionData(order, action, value));
                        plugin.getLogger().info("Loaded action: " + actionType + " for trigger: " + triggerName);
                    } else {
                        plugin.getLogger().warning("Unknown action type: " + actionType);
                    }
                }
                
                if (!actionList.isEmpty()) {
                    actionList.sort(Comparator.comparingInt(com.oolonghoo.woonpc.action.NpcAction.NpcActionData::order));
                    data.setActions(trigger, actionList);
                    plugin.getLogger().info("Loaded " + actionList.size() + " actions for trigger " + triggerName);
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid trigger name: " + triggerName);
            }
        }
    }
}
