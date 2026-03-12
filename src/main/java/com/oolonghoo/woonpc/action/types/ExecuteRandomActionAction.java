package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.action.ActionExecutionContext;
import com.oolonghoo.woonpc.action.NpcAction;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

public class ExecuteRandomActionAction extends NpcAction {
    
    private static final Random random = new Random();
    
    public ExecuteRandomActionAction() {
        super("execute_random_action", false);
    }
    
    @Override
    public void execute(Player player, String value) {
    }
    
    @Override
    public void executeWithContext(ActionExecutionContext context, String value) {
        List<NpcAction.NpcActionData> actions = context.getActions();
        int currentIndex = context.getCurrentIndex();
        
        if (currentIndex >= actions.size() - 1) {
            return;
        }
        
        int remainingCount = actions.size() - currentIndex - 1;
        if (remainingCount <= 0) {
            return;
        }
        
        int randomOffset = random.nextInt(remainingCount) + 1;
        int randomIndex = currentIndex + randomOffset;
        
        NpcAction.NpcActionData randomAction = actions.get(randomIndex);
        try {
            randomAction.execute(context.getPlayer());
        } catch (Exception e) {
            WooNPC.getInstance().getLogger().warning(
                "[WooNPC] 执行随机动作失败：" + randomAction.action().getName() + " - " + e.getMessage()
            );
        }
        
        context.skipRemaining();
    }
    
    @Override
    public String getDescription() {
        return "从后续动作中随机选择一个执行";
    }
    
    @Override
    public String getValueHint() {
        return "无需参数";
    }
}
