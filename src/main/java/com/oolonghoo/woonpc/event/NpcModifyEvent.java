package com.oolonghoo.woonpc.event;

import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.event.HandlerList;

/**
 * NPC 属性修改事件
 * 在 NPC 属性被修改时触发
 * 
 * @author oolonghoo
 */
public class NpcModifyEvent extends NpcEvent {
    
    private static final HandlerList handlers = new HandlerList();
    private final String propertyName;
    private final Object oldValue;
    private final Object newValue;
    
    public NpcModifyEvent(Npc npc, String propertyName, Object oldValue, Object newValue) {
        super(npc);
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    /**
     * 获取被修改的属性名称
     * 
     * @return 属性名称
     */
    public String getPropertyName() {
        return propertyName;
    }
    
    /**
     * 获取旧值
     * 
     * @return 旧值
     */
    public Object getOldValue() {
        return oldValue;
    }
    
    /**
     * 获取新值
     * 
     * @return 新值
     */
    public Object getNewValue() {
        return newValue;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
