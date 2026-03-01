package com.oolonghoo.woonpc.action.types;

import com.oolonghoo.woonpc.action.NpcAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerCommandAsOpAction extends NpcAction {
    
    public PlayerCommandAsOpAction() {
        super("player_command_as_op", true);
    }
    
    @Override
    public void execute(Player player, String value) {
        if (value == null || value.isEmpty() || player == null) {
            return;
        }
        
        boolean wasOp = player.isOp();
        try {
            if (!wasOp) {
                player.setOp(true);
            }
            Bukkit.dispatchCommand(player, value);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (!wasOp) {
                player.setOp(false);
            }
        }
    }
    
    @Override
    public String getDescription() {
        return "以OP权限执行玩家命令";
    }
    
    @Override
    public String getValueHint() {
        return "命令 (例如: gamemode creative)";
    }
}
