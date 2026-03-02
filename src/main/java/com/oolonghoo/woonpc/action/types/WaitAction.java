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
        
        // 标记跳过当前循环的剩余动作（等待后再执行）
        context.skipRemaining();
        
        // 延迟后继续执行后续动作
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(WooNPC.class), () -> {
            continueExecution(context, nextIndex);
        }, ticks);
    }
    
    private void continueExecution(ActionExecutionContext context, int startIndex) {
        // 重置上下文状态，确保延迟后的执行有干净的状态
        context.resetSkipRemaining();
        context.setJumpToIndex(-1);
        context.setCurrentIndex(startIndex);
        
        java.util.List<NpcAction.NpcActionData> actions = context.getActions();
        
        for (int i = startIndex; i < actions.size(); i++) {
            NpcAction.NpcActionData actionData = actions.get(i);
            if (actionData == null) {
                continue;
            }
            
            try {
                // 更新当前索引
                context.setCurrentIndex(i);
                
                actionData.executeWithContext(context);
                
                // 如果后续动作要求跳过剩余动作（如另一个 wait 或 execute_random_action）
                if (context.isSkipRemaining()) {
                    break;
                }
                
                // 如果后续动作要求跳转到指定索引
                if (context.getJumpToIndex() >= 0) {
                    i = context.getJumpToIndex() - 1; // -1 因为循环会 +1
                    context.setJumpToIndex(-1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
