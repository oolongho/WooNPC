package com.oolonghoo.woonpc.command;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.action.ActionManager;
import com.oolonghoo.woonpc.action.ActionTrigger;
import com.oolonghoo.woonpc.action.NpcAction;
import com.oolonghoo.woonpc.config.MessageManager;
import com.oolonghoo.woonpc.manager.NpcManager;
import com.oolonghoo.woonpc.npc.GlowingColor;
import com.oolonghoo.woonpc.npc.Npc;
import com.oolonghoo.woonpc.npc.NpcData;
import com.oolonghoo.woonpc.npc.NpcEquipmentSlot;
import com.oolonghoo.woonpc.npc.NpcImpl;
import com.oolonghoo.woonpc.npc.NpcPose;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 主命令处理器
 * 处理 /npc 命令及其子命令
 * 
 * @author oolongho
 */
public class MainCommand implements CommandExecutor, TabCompleter {

    private final WooNPC plugin;
    private final MessageManager msg;
    private final NpcManager npcManager;
    private final ActionManager actionManager;

    public MainCommand(WooNPC plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
        this.npcManager = plugin.getNpcManager();
        this.actionManager = plugin.getActionManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "skin":
                return handleSkin(sender, args);
            case "glowing":
                return handleGlowing(sender, args);
            case "hologram":
                return handleHologram(sender, args);
            case "look":
                return handleTurnToPlayer(sender, args);
            case "equip":
                return handleEquipment(sender, args);
            case "pose":
                return handlePose(sender, args);
            case "action":
                return handleAction(sender, args);
            case "movehere":
                return handleMoveHere(sender, args);
            case "moveto":
                return handleMoveTo(sender, args);
            case "copy":
                return handleCopy(sender, args);
            case "teleport":
                return handleTeleport(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "help":
                sendHelp(sender, label);
                return true;
            default:
                sender.sendMessage(msg.getWithPrefix("unknown-command", "command", label));
                return true;
        }
    }

