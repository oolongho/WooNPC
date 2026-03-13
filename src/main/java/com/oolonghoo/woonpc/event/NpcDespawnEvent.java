package com.oolonghoo.woonpc.event;

import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * NPC 消失事件
 * 在 NPC 对玩家不可见时触发
 * o
 */
public class NpcDespawnEvent extends NpcEvent {
    
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    
    public NpcDespawnEvent(Npc npc, Player player) {
        super(npc);
        this.player = player;
    }
    
    /**
     * 获取看不到 NPC 的玩家
     * 
     * @return 玩家
     */
    public Player getPlayer() {
        return player;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
