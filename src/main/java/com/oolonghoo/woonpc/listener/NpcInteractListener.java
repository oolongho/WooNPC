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

/**
 * NPC 交互监听器
 * 处理玩家与 NPC 的交互事件
 * 
 * @author oolongho
 */
public class NpcInteractListener implements Listener {
    
    private final WooNPC plugin;
    private final NpcManager npcManager;
    private final ActionManager actionManager;
    
    public NpcInteractListener(WooNPC plugin) {
        this.plugin = plugin;
        this.npcManager = plugin.getNpcManager();
        this.actionManager = plugin.getActionManager();
    }
    
    /**
     * 处理玩家与未知实体交互事件 (Paper特有事件，用于NPC)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerUseUnknownEntity(PlayerUseUnknownEntityEvent event) {
        Npc npc = findNpcByEntityId(event.getEntityId());
        if (npc == null) {
            return;
        }
        
        // 只处理主手交互
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // 检查冷却时间
        if (isOnCooldown(player, npc)) {
            return;
        }
        
        // 更新最后交互时间
        updateLastInteraction(player, npc);
        
        // 确定触发器类型
        ActionTrigger trigger = event.isAttack() ? ActionTrigger.LEFT_CLICK : ActionTrigger.RIGHT_CLICK;
        
        // 执行动作
        executeNpcActions(npc, player, trigger);
    }
    
    /**
     * 处理玩家与实体交互事件 (备用，用于真实实体)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // 只处理主手交互
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        int entityId = event.getRightClicked().getEntityId();
        
        // 查找对应的 NPC
        Npc npc = findNpcByEntityId(entityId);
        if (npc == null) {
            return;
        }
        
        // 取消默认交互
        event.setCancelled(true);
        
        // 检查冷却时间
        if (isOnCooldown(player, npc)) {
            return;
        }
        
        // 更新最后交互时间
        updateLastInteraction(player, npc);
        
        // 确定触发器类型
        ActionTrigger trigger = player.isSneaking() ? ActionTrigger.LEFT_CLICK : ActionTrigger.RIGHT_CLICK;
        
        // 执行动作
        executeNpcActions(npc, player, trigger);
    }
    
    /**
     * 根据实体 ID 查找 NPC
     * 
     * @param entityId 实体 ID
     * @return NPC 对象，如果未找到则返回 null
     */
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
    
    /**
     * 检查是否在冷却时间内
     * 
     * @param player 玩家
     * @param npc    NPC
     * @return 是否在冷却中
     */
    private boolean isOnCooldown(Player player, Npc npc) {
        float cooldown = npc.getData().getInteractionCooldown();
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
    
    /**
     * 更新最后交互时间
     * 
     * @param player 玩家
     * @param npc    NPC
     */
    private void updateLastInteraction(Player player, Npc npc) {
        npc.getLastPlayerInteraction().put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * 执行 NPC 动作
     * 
     * @param npc     NPC
     * @param player  玩家
     * @param trigger 触发器
     */
    private void executeNpcActions(Npc npc, Player player, ActionTrigger trigger) {
        if (actionManager == null) {
            return;
        }
        
        // 在主线程执行动作
        if (Bukkit.isPrimaryThread()) {
            actionManager.executeActions(npc, player, trigger);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                actionManager.executeActions(npc, player, trigger);
            });
        }
    }
}
