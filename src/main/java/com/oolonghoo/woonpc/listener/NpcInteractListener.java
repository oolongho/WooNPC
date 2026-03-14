package com.oolonghoo.woonpc.listener;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.action.ActionManager;
import com.oolonghoo.woonpc.action.ActionTrigger;
import com.oolonghoo.woonpc.manager.NpcManager;
import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcInteractListener implements Listener {
    
    private final WooNPC plugin;
    private final NpcManager npcManager;
    private final ActionManager actionManager;
    private final Map<String, Long> recentInteractions = new ConcurrentHashMap<>();
    private static final long INTERACTION_THRESHOLD_MS = 100;
    
    public NpcInteractListener(WooNPC plugin) {
        this.plugin = plugin;
        this.npcManager = plugin.getNpcManager();
        this.actionManager = plugin.getActionManager();
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerUseUnknownEntity(PlayerUseUnknownEntityEvent event) {
        Npc npc = findNpcByEntityId(event.getEntityId());
        if (npc == null) {
            return;
        }
        
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        
        String key = player.getUniqueId() + ":" + npc.getEntityId();
        long now = System.currentTimeMillis();
        Long lastTime = recentInteractions.get(key);
        if (lastTime != null && now - lastTime < INTERACTION_THRESHOLD_MS) {
            return;
        }
        recentInteractions.put(key, now);
        
        if (isOnCooldown(player, npc)) {
            return;
        }
        
        updateLastInteraction(player, npc);
        
        ActionTrigger trigger = event.isAttack() ? ActionTrigger.LEFT_CLICK : ActionTrigger.RIGHT_CLICK;
        
        executeNpcActions(npc, player, trigger);
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        int entityId = event.getRightClicked().getEntityId();
        
        Npc npc = findNpcByEntityId(entityId);
        if (npc == null) {
            return;
        }
        
        String key = player.getUniqueId() + ":" + npc.getEntityId();
        long now = System.currentTimeMillis();
        Long lastTime = recentInteractions.get(key);
        if (lastTime != null && now - lastTime < INTERACTION_THRESHOLD_MS) {
            event.setCancelled(true);
            return;
        }
        recentInteractions.put(key, now);
        
        event.setCancelled(true);
        
        if (isOnCooldown(player, npc)) {
            return;
        }
        
        updateLastInteraction(player, npc);
        
        executeNpcActions(npc, player, ActionTrigger.RIGHT_CLICK);
    }
    
    private Npc findNpcByEntityId(int entityId) {
        if (npcManager == null) {
            return null;
        }
        
        for (Npc npc : npcManager.getAllNpcs()) {
            if (npc.getEntityId() == entityId) {
                return npc;
            }
        }
        
        return null;
    }
    
    private boolean isOnCooldown(Player player, Npc npc) {
        float npcCooldown = npc.getData().getInteractionCooldown();
        
        float cooldown;
        if (npcCooldown > 0) {
            cooldown = npcCooldown;
        } else {
            cooldown = plugin.getConfigLoader().getClickCooldown() / 1000.0f;
        }
        
        if (cooldown <= 0) {
            return false;
        }
        
        Map<UUID, Long> lastInteractions = npc.getLastPlayerInteraction();
        Long lastTime = lastInteractions.get(player.getUniqueId());
        
        if (lastTime == null) {
            return false;
        }
        
        long cooldownMs = (long) (cooldown * 1000);
        return System.currentTimeMillis() - lastTime < cooldownMs;
    }
    
    private void updateLastInteraction(Player player, Npc npc) {
        npc.getLastPlayerInteraction().put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    private void executeNpcActions(Npc npc, Player player, ActionTrigger trigger) {
        if (actionManager == null) {
            return;
        }
        
        // 获取动作列表
        java.util.List<com.oolonghoo.woonpc.action.NpcAction.NpcActionData> actions = 
            new java.util.ArrayList<>();
        java.util.List<com.oolonghoo.woonpc.action.NpcAction.NpcActionData> directActions = 
            actionManager.getNpcActions(npc.getData().getId(), trigger);
        actions.addAll(directActions);
        
        // 添加 ANY_CLICK 触发器的动作
        java.util.List<com.oolonghoo.woonpc.action.NpcAction.NpcActionData> anyClickActions = 
            actionManager.getNpcActions(npc.getData().getId(), ActionTrigger.ANY_CLICK);
        actions.addAll(anyClickActions);
        
        // 触发 NPC 交互事件
        com.oolonghoo.woonpc.event.NpcInteractEvent interactEvent = 
            new com.oolonghoo.woonpc.event.NpcInteractEvent(npc, player, trigger, actions);
        org.bukkit.Bukkit.getPluginManager().callEvent(interactEvent);
        
        if (interactEvent.isCancelled()) {
            return;
        }
        
        // 使用事件中可能被修改的动作列表
        actions = interactEvent.getActions();
        
        if (actions.isEmpty()) {
            return;
        }
        
        // 排序动作
        actions.sort(java.util.Comparator.comparingInt(com.oolonghoo.woonpc.action.NpcAction.NpcActionData::order));
        
        // 执行动作
        if (Bukkit.isPrimaryThread()) {
            actionManager.executeActions(npc, player, trigger);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                actionManager.executeActions(npc, player, trigger);
            });
        }
    }
}
