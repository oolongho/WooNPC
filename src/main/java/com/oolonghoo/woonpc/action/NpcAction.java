package com.oolonghoo.woonpc.action;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NPC 动作抽象基类
 * 定义 NPC 可以执行的动作
 * 
 * @author oolongho
 */
public abstract class NpcAction {
    
    /**
     * 动作名称
     */
    protected final String name;
    
    /**
     * 是否需要值参数
     */
    protected final boolean requiresValue;
    
    /**
     * 构造函数
     * 
     * @param name          动作名称
     * @param requiresValue 是否需要值参数
     */
    public NpcAction(@NotNull String name, boolean requiresValue) {
        this.name = name.toLowerCase();
        this.requiresValue = requiresValue;
    }
    
    /**
     * 执行动作
     * 
     * @param player 触发动作的玩家
     * @param value  动作参数值 (可为 null)
     */
    public abstract void execute(@NotNull Player player, @Nullable String value);
    
    /**
     * 获取动作名称
     * 
     * @return 动作名称
     */
    @NotNull
    public String getName() {
        return name;
    }
    
    /**
     * 检查是否需要值参数
     * 
     * @return 是否需要值参数
     */
    public boolean requiresValue() {
        return requiresValue;
    }
    
    /**
     * NPC 动作数据记录
     * 用于存储动作的执行顺序、动作对象和参数值
     */
    public record NpcActionData(int order, NpcAction action, String value) {
        
        /**
         * 执行动作
         * 
         * @param player 触发动作的玩家
         */
        public void execute(Player player) {
            action.execute(player, value);
        }
    }
}
