package com.oolonghoo.woonpc;

import com.oolonghoo.woonpc.action.ActionManager;
import com.oolonghoo.woonpc.action.types.ConsoleCommandAction;
import com.oolonghoo.woonpc.action.types.ExecuteRandomActionAction;
import com.oolonghoo.woonpc.action.types.MessageAction;
import com.oolonghoo.woonpc.action.types.NeedPermissionAction;
import com.oolonghoo.woonpc.action.types.PlayerCommandAction;
import com.oolonghoo.woonpc.action.types.PlayerCommandAsOpAction;
import com.oolonghoo.woonpc.action.types.PlaySoundAction;
import com.oolonghoo.woonpc.action.types.WaitAction;
import com.oolonghoo.woonpc.command.MainCommand;
import com.oolonghoo.woonpc.config.ConfigLoader;
import com.oolonghoo.woonpc.config.MessageManager;
import com.oolonghoo.woonpc.hook.HologramHook;
import com.oolonghoo.woonpc.listener.NpcInteractListener;
import com.oolonghoo.woonpc.listener.PlayerJoinListener;
import com.oolonghoo.woonpc.listener.PlayerQuitListener;
import com.oolonghoo.woonpc.manager.NpcManager;
import com.oolonghoo.woonpc.npc.Npc;
import com.oolonghoo.woonpc.npc.NpcData;
import com.oolonghoo.woonpc.npc.NpcImpl;
import com.oolonghoo.woonpc.skin.SkinManager;
import com.oolonghoo.woonpc.tracker.LookTracker;
import com.oolonghoo.woonpc.tracker.VisibilityTracker;
import com.oolonghoo.woonpc.util.DebugManager;
import com.oolonghoo.woonpc.util.PlaceholderUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WooNPC - 轻量级高性能 NPC 插件
 * 
 * @author oolongho
 */
public class WooNPC extends JavaPlugin {
    
    private static WooNPC instance;
    
    // 配置管理器
    private ConfigLoader configLoader;
    private MessageManager messageManager;
    
    // NPC 管理器
    private NpcManager npcManager;
    
    // 追踪器
    private VisibilityTracker visibilityTracker;
    private LookTracker lookTracker;
    
    // 调试管理器
    private DebugManager debugManager;
    
    // 动作管理器
    private ActionManager actionManager;
    
    // 皮肤管理器
    private SkinManager skinManager;
    
    // 全息图 Hook
    private HologramHook hologramHook;
    
    // 占位符刷新任务 ID
    private int placeholderRefreshTaskId = -1;
    
    // NPC 存储 (临时保留用于兼容)
    private final Map<String, Npc> npcs = new ConcurrentHashMap<>();
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化配置
        this.configLoader = new ConfigLoader(this);
        this.configLoader.load();
        
        // 初始化消息管理器
        this.messageManager = new MessageManager(this);
        this.messageManager.load();
        
        // 初始化调试管理器
        this.debugManager = new DebugManager(this);
        
        // 初始化 NPC 管理器
        this.npcManager = new NpcManager(this);
        
        // 初始化动作管理器
        this.actionManager = new ActionManager();
        
        // 初始化皮肤管理器
        this.skinManager = new SkinManager(this);
        
        // 初始化全息图 Hook
        this.hologramHook = new HologramHook(this);
        
        // 应用皮肤 API 配置
        applySkinConfig();
        
        // 注册默认动作
        registerDefaultActions();
        
        // 初始化追踪器
        this.visibilityTracker = new VisibilityTracker(this);
        this.lookTracker = new LookTracker(this);
        
        // 注册监听器
        registerListeners();
        
        // 注册命令
        registerCommands();
        
        // 加载NPC数据
        npcManager.loadNpcs();
        
        // 为所有玩家生成NPC
        for (Npc npc : npcManager.getAllNpcs()) {
            npc.spawnForAll();
        }
        
        // 启动追踪器
        startTrackers();
        
