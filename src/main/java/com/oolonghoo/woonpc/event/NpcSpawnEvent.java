package com.oolonghoo.woonpc.event;

import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * NPC 生成事件
 * 在 NPC 对玩家可见时触发
 * o
 */
public class NpcSpawnEvent extends NpcEvent {
    
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    
    public NpcSpawnEvent(Npc npc, Player player) {
        super(npc);
        this.player = player;
    }
    
    /**
     * 获取看到 NPC 的玩家
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
