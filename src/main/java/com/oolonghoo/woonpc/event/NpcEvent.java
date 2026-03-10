package com.oolonghoo.woonpc.event;

import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * NPC 事件基类
 * 所有 NPC 相关事件的父类
 * 
 * @author oolonghoo
 */
public abstract class NpcEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    protected final Npc npc;
    protected boolean cancelled = false;
    
    public NpcEvent(Npc npc) {
        this.npc = npc;
    }
    
    public NpcEvent(Npc npc, boolean isAsync) {
        super(isAsync);
        this.npc = npc;
    }
    
    /**
     * 获取 NPC 对象
     * 
     * @return NPC 对象
     */
    public Npc getNpc() {
        return npc;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