        getLogger().info("WooNPC v" + getPluginMeta().getVersion() + " 已启用!");
    }
    
    @Override
    public void onDisable() {
        // 停止追踪器
        stopTrackers();
        
        // 保存所有 NPC 数据
        saveAllNpcs();
        
        // 移除所有 NPC
        for (Npc npc : npcs.values()) {
            npc.removeForAll();
        }
        npcs.clear();
        
        // 关闭皮肤管理器
        if (skinManager != null) {
            skinManager.shutdown();
        }
        
        // 关闭全息图 Hook
        if (hologramHook != null) {
            hologramHook.shutdown();
        }
        
        getLogger().info("WooNPC 已禁用!");
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NpcInteractListener(this), this);
    }
    
    private void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        
        if (getCommand("woonpc") != null) {
            getCommand("woonpc").setExecutor(mainCommand);
            getCommand("woonpc").setTabCompleter(mainCommand);
        } else {
            getLogger().warning("无法注册命令: woonpc 命令未在 plugin.yml 中定义");
        }
    }
    
    /**
     * 注册默认动作类型
     */
    private void registerDefaultActions() {
        actionManager.registerAction(new MessageAction());
        actionManager.registerAction(new PlayerCommandAction());
        actionManager.registerAction(new ConsoleCommandAction());
        actionManager.registerAction(new PlaySoundAction());
        actionManager.registerAction(new WaitAction());
        actionManager.registerAction(new ExecuteRandomActionAction());
        actionManager.registerAction(new NeedPermissionAction());
        actionManager.registerAction(new PlayerCommandAsOpAction());
        
        getLogger().info("已注册 " + actionManager.getAllActions().size() + " 个默认动作类型");
    }
    
    private void startTrackers() {
        visibilityTracker.start();
        lookTracker.start();
        startPlaceholderRefreshTask();
    }
    
    private void stopTrackers() {
        if (visibilityTracker != null) {
            visibilityTracker.stop();
        }
        if (lookTracker != null) {
            lookTracker.stop();
        }
        stopPlaceholderRefreshTask();
    }
    
    private void startPlaceholderRefreshTask() {
        if (!PlaceholderUtil.isPlaceholderApiEnabled()) {
            return;
        }
        
        int interval = configLoader.getPlaceholderRefreshInterval();
        if (interval <= 0) {
            return;
        }
        
        // 确保间隔合理，避免过于频繁执行影响主线程性能
        // 最小 20 ticks (1秒)，最大 6000 ticks (5分钟)
        final int MIN_INTERVAL = 20;
        final int MAX_INTERVAL = 6000;
        if (interval < MIN_INTERVAL) {
            getLogger().warning("占位符刷新间隔 " + interval + " ticks 过小，已调整为最小值 " + MIN_INTERVAL + " ticks");
            interval = MIN_INTERVAL;
        } else if (interval > MAX_INTERVAL) {
            getLogger().warning("占位符刷新间隔 " + interval + " ticks 过大，已调整为最大值 " + MAX_INTERVAL + " ticks");
            interval = MAX_INTERVAL;
        }
        
        // 使用同步任务，因为 npc.updateForAll() 需要在主线程执行
        placeholderRefreshTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Npc npc : npcManager.getAllNpcs()) {
                String displayName = npc.getData().getDisplayName();
                if (PlaceholderUtil.containsPlaceholders(displayName)) {
                    npc.updateForAll();
                }
            }
        }, interval, interval).getTaskId();
        
        getLogger().info("已启动占位符刷新任务，间隔: " + interval + " ticks");
    }
    
    private void stopPlaceholderRefreshTask() {
        if (placeholderRefreshTaskId != -1) {
            Bukkit.getScheduler().cancelTask(placeholderRefreshTaskId);
            placeholderRefreshTaskId = -1;
        }
    }
    
    private void saveAllNpcs() {
        if (npcManager != null) {
            npcManager.saveAllNpcs();
        }
    }
    
    // ==================== NPC 管理方法 ====================
    
    /**
     * 创建新 NPC
     * 
     * @param data NPC 数据
     * @return 创建的 NPC
     */
    public Npc createNpc(NpcData data) {
        NpcImpl npc = new NpcImpl(data);
        npc.create();
        npcs.put(data.getId(), npc);
        npc.spawnForAll();
        return npc;
    }
    
    /**
     * 获取 NPC
     * 
     * @param id NPC ID
     * @return NPC 对象，如果不存在则返回 null
     */
    public Npc getNpc(String id) {
        return npcs.get(id);
    }
    
    /**
     * 根据 NPC 名称获取 NPC
     * 
     * @param name NPC 名称
     * @return NPC 对象，如果不存在则返回 null
     */
    public Npc getNpcByName(String name) {
        for (Npc npc : npcs.values()) {
            if (npc.getData().getName().equalsIgnoreCase(name)) {
                return npc;
            }
        }
        return null;
    }
    
    /**
     * 移除 NPC
     * 
     * @param id NPC ID
     * @return 是否成功移除
     */
    public boolean removeNpc(String id) {
        Npc npc = npcs.remove(id);
        if (npc != null) {
            npc.removeForAll();
            return true;
        }
        return false;
    }
    
    /**
     * 获取所有 NPC
     * 
     * @return NPC 集合
     */
    public Collection<Npc> getAllNpcs() {
        return npcManager.getAllNpcs();
    }
    
    /**
     * 获取 NPC 数量
     * 
     * @return NPC 数量
     */
    public int getNpcCount() {
        return npcs.size();
    }
    
    // ==================== 静态方法 ====================
    
    /**
     * 获取插件实例
     * 
     * @return 插件实例
     */
    public static WooNPC getInstance() {
        return instance;
    }
    
    // ==================== Getters ====================
    
    /**
     * 获取配置加载器
     * 
     * @return 配置加载器
     */
    public ConfigLoader getConfigLoader() {
        return configLoader;
    }
    
    /**
     * 获取消息管理器
     * 
     * @return 消息管理器
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    /**
     * 获取 NPC 管理器
     * 
     * @return NPC 管理器
     */
    public NpcManager getNpcManager() {
        return npcManager;
    }
    
    /**
     * 获取可见性追踪器
     * 
     * @return 可见性追踪器
     */
    public VisibilityTracker getVisibilityTracker() {
        return visibilityTracker;
    }
    
    /**
     * 获取头部旋转追踪器
     * 
     * @return 头部旋转追踪器
     */
    public LookTracker getLookTracker() {
        return lookTracker;
    }
    
    /**
     * 获取调试管理器
     * 
     * @return 调试管理器
     */
    public DebugManager getDebugManager() {
        return debugManager;
    }
    
    /**
     * 获取动作管理器
     * 
     * @return 动作管理器
     */
    public ActionManager getActionManager() {
        return actionManager;
    }
    
    /**
     * 获取皮肤管理器
     * 
     * @return 皮肤管理器
     */
    public SkinManager getSkinManager() {
        return skinManager;
    }
    
    public HologramHook getHologramHook() {
        return hologramHook;
    }
    
    private void applySkinConfig() {
        skinManager.setUseSkinsRestorer(configLoader.isEnableSkinsRestorer());
        skinManager.setUseMojang(configLoader.isEnableMojang());
        skinManager.setUseAshcon(configLoader.isEnableAshcon());
        skinManager.setUseMineTools(configLoader.isEnableMineTools());
    }
    
    public void reload() {
        configLoader.load();
        messageManager.load();
        
        applySkinConfig();
        
        // 先移除所有 NPC
        for (Npc npc : npcManager.getAllNpcs()) {
            npc.removeForAll();
        }
        
        // 清空 ActionManager 缓存
        getActionManager().clearAllNpcActions();
        
        // 重新加载 NPC
        npcManager.loadNpcs();
        
        // 重新生成所有 NPC 给玩家
        for (Npc npc : npcManager.getAllNpcs()) {
            npc.spawnForAll();
        }
        
        getLogger().info("配置已重载!");
    }
}
