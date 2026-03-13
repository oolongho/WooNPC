package com.oolonghoo.woonpc.action;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC 动作管理器
 * 管理所有注册的动作类型和 NPC 的动作执行
 * 
 */
public class ActionManager {
    
    /**
     * 已注册的动作映射
     */
    private final Map<String, NpcAction> actions = new ConcurrentHashMap<>();
    
    /**
     * NPC 动作映射 (NPC ID -> 触发器 -> 动作列表)
     */
    private final Map<String, Map<ActionTrigger, List<NpcAction.NpcActionData>>> npcActions = new ConcurrentHashMap<>();
    
    /**
     * 注册动作类型
     * 
     * @param action 动作对象
     */
    public void registerAction(@NotNull NpcAction action) {
        actions.put(action.getName().toLowerCase(), action);
    }
    
    /**
     * 注销动作类型
     * 
     * @param action 动作对象
     */
    public void unregisterAction(@NotNull NpcAction action) {
        actions.remove(action.getName().toLowerCase());
    }
    
    /**
     * 根据名称获取动作类型
     * 
     * @param name 动作名称
     * @return 动作对象，如果不存在则返回 null
     */
    public NpcAction getAction(@NotNull String name) {
        return actions.get(name.toLowerCase());
    }
    
    /**
     * 获取所有已注册的动作类型
     * 
     * @return 动作列表
     */
    @NotNull
    public List<NpcAction> getAllActions() {
        return new ArrayList<>(actions.values());
    }
    
