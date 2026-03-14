package com.oolonghoo.woonpc.event;

import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * NPC 删除事件
 * 在 NPC 被删除时触发
 * o
 */
public class NpcDeleteEvent extends NpcEvent implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    
    public NpcDeleteEvent(Npc npc) {
        super(npc);
    }
    
    @Override
    public boolean isCancelled() {
        return super.cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        super.cancelled = cancel;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
