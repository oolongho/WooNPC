package com.oolonghoo.woonpc.action;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NpcAction {
    
    protected final String name;
    protected final boolean requiresValue;
    
    public NpcAction(@NotNull String name, boolean requiresValue) {
        this.name = name.toLowerCase();
        this.requiresValue = requiresValue;
    }
    
    public abstract void execute(@NotNull Player player, @Nullable String value);
    
    public void executeWithContext(@NotNull ActionExecutionContext context, @Nullable String value) {
        execute(context.getPlayer(), value);
    }
    
    @NotNull
    public String getName() {
        return name;
    }
    
    public boolean requiresValue() {
        return requiresValue;
    }
    
    public String getDescription() {
        return "";
    }
    
    public String getValueHint() {
        return "";
    }
    
    public record NpcActionData(int order, NpcAction action, String value) {
        
        public void execute(Player player) {
            action.execute(player, value);
        }
        
        public void executeWithContext(ActionExecutionContext context) {
            action.executeWithContext(context, value);
        }
    }
}
