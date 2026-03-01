package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.action.ActionExecutionContext;
import com.oolonghoo.woonpc.action.NpcAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WaitAction extends NpcAction {
    
    public WaitAction() {
        super("wait", true);
    }
    
    @Override
    public void execute(Player player, String value) {
    }
    
    @Override
    public void executeWithContext(ActionExecutionContext context, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        
        long ticks;
        try {
            ticks = Long.parseLong(value);
        } catch (NumberFormatException e) {
            return;
        }
        
        if (ticks <= 0) {
            return;
        }
        
        int nextIndex = context.getCurrentIndex() + 1;
        if (nextIndex >= context.getActions().size()) {
            return;
        }
        
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(WooNPC.class), () -> {
        }, ticks);
        
        context.skipRemaining();
    }
    
    @Override
    public String getDescription() {
        return "等待指定时间后继续执行后续动作";
    }
    
    @Override
    public String getValueHint() {
        return "tick数 (例如: 20 = 1秒)";
    }
}
