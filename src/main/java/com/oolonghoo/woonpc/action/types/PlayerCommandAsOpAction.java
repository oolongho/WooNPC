package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.action.NpcAction;
import com.oolonghoo.woonpc.util.CommandSafety;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 以 OP 权限执行玩家命令
 * 注意：此动作需要管理员谨慎配置
 * o
 */
public class PlayerCommandAsOpAction extends NpcAction {
    
    public PlayerCommandAsOpAction() {
        super("player_command_as_op", true);
    }
    
    @Override
    public void execute(Player player, String value) {
        if (value == null || value.isEmpty() || player == null) {
            return;
        }
        
        // 安全检查：即使是 OP 命令也要验证
        String sanitizedCommand = CommandSafety.sanitizeCommand(value);
        if (sanitizedCommand == null) {
            player.sendMessage("§c[系统] 该命令被禁止使用");
            return;
        }
        
        boolean wasOp = player.isOp();
        try {
            if (!wasOp) {
                player.setOp(true);
            }
            Bukkit.dispatchCommand(player, sanitizedCommand);
        } catch (IllegalStateException | IllegalArgumentException e) {
            WooNPC.getInstance().getLogger().warning(() ->
                "[WooNPC] 执行 OP 命令失败：" + sanitizedCommand + " - " + e.getMessage()
            );
        } finally {
            if (!wasOp) {
                player.setOp(false);
            }
        }
    }
    
    @Override
    public String getDescription() {
        return "以 OP 权限执行玩家命令（需要谨慎使用）";
    }
    
    @Override
    public String getValueHint() {
        return "命令 (例如：gamemode creative)";
    }
}
