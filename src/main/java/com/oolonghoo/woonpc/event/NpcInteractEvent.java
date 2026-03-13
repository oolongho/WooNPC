package com.oolonghoo.woonpc.event;

import com.oolonghoo.woonpc.action.ActionTrigger;
import com.oolonghoo.woonpc.action.NpcAction;
import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import java.util.List;

/**
 * NPC 交互事件
 * 在玩家与 NPC 交互时触发
 * o
 */
public class NpcInteractEvent extends NpcEvent implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ActionTrigger trigger;
    private List<NpcAction.NpcActionData> actions;
    private boolean cancelled = false;
    
    public NpcInteractEvent(Npc npc, Player player, ActionTrigger trigger, List<NpcAction.NpcActionData> actions) {
        super(npc);
        this.player = player;
        this.trigger = trigger;
        this.actions = actions;
    }
    
    /**
     * 获取交互玩家
     * 
     * @return 玩家
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * 获取触发器类型
     * 
     * @return 触发器
     */
    public ActionTrigger getTrigger() {
        return trigger;
    }
    
    /**
     * 获取动作列表
     * 
     * @return 动作列表
     */
    public List<NpcAction.NpcActionData> getActions() {
        return actions;
    }
    
    /**
     * 设置动作列表
     * 允许第三方插件修改要执行的动作
     * 
     * @param actions 新的动作列表
     */
    public void setActions(List<NpcAction.NpcActionData> actions) {
        this.actions = actions;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