    /**
     * 为 NPC 添加动作
     * 
     * @param npcId      NPC ID
     * @param trigger    触发器
     * @param actionData 动作数据
     */
    public void addNpcAction(@NotNull String npcId, @NotNull ActionTrigger trigger, 
                             @NotNull NpcAction.NpcActionData actionData) {
        npcActions.computeIfAbsent(npcId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(trigger, k -> new ArrayList<>())
                .add(actionData);
        
        // 按顺序排序
        npcActions.get(npcId).get(trigger).sort(Comparator.comparingInt(NpcAction.NpcActionData::order));
    }
    
    /**
     * 为 NPC 设置动作列表 (替换现有动作)
     * 
     * @param npcId      NPC ID
     * @param trigger    触发器
     * @param actionList 动作列表
     */
    public void setNpcActions(@NotNull String npcId, @NotNull ActionTrigger trigger,
                              @NotNull List<NpcAction.NpcActionData> actionList) {
        npcActions.computeIfAbsent(npcId, k -> new ConcurrentHashMap<>())
                .put(trigger, new ArrayList<>(actionList));
        
        // 按顺序排序
        npcActions.get(npcId).get(trigger).sort(Comparator.comparingInt(NpcAction.NpcActionData::order));
    }
    
    /**
     * 获取 NPC 的动作列表
     * 
     * @param npcId   NPC ID
     * @param trigger 触发器
     * @return 动作列表
     */
    @NotNull
    public List<NpcAction.NpcActionData> getNpcActions(@NotNull String npcId, @NotNull ActionTrigger trigger) {
        // 首先尝试从 NpcData 获取（权威数据源）
        try {
            org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("WooNPC");
            if (plugin instanceof com.oolonghoo.woonpc.WooNPC wooNPC) {
                com.oolonghoo.woonpc.npc.Npc npc = wooNPC.getNpcManager().getNpc(java.util.UUID.fromString(npcId));
                if (npc != null) {
                    List<NpcAction.NpcActionData> npcActionList = npc.getData().getActions(trigger);
                    if (npcActionList != null && !npcActionList.isEmpty()) {
                        return new ArrayList<>(npcActionList);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            // UUID 解析失败，忽略
        }
        
        // 回退到缓存获取
        Map<ActionTrigger, List<NpcAction.NpcActionData>> triggerMap = npcActions.get(npcId);
        if (triggerMap == null) {
            return Collections.emptyList();
        }
        
        List<NpcAction.NpcActionData> actionList = triggerMap.get(trigger);
        return actionList != null ? new ArrayList<>(actionList) : Collections.emptyList();
    }
    
    /**
     * 清除 NPC 的所有动作
     * 
     * @param npcId NPC ID
     */
    public void clearNpcActions(@NotNull String npcId) {
        npcActions.remove(npcId);
    }
    
    /**
     * 清除 NPC 指定触发器的动作
     * 
     * @param npcId   NPC ID
     * @param trigger 触发器
     */
    public void clearNpcActions(@NotNull String npcId, @NotNull ActionTrigger trigger) {
        Map<ActionTrigger, List<NpcAction.NpcActionData>> triggerMap = npcActions.get(npcId);
        if (triggerMap != null) {
            triggerMap.remove(trigger);
        }
    }
    
    /**
     * 清除所有 NPC 的动作缓存
     */
    public void clearAllNpcActions() {
        npcActions.clear();
    }
    
    /**
     * 执行 NPC 动作
     * 
     * @param npc     NPC 对象
     * @param player  触发玩家
     * @param trigger 触发器
     */
    public void executeActions(@NotNull Npc npc, @NotNull Player player, @NotNull ActionTrigger trigger) {
        String npcId = npc.getData().getId();
        
        List<NpcAction.NpcActionData> directActions = getNpcActions(npcId, trigger);
        
        List<NpcAction.NpcActionData> anyClickActions = Collections.emptyList();
        if (trigger == ActionTrigger.LEFT_CLICK || trigger == ActionTrigger.RIGHT_CLICK) {
            anyClickActions = getNpcActions(npcId, ActionTrigger.ANY_CLICK);
        }
        
        List<NpcAction.NpcActionData> allActions = new ArrayList<>();
        allActions.addAll(directActions);
        allActions.addAll(anyClickActions);
        allActions.sort(Comparator.comparingInt(NpcAction.NpcActionData::order));
        
        if (allActions.isEmpty()) {
            return;
        }
        
        debug("Executing " + allActions.size() + " actions for NPC " + npcId + " with trigger " + trigger);
        
        ActionExecutionContext context = new ActionExecutionContext(npc, player, trigger, allActions);
        
        while (context.getCurrentIndex() < allActions.size() && !context.isTerminated()) {
            NpcAction.NpcActionData actionData = context.getCurrentAction();
            if (actionData == null) {
                break;
            }
            
            try {
                debug("Executing action: " + actionData.action().getName() + " with value: " + actionData.value());
                actionData.executeWithContext(context);
                
                if (context.isSkipRemaining()) {
                    break;
                }
                
                if (context.getJumpToIndex() >= 0) {
                    context.setCurrentIndex(context.getJumpToIndex());
                    context.setJumpToIndex(-1);
                } else {
                    context.nextAction();
                }
            } catch (Exception e) {
                WooNPC.getInstance().getLogger().warning("Error executing action " + actionData.action().getName() + ": " + e.getMessage());
                context.nextAction();
            }
        }
    }
    
    /**
     * 检查 NPC 是否有指定触发器的动作
     * 
     * @param npcId   NPC ID
     * @param trigger 触发器
     * @return 是否有动作
     */
    public boolean hasActions(@NotNull String npcId, @NotNull ActionTrigger trigger) {
        Map<ActionTrigger, List<NpcAction.NpcActionData>> triggerMap = npcActions.get(npcId);
        if (triggerMap == null) {
            return false;
        }
        
        List<NpcAction.NpcActionData> actionList = triggerMap.get(trigger);
        return actionList != null && !actionList.isEmpty();
    }
    
    private void debug(String message) {
        WooNPC plugin = WooNPC.getInstance();
        if (plugin != null && plugin.getConfigLoader().isDebug()) {
            plugin.getLogger().info("[ActionManager] " + message);
        }
    }
}