    /**
     * 创建NPC
     * 用法: /npc create <名称> [--type <类型>]
     */
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.create")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.getWithPrefix("player-only"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(msg.getWithPrefix("help.create"));
            return true;
        }

        Player player = (Player) sender;
        String name = args[1];

        // 检查名称是否已存在
        if (npcManager.exists(name)) {
            sender.sendMessage(msg.getWithPrefix("npc.already-exists", "name", name));
            return true;
        }

        // 解析 --type 参数
        EntityType entityType = EntityType.PLAYER;
        for (int i = 2; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("--type")) {
                try {
                    entityType = EntityType.valueOf(args[i + 1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(msg.getWithPrefix("npc.invalid-type", "type", args[i + 1]));
                    return true;
                }
            }
        }

        // 创建 NPC 数据
        NpcData data = new NpcData(name, player.getUniqueId(), player.getLocation());
        data.setType(entityType);

        // 通过 NpcManager 创建 NPC
        Npc npc = npcManager.createNpc(name, player.getLocation());
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc.limit-reached"));
            return true;
        }
        
        // 设置类型
        npc.getData().setType(entityType);
        
        // 生成给所有玩家
        npc.spawnForAll();

        sender.sendMessage(msg.getWithPrefix("npc.created", "name", name));
        return true;
    }

    /**
     * 删除NPC
     * 用法: /npc remove <名称>
     */
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.remove")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(msg.getWithPrefix("help.remove"));
            return true;
        }

        String name = args[1];
        if (!npcManager.removeNpc(name)) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        sender.sendMessage(msg.getWithPrefix("npc.removed", "name", name));
        return true;
    }

    /**
     * 列出所有NPC
     * 用法: /npc list
     */
    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.use")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        sender.sendMessage(msg.get("npc.list.header"));

        if (npcManager.getNpcCount() == 0) {
            sender.sendMessage(msg.get("npc.list.empty"));
            return true;
        }

        int index = 1;
        for (Npc npc : npcManager.getAllNpcs()) {
            Location loc = npc.getLocation();
            String worldName = loc != null && loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
            sender.sendMessage(msg.get("npc.list.format",
                "index", String.valueOf(index++),
                "name", npc.getName(),
                "world", worldName
            ));
        }

        sender.sendMessage(msg.get("npc.list.footer", "count", String.valueOf(npcManager.getNpcCount())));
        return true;
    }

    /**
     * 查看NPC信息
     * 用法: /npc info <名称>
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.use")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(msg.getWithPrefix("help.info"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        // 显示NPC信息
        sender.sendMessage(msg.get("npc.info.header", "name", npc.getName()));
        sender.sendMessage(msg.get("npc.info.id", "id", npc.getData().getId().toString()));
        
        Location loc = npc.getLocation();
        sender.sendMessage(msg.get("npc.info.location",
            "world", loc != null && loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
            "x", loc != null ? String.format("%.2f", loc.getX()) : "0",
            "y", loc != null ? String.format("%.2f", loc.getY()) : "0",
            "z", loc != null ? String.format("%.2f", loc.getZ()) : "0"
        ));
        
        sender.sendMessage(msg.get("npc.info.skin", "skin", npc.getData().getSkinName() != null ? npc.getData().getSkinName() : "default"));
        sender.sendMessage(msg.get("npc.info.glowing", "glowing", npc.getData().isGlowing() ? npc.getData().getGlowingColor().getConfigName() : "false"));
        sender.sendMessage(msg.get("npc.info.turn-to-player", "turn_to_player", String.valueOf(npc.getData().isTurnToPlayer())));
        sender.sendMessage(msg.get("npc.info.show-in-tab", "show_in_tab", String.valueOf(npc.getData().isShowInTab())));
        sender.sendMessage(msg.get("npc.info.hologram", "hologram_lines", String.join(", ", npc.getData().getHologramLines())));
        sender.sendMessage(msg.get("npc.info.footer"));

        return true;
    }

    /**
     * 设置皮肤
     * 用法: /npc skin <名称> <皮肤名/@mirror>
     */
    private boolean handleSkin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.skin")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.getWithPrefix("help.skin"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        String skin = args[2];
        if (skin.equalsIgnoreCase("@mirror")) {
            npc.getData().setSkinMirror(true);
            sender.sendMessage(msg.getWithPrefix("skin.set-mirror", "name", name));
            npc.updateForAll();
            npcManager.saveNpcs();
        } else {
            sender.sendMessage(msg.get("skin.fetching", "skin", skin));
            npc.getData().setSkinMirror(false);
            
            plugin.getSkinManager().getSkinAsync(skin).thenAccept(skinData -> {
                if (skinData == null) {
                    sender.sendMessage(msg.getWithPrefix("skin.not-found", "skin", skin));
                    return;
                }
                
                npc.getData().setSkinName(skin);
                npc.getData().setSkin(skinData.getTextureValue(), skinData.getTextureSignature());
                npc.updateForAll();
                npcManager.saveNpcs();
                
                sender.sendMessage(msg.getWithPrefix("skin.set", "name", name, "skin", skin));
            });
        }

        return true;
    }

    /**
     * 设置发光效果
     * 用法: /npc glowing <名称> <true/false> [颜色]
     */
    private boolean handleGlowing(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.glowing")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.getWithPrefix("help.glowing"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        boolean enabled = Boolean.parseBoolean(args[2]);
        String colorName = args.length > 3 ? args[3].toUpperCase() : "WHITE";
        
        GlowingColor color = GlowingColor.fromConfigName(colorName);

        npc.getData().setGlowing(enabled);
        if (enabled) {
            npc.getData().setGlowingColor(color);
            sender.sendMessage(msg.getWithPrefix("glowing.enabled", "name", name, "color", color.getConfigName()));
        } else {
            sender.sendMessage(msg.getWithPrefix("glowing.disabled", "name", name));
        }

        npc.updateForAll();
        npcManager.saveNpcs();
        return true;
    }

    /**
     * 处理全息文字
     * 用法: /npc hologram <名称> <add/delete/set/clear/list> [内容/行号]
     */
    private boolean handleHologram(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.hologram")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.getWithPrefix("help.hologram"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        String action = args[2].toLowerCase();

        switch (action) {
            case "add":
                if (args.length < 4) {
                    sender.sendMessage(msg.getWithPrefix("help.hologram"));
                    return true;
                }
                String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                npc.getData().addHologramLine(text);
                sender.sendMessage(msg.getWithPrefix("hologram.added", "text", text));
                break;

            case "delete":
                if (args.length < 4) {
                    sender.sendMessage(msg.getWithPrefix("help.hologram"));
                    return true;
                }
                try {
                    int line = Integer.parseInt(args[3]) - 1;
                    List<String> lines = npc.getData().getHologramLines();
                    if (line < 0 || line >= lines.size()) {
                        sender.sendMessage(msg.getWithPrefix("hologram.invalid-line"));
                        return true;
                    }
                    npc.getData().removeHologramLine(line);
                    sender.sendMessage(msg.getWithPrefix("hologram.removed", "line", String.valueOf(line + 1)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(msg.getWithPrefix("invalid-number"));
                }
                break;

            case "set":
                if (args.length < 5) {
                    sender.sendMessage(msg.getWithPrefix("help.hologram"));
                    return true;
                }
                try {
                    int line = Integer.parseInt(args[3]) - 1;
                    String newText = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                    npc.getData().setHologramLine(line, newText);
                    sender.sendMessage(msg.getWithPrefix("hologram.set", "line", String.valueOf(line + 1), "text", newText));
                } catch (NumberFormatException e) {
                    sender.sendMessage(msg.getWithPrefix("invalid-number"));
                }
                break;

            case "clear":
                npc.getData().getHologramLines().clear();
                sender.sendMessage(msg.getWithPrefix("hologram.cleared"));
                break;

            case "list":
                sender.sendMessage(msg.get("hologram.list.header"));
                List<String> lines = npc.getData().getHologramLines();
                if (lines.isEmpty()) {
                    sender.sendMessage(msg.get("hologram.list.empty"));
                } else {
                    for (int i = 0; i < lines.size(); i++) {
                        sender.sendMessage(msg.get("hologram.list.format", "index", String.valueOf(i + 1), "text", lines.get(i)));
                    }
                }
                break;

            default:
                sender.sendMessage(msg.getWithPrefix("help.hologram"));
        }

        npc.updateForAll();
        npcManager.saveNpcs();
        return true;
    }

    /**
     * 设置头部追踪
     * 用法: /npc turn_to_player <名称> <true/false>
     */
    private boolean handleTurnToPlayer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.edit")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.getWithPrefix("help.turn"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        boolean enabled = Boolean.parseBoolean(args[2]);
        npc.getData().setTurnToPlayer(enabled);
        
        if (enabled) {
            sender.sendMessage(msg.getWithPrefix("turn-to-player.enabled", "name", name));
        } else {
            sender.sendMessage(msg.getWithPrefix("turn-to-player.disabled", "name", name));
        }

        npcManager.saveNpcs();
        return true;
    }

    /**
     * 设置装备
     * 用法: /npc equipment <名称> <槽位> [@hand]
     */
    private boolean handleEquipment(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.equipment")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.getWithPrefix("player-only"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.getWithPrefix("help.equipment"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        String slotName = args[2].toUpperCase();
        NpcEquipmentSlot slot = NpcEquipmentSlot.fromConfigName(slotName);
        if (slot == null) {
            // 尝试其他格式
            for (NpcEquipmentSlot s : NpcEquipmentSlot.values()) {
                if (s.name().equalsIgnoreCase(slotName) || s.getNmsName().equalsIgnoreCase(slotName)) {
                    slot = s;
                    break;
                }
            }
        }
        
        if (slot == null) {
            sender.sendMessage(msg.getWithPrefix("equipment.invalid-slot", 
                "slots", Arrays.stream(NpcEquipmentSlot.values())
                    .map(NpcEquipmentSlot::getConfigName)
                    .collect(Collectors.joining(", "))));
            return true;
        }

        Player player = (Player) sender;

        // 检查是否使用 @hand 参数
        if (args.length >= 4 && args[3].equalsIgnoreCase("@hand")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                npc.getData().removeEquipment(slot);
                sender.sendMessage(msg.getWithPrefix("equipment.cleared", "slot", slot.getConfigName()));
            } else {
                npc.getData().addEquipment(slot, item.clone());
                sender.sendMessage(msg.getWithPrefix("equipment.set", "slot", slot.getConfigName()));
            }
        } else {
            // 使用手中物品
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                npc.getData().removeEquipment(slot);
                sender.sendMessage(msg.getWithPrefix("equipment.cleared", "slot", slot.getConfigName()));
            } else {
                npc.getData().addEquipment(slot, item.clone());
                sender.sendMessage(msg.getWithPrefix("equipment.set", "slot", slot.getConfigName()));
            }
        }

        npc.updateForAll();
        npcManager.saveNpcs();
        return true;
    }

    /**
     * 设置姿势
     * 用法: /npc pose <名称> <姿势>
     */
    private boolean handlePose(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.pose")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.getWithPrefix("help.pose"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        String poseName = args[2];
        NpcPose pose = NpcPose.getByName(poseName);
        if (pose == null) {
            sender.sendMessage(msg.getWithPrefix("pose.invalid", "pose", poseName));
            return true;
        }
        
        // 调用NpcImpl的setPose方法
        if (npc instanceof NpcImpl npcImpl) {
            npcImpl.setPose(pose);
        } else {
            npc.getData().setPose(pose.getConfigName());
            npc.updateForAll();
        }
        
        sender.sendMessage(msg.getWithPrefix("pose.set", "name", name, "pose", pose.getConfigName()));
        npcManager.saveNpcs();
        return true;
    }

    /**
     * 处理动作
     * 用法: /npc action <名称> <触发器> <add/delete/list/clear> [动作类型] [值]
     */
    private boolean handleAction(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.action")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(msg.getWithPrefix("help.action"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        String triggerName = args[2].toUpperCase();
        ActionTrigger trigger = ActionTrigger.getByName(triggerName);
        if (trigger == null) {
            sender.sendMessage(msg.getWithPrefix("action.invalid-trigger", 
                "triggers", Arrays.stream(ActionTrigger.values())
                    .map(ActionTrigger::name)
                    .collect(Collectors.joining(", "))));
            return true;
        }

        String action = args[3].toLowerCase();
        String npcId = npc.getData().getId();

        switch (action) {
            case "add":
                if (args.length < 5) {
                    sender.sendMessage(msg.getWithPrefix("help.action"));
                    return true;
                }
                
                String actionType = args[4].toLowerCase();
                NpcAction npcAction = actionManager.getAction(actionType);
                if (npcAction == null) {
                    sender.sendMessage(msg.getWithPrefix("action.invalid-type", 
                        "types", actionManager.getAllActions().stream()
                            .map(NpcAction::getName)
                            .collect(Collectors.joining(", "))));
                    return true;
                }
                
                // 检查是否需要值参数
                String value = null;
                if (args.length >= 6) {
                    value = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
                }
                
                if (npcAction.requiresValue() && (value == null || value.isEmpty())) {
                    sender.sendMessage(msg.getWithPrefix("action.requires-value"));
                    return true;
                }
                
                // 获取当前动作数量
                List<NpcAction.NpcActionData> currentActions = npc.getData().getActions(trigger);
                int order = currentActions.size() + 1;
                
                // 添加动作
                NpcAction.NpcActionData actionData = new NpcAction.NpcActionData(order, npcAction, value);
                npc.getData().addAction(trigger, actionData);
                actionManager.addNpcAction(npcId, trigger, actionData);
                
                sender.sendMessage(msg.getWithPrefix("action.added", 
                    "type", actionType, 
                    "order", String.valueOf(order)));
                break;

            case "delete":
                if (args.length < 5) {
                    sender.sendMessage(msg.getWithPrefix("help.action"));
                    return true;
                }
                
                try {
                    int index = Integer.parseInt(args[4]) - 1;
                    List<NpcAction.NpcActionData> actions = npc.getData().getActions(trigger);
                    if (index < 0 || index >= actions.size()) {
                        sender.sendMessage(msg.getWithPrefix("action.invalid-index"));
                        return true;
                    }
                    
                    // 移除并重新排序
                    actions.remove(index);
                    List<NpcAction.NpcActionData> reorderedActions = reorderActions(actions);
                    npc.getData().setActions(trigger, reorderedActions);
                    actionManager.setNpcActions(npcId, trigger, reorderedActions);
                    
                    sender.sendMessage(msg.getWithPrefix("action.removed", "index", String.valueOf(index + 1)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(msg.getWithPrefix("invalid-number"));
                }
                break;

            case "clear":
                npc.getData().clearActions(trigger);
                actionManager.clearNpcActions(npcId, trigger);
                sender.sendMessage(msg.getWithPrefix("action.cleared"));
                break;

            case "list":
                List<NpcAction.NpcActionData> actions = npc.getData().getActions(trigger);
                if (actions.isEmpty()) {
                    sender.sendMessage(msg.get("action.list.empty"));
                    return true;
                }
                
                sender.sendMessage(msg.get("action.list.header", "trigger", trigger.name()));
                for (int i = 0; i < actions.size(); i++) {
                    NpcAction.NpcActionData ad = actions.get(i);
                    sender.sendMessage(msg.get("action.list.format",
                        "index", String.valueOf(i + 1),
                        "type", ad.action().getName(),
                        "value", ad.value() != null ? ad.value() : ""
                    ));
                }
                break;

            default:
                sender.sendMessage(msg.getWithPrefix("help.action"));
        }

        npcManager.saveNpcs();
        return true;
    }

    /**
     * 重新排序动作列表
     */
    private List<NpcAction.NpcActionData> reorderActions(List<NpcAction.NpcActionData> actions) {
        List<NpcAction.NpcActionData> newActions = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            NpcAction.NpcActionData a = actions.get(i);
            newActions.add(new NpcAction.NpcActionData(i + 1, a.action(), a.value()));
        }
        return newActions;
    }

    /**
     * 移动NPC到当前位置
     * 用法: /npc movehere <名称>
     */
    private boolean handleMoveHere(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.edit")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.getWithPrefix("player-only"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(msg.getWithPrefix("help.move"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        Player player = (Player) sender;
        npc.getData().setLocation(player.getLocation());
        npc.moveForAll();
        
        sender.sendMessage(msg.getWithPrefix("move.here", "name", name));

        npcManager.saveNpcs();
        return true;
    }

    /**
     * 移动NPC到指定位置
     * 用法: /npc moveto <名称> <x> <y> <z> [yaw] [pitch]
     */
    private boolean handleMoveTo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.edit")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (args.length < 5) {
            sender.sendMessage(msg.getWithPrefix("help.move"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        Location currentLoc = npc.getLocation();
        if (currentLoc == null || currentLoc.getWorld() == null) {
            sender.sendMessage(msg.getWithPrefix("npc.invalid-location"));
            return true;
        }

        try {
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]);
            float yaw = args.length > 5 ? Float.parseFloat(args[5]) : currentLoc.getYaw();
            float pitch = args.length > 6 ? Float.parseFloat(args[6]) : currentLoc.getPitch();

            Location newLoc = new Location(currentLoc.getWorld(), x, y, z, yaw, pitch);
            npc.getData().setLocation(newLoc);
            npc.moveForAll();
            
            sender.sendMessage(msg.getWithPrefix("move.to", "name", name));
            npcManager.saveNpcs();
        } catch (NumberFormatException e) {
            sender.sendMessage(msg.getWithPrefix("invalid-number"));
        }

        return true;
    }

    /**
     * 复制NPC
     * 用法: /npc copy <源名称> <目标名称>
     */
    private boolean handleCopy(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.create")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.getWithPrefix("help.copy"));
            return true;
        }

        String sourceName = args[1];
        String targetName = args[2];

        Npc source = npcManager.getNpc(sourceName);
        if (source == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", sourceName));
            return true;
        }

        if (npcManager.exists(targetName)) {
            sender.sendMessage(msg.getWithPrefix("npc.already-exists", "name", targetName));
            return true;
        }

        NpcData sourceData = source.getData();
        Npc target = npcManager.createNpc(targetName, sourceData.getLocation());
        if (target == null) {
            sender.sendMessage(msg.getWithPrefix("npc.limit-reached"));
            return true;
        }
        
        // 复制皮肤数据
        if (sourceData.getSkinValue() != null && !sourceData.getSkinValue().isEmpty()) {
            target.getData().setSkin(sourceData.getSkinValue(), sourceData.getSkinSignature());
        }
        
        // 复制其他属性
        target.getData().setSkinName(sourceData.getSkinName());
        target.getData().setSkinMirror(sourceData.isSkinMirror());
        // 显示名称使用新名称，不复制源NPC的显示名称
        target.getData().setDisplayName(targetName);
        target.getData().setShowInTab(sourceData.isShowInTab());
        target.getData().setTurnToPlayer(sourceData.isTurnToPlayer());
        target.getData().setGlowing(sourceData.isGlowing());
        target.getData().setGlowingColor(sourceData.getGlowingColor());
        target.getData().setPose(sourceData.getPose());
        target.getData().setScale(sourceData.getScale());
        target.getData().setType(sourceData.getType());

        target.spawnForAll();
        npcManager.saveNpcs();

        sender.sendMessage(msg.getWithPrefix("copy.success", "source", sourceName, "target", targetName));
        return true;
    }

    /**
     * 传送到NPC
     * 用法: /npc tp <名称>
     */
    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.use")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.getWithPrefix("player-only"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(msg.getWithPrefix("help.teleport"));
            return true;
        }

        String name = args[1];
        Npc npc = npcManager.getNpc(name);
        if (npc == null) {
            sender.sendMessage(msg.getWithPrefix("npc-not-found", "npc", name));
            return true;
        }

        Player player = (Player) sender;
        Location loc = npc.getLocation();
        if (loc == null || loc.getWorld() == null) {
            sender.sendMessage(msg.getWithPrefix("npc.invalid-location"));
            return true;
        }
        
        player.teleport(loc);
        sender.sendMessage(msg.getWithPrefix("teleport.success", "name", name));
        return true;
    }

    /**
     * 重载配置
     * 用法: /npc reload
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("woonpc.reload")) {
            sender.sendMessage(msg.getWithPrefix("no-permission"));
            return true;
        }

        plugin.reload();
        sender.sendMessage(msg.getWithPrefix("reload-success"));
        return true;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(msg.get("help.header", "command", label));
        
        if (sender.hasPermission("woonpc.create")) {
            sender.sendMessage(msg.get("help.create", "command", label));
        }
        if (sender.hasPermission("woonpc.remove")) {
            sender.sendMessage(msg.get("help.remove", "command", label));
        }
        if (sender.hasPermission("woonpc.skin")) {
            sender.sendMessage(msg.get("help.skin", "command", label));
        }
        if (sender.hasPermission("woonpc.glowing")) {
            sender.sendMessage(msg.get("help.glowing", "command", label));
        }
        if (sender.hasPermission("woonpc.hologram")) {
            sender.sendMessage(msg.get("help.hologram", "command", label));
        }
        if (sender.hasPermission("woonpc.edit")) {
            sender.sendMessage(msg.get("help.look", "command", label));
        }
        if (sender.hasPermission("woonpc.equipment")) {
            sender.sendMessage(msg.get("help.equipment", "command", label));
        }
        if (sender.hasPermission("woonpc.pose")) {
            sender.sendMessage(msg.get("help.pose", "command", label));
        }
        if (sender.hasPermission("woonpc.action")) {
            sender.sendMessage(msg.get("help.action", "command", label));
        }
        if (sender.hasPermission("woonpc.edit")) {
            sender.sendMessage(msg.get("help.move", "command", label));
            sender.sendMessage(msg.get("help.copy", "command", label));
        }
        if (sender.hasPermission("woonpc.use")) {
            sender.sendMessage(msg.get("help.teleport", "command", label));
            sender.sendMessage(msg.get("help.list", "command", label));
            sender.sendMessage(msg.get("help.info", "command", label));
        }
        if (sender.hasPermission("woonpc.reload")) {
            sender.sendMessage(msg.get("help.reload", "command", label));
        }
        
        sender.sendMessage(msg.get("help.footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 子命令补全
            List<String> subCommands = getSubCommandsForSender(sender);
            String prefix = args[0].toLowerCase();
            completions.addAll(subCommands.stream()
                .filter(cmd -> cmd.startsWith(prefix))
                .collect(Collectors.toList()));
        } else if (args.length == 2) {
            // NPC名称补全或特殊参数
            String subCmd = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            
            // create 不需要补全现有NPC名称
            if (!subCmd.equals("create")) {
                completions.addAll(npcManager.getAllNpcs().stream()
                    .map(Npc::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList()));
            }
        } else if (args.length >= 3) {
            // 特定子命令的参数补全
            String subCmd = args[0].toLowerCase();
            String prefix = args[args.length - 1].toLowerCase();

            switch (subCmd) {
                case "create":
                    if (args.length == 3) {
                        // 提供 --type 选项
                        completions.add("--type");
                    } else if (args.length == 4 && args[2].equalsIgnoreCase("--type")) {
                        // 实体类型补全
                        for (EntityType type : EntityType.values()) {
                            if (type.name().toLowerCase().startsWith(prefix)) {
                                completions.add(type.name());
                            }
                        }
                    }
                    break;

                case "skin":
                    if (args.length == 3) {
                        completions.add("@mirror");
                        // 添加在线玩家名
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            if (online.getName().toLowerCase().startsWith(prefix)) {
                                completions.add(online.getName());
                            }
                        }
                    }
                    break;

                case "glowing":
                    if (args.length == 3) {
                        completions.addAll(Arrays.asList("true", "false"));
                    } else if (args.length == 4) {
                        for (GlowingColor color : GlowingColor.values()) {
                            if (color.getConfigName().toLowerCase().startsWith(prefix)) {
                                completions.add(color.getConfigName());
                            }
                        }
                    }
                    break;

                case "hologram":
                    if (args.length == 3) {
                        completions.addAll(Arrays.asList("add", "delete", "set", "clear", "list"));
                    }
                    break;

                case "look":
                    completions.addAll(Arrays.asList("true", "false"));
                    break;

                case "equip":
                    if (args.length == 3) {
                        for (NpcEquipmentSlot slot : NpcEquipmentSlot.values()) {
                            if (slot.getConfigName().toLowerCase().startsWith(prefix)) {
                                completions.add(slot.getConfigName());
                            }
                        }
                    } else if (args.length == 4) {
                        completions.add("@hand");
                    }
                    break;

                case "pose":
                    if (args.length == 3) {
                        for (NpcPose pose : NpcPose.values()) {
                            if (pose.getConfigName().toLowerCase().startsWith(prefix)) {
                                completions.add(pose.getConfigName());
                            }
                        }
                    }
                    break;

                case "action":
                    if (args.length == 3) {
                        // 触发器补全
                        for (ActionTrigger trigger : ActionTrigger.values()) {
                            if (trigger.name().toLowerCase().startsWith(prefix)) {
                                completions.add(trigger.name());
                            }
                        }
                    } else if (args.length == 4) {
                        completions.addAll(Arrays.asList("add", "delete", "list", "clear"));
                    } else if (args.length == 5 && args[3].equalsIgnoreCase("add")) {
                        // 动作类型补全
                        for (NpcAction action : actionManager.getAllActions()) {
                            if (action.getName().toLowerCase().startsWith(prefix)) {
                                completions.add(action.getName());
                            }
                        }
                    } else if (args.length == 5 && args[3].equalsIgnoreCase("delete")) {
                        // 动作索引补全
                        Npc npc = npcManager.getNpc(args[1]);
                        if (npc != null) {
                            ActionTrigger trigger = ActionTrigger.getByName(args[2]);
                            if (trigger != null) {
                                List<NpcAction.NpcActionData> actions = npc.getData().getActions(trigger);
                                for (int i = 1; i <= actions.size(); i++) {
                                    completions.add(String.valueOf(i));
                                }
                            }
                        }
                    }
                    break;

                case "copy":
                    // 第二个参数是新NPC名称，不需要补全
                    break;
            }
        }

        return completions;
    }

    /**
     * 获取发送者可用的子命令
     */
    private List<String> getSubCommandsForSender(CommandSender sender) {
        List<String> commands = new ArrayList<>();
        
        if (sender.hasPermission("woonpc.create")) {
            commands.add("create");
        }
        if (sender.hasPermission("woonpc.remove")) {
            commands.add("delete");
        }
        if (sender.hasPermission("woonpc.use")) {
            commands.add("list");
            commands.add("info");
            commands.add("teleport");
        }
        if (sender.hasPermission("woonpc.skin")) {
            commands.add("skin");
        }
        if (sender.hasPermission("woonpc.glowing")) {
            commands.add("glowing");
        }
        if (sender.hasPermission("woonpc.hologram")) {
            commands.add("hologram");
        }
        if (sender.hasPermission("woonpc.edit")) {
            commands.add("look");
            commands.add("movehere");
            commands.add("moveto");
            commands.add("copy");
        }
        if (sender.hasPermission("woonpc.equipment")) {
            commands.add("equip");
        }
        if (sender.hasPermission("woonpc.pose")) {
            commands.add("pose");
        }
        if (sender.hasPermission("woonpc.action")) {
            commands.add("action");
        }
        if (sender.hasPermission("woonpc.reload")) {
            commands.add("reload");
        }
        
        commands.add("help");
        
        return commands;
    }
}
