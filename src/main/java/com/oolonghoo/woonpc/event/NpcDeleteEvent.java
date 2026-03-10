package com.oolonghoo.woonpc.event;

import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * NPC 删除事件
 * 在 NPC 被删除时触发
 * 
 * @author oolonghoo
 */
public class NpcDeleteEvent extends NpcEvent implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    
    public NpcDeleteEvent(Npc npc) {
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
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
