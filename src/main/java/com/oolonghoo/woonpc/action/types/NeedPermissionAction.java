package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.action.ActionExecutionContext;
import com.oolonghoo.woonpc.action.NpcAction;
import org.bukkit.entity.Player;

public class NeedPermissionAction extends NpcAction {
    
    public NeedPermissionAction() {
        super("need_permission", true);
    }
    
    @Override
    public void execute(Player player, String value) {
    }
    
    @Override
    public void executeWithContext(ActionExecutionContext context, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        
        Player player = context.getPlayer();
        if (player == null) {
            context.terminate();
            return;
        }
        
        if (!player.hasPermission(value)) {
            context.terminate();
        }
    }
    
    @Override
    public String getDescription() {
        return "检查玩家权限，无权限则终止后续动作";
    }
    
    @Override
    public String getValueHint() {
        return "权限节点 (例如: woonpc.vip)";
    }
}
