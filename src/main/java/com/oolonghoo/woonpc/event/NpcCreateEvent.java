package com.oolonghoo.woonpc.event;

import com.oolonghoo.woonpc.npc.Npc;
import com.oolonghoo.woonpc.npc.NpcData;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * NPC 创建事件
 * 在 NPC 被创建时触发
 * o
 */
public class NpcCreateEvent extends NpcEvent implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    
    public NpcCreateEvent(Npc npc) {
        super(npc);
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
    
    /**
     * 获取 NPC 数据
     * 
     * @return NPC 数据
     */
    public NpcData getNpcData() {
        return npc.getData();
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
