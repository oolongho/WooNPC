package com.oolonghoo.woonpc.npc;

import com.oolonghoo.woonpc.action.ActionTrigger;
import com.oolonghoo.woonpc.action.NpcAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * NPC 数据模型
 * 存储 NPC 的所有属性信息
 * 
 * @author oolongho
 */
public class NpcData {
    
    // 基础信息
    private final String id;
    private final String name;
    private final UUID creator;
    
    // 显示信息
    private String displayName;
    private String skinValue;
    private String skinSignature;
    private boolean skinMirror;
    private String skinName;
    
    // 位置信息
    private Location location;
    
    // 状态信息
    private boolean showInTab;
    private boolean glowing;
    private GlowingColor glowingColor;
    private EntityType type;
    private boolean collidable;
    
    // 装备
    private Map<NpcEquipmentSlot, ItemStack> equipment;
    
    // 交互
    private boolean turnToPlayer;
    private int turnToPlayerDistance;
    private int visibilityDistance;
    private float interactionCooldown;
    
    // 全息文字
    private List<String> hologramLines;
    
    // 姿势
    private String pose;
    
    // 缩放
    private float scale;
    
    // 实体效果
    private Set<NpcEffect> effects;
    
    // 动作 (触发器 -> 动作列表)
    private Map<ActionTrigger, List<NpcAction.NpcActionData>> actions;
    
    // 脏标记 (用于判断是否需要更新)
    private volatile boolean dirty;
    
    /**
     * 创建默认 NPC 数据
     * 
     * @param name     NPC 名称
     * @param creator  创建者 UUID
     * @param location NPC 位置
     */
    public NpcData(String name, UUID creator, Location location) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.creator = creator;
        this.location = location != null ? location.clone() : null;
        this.displayName = name;
        this.type = EntityType.PLAYER;
        this.showInTab = false;
        this.glowing = false;
        this.glowingColor = GlowingColor.WHITE;
        this.turnToPlayer = false;
        this.turnToPlayerDistance = -1; // 使用配置默认值
        this.visibilityDistance = -1;   // 使用配置默认值
        this.interactionCooldown = 0;
        this.scale = 1.0f;
        this.skinMirror = false;
        this.collidable = false;
        this.equipment = new ConcurrentHashMap<>();
        this.hologramLines = new ArrayList<>();
        this.pose = "STANDING";
        this.effects = ConcurrentHashMap.newKeySet();
        this.actions = new ConcurrentHashMap<>();
        this.dirty = true;
    }
    
    /**
     * 完整构造函数
     */
    public NpcData(String id, String name, UUID creator, String displayName,
                   String skinValue, String skinSignature, boolean skinMirror,
                   Location location, boolean showInTab, boolean glowing,
                   GlowingColor glowingColor, EntityType type,
                   Map<NpcEquipmentSlot, ItemStack> equipment,
                   boolean turnToPlayer, int turnToPlayerDistance,
                   int visibilityDistance, float interactionCooldown,
                   List<String> hologramLines, String pose, float scale,
                   boolean collidable, String skinName) {
        this.id = id;
        this.name = name;
        this.creator = creator;
        this.displayName = displayName;
        this.skinValue = skinValue;
        this.skinSignature = skinSignature;
        this.skinMirror = skinMirror;
        this.skinName = skinName;
        this.location = location != null ? location.clone() : null;
        this.showInTab = showInTab;
        this.glowing = glowing;
        this.glowingColor = glowingColor;
        this.type = type;
        this.collidable = collidable;
        this.equipment = equipment != null ? new ConcurrentHashMap<>(equipment) : new ConcurrentHashMap<>();
        this.turnToPlayer = turnToPlayer;
        this.turnToPlayerDistance = turnToPlayerDistance;
        this.visibilityDistance = visibilityDistance;
        this.interactionCooldown = interactionCooldown;
        this.hologramLines = hologramLines != null ? new ArrayList<>(hologramLines) : new ArrayList<>();
        this.pose = pose;
        this.scale = scale;
        this.effects = ConcurrentHashMap.newKeySet();
        this.actions = new ConcurrentHashMap<>();
        this.dirty = true;
    }
    
    // ==================== Getters ====================
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public UUID getCreator() {
        return creator != null ? creator : UUID.fromString("00000000-0000-0000-0000-000000000000");
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getSkinValue() {
        return skinValue;
    }
    
    public String getSkinSignature() {
        return skinSignature;
    }
    
    public boolean isSkinMirror() {
        return skinMirror;
    }
    
    public String getSkinName() {
        return skinName;
    }
    
    public Location getLocation() {
        return location != null ? location.clone() : null;
    }
    
    public boolean isShowInTab() {
        return showInTab;
    }
    
    public boolean isGlowing() {
        return glowing;
    }
    
    public GlowingColor getGlowingColor() {
        return glowingColor != null ? glowingColor : GlowingColor.WHITE;
    }
    
    public EntityType getType() {
        return type != null ? type : EntityType.PLAYER;
    }
    
    public boolean isCollidable() {
        return collidable;
    }
    
    public Map<NpcEquipmentSlot, ItemStack> getEquipment() {
        return equipment;
    }
    
    public boolean isTurnToPlayer() {
        return turnToPlayer;
    }
    
    public int getTurnToPlayerDistance() {
        return turnToPlayerDistance;
    }
    
    public int getVisibilityDistance() {
        return visibilityDistance;
    }
    
    public float getInteractionCooldown() {
        return interactionCooldown;
    }
    
    public List<String> getHologramLines() {
        return hologramLines;
    }
    
    public String getPose() {
        return pose;
    }
    
    public float getScale() {
        return scale;
    }
    
    public Map<ActionTrigger, List<NpcAction.NpcActionData>> getActions() {
        return actions;
    }
    
    public List<NpcAction.NpcActionData> getActions(ActionTrigger trigger) {
        return actions.getOrDefault(trigger, Collections.emptyList());
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    // ==================== Setters ====================
    
    public NpcData setDisplayName(String displayName) {
        this.displayName = displayName;
        this.dirty = true;
        return this;
    }
    
    public NpcData setSkin(String value, String signature) {
        this.skinValue = value;
        this.skinSignature = signature;
        this.dirty = true;
        return this;
    }
    
    public NpcData setSkinMirror(boolean skinMirror) {
        this.skinMirror = skinMirror;
        this.dirty = true;
        return this;
    }
    
    public NpcData setSkinName(String skinName) {
        this.skinName = skinName;
        this.dirty = true;
        return this;
    }
    
    public NpcData setLocation(Location location) {
        this.location = location != null ? location.clone() : null;
        this.dirty = true;
        return this;
    }
    
    public NpcData setShowInTab(boolean showInTab) {
        this.showInTab = showInTab;
        this.dirty = true;
        return this;
    }
    
    public NpcData setGlowing(boolean glowing) {
        this.glowing = glowing;
        this.dirty = true;
        return this;
    }
    
    public NpcData setGlowingColor(GlowingColor glowingColor) {
        this.glowingColor = glowingColor;
        this.dirty = true;
        return this;
    }
    
    public NpcData setType(EntityType type) {
        this.type = type;
        this.dirty = true;
        return this;
    }
    
    public NpcData setCollidable(boolean collidable) {
        this.collidable = collidable;
        this.dirty = true;
        return this;
    }
    
    public NpcData setEquipment(Map<NpcEquipmentSlot, ItemStack> equipment) {
        this.equipment = equipment != null ? new ConcurrentHashMap<>(equipment) : new ConcurrentHashMap<>();
        this.dirty = true;
        return this;
    }
    
    public NpcData addEquipment(NpcEquipmentSlot slot, ItemStack item) {
        this.equipment.put(slot, item);
        this.dirty = true;
        return this;
    }
    
    public NpcData removeEquipment(NpcEquipmentSlot slot) {
        this.equipment.remove(slot);
        this.dirty = true;
        return this;
    }
    
    public NpcData setTurnToPlayer(boolean turnToPlayer) {
        this.turnToPlayer = turnToPlayer;
        this.dirty = true;
        return this;
    }
    
    public NpcData setTurnToPlayerDistance(int distance) {
        this.turnToPlayerDistance = distance;
        this.dirty = true;
        return this;
    }
    
    public NpcData setVisibilityDistance(int distance) {
        this.visibilityDistance = distance;
        this.dirty = true;
        return this;
    }
    
    public NpcData setInteractionCooldown(float cooldown) {
        this.interactionCooldown = cooldown;
        this.dirty = true;
        return this;
    }
    
    public NpcData setHologramLines(List<String> lines) {
        this.hologramLines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
        this.dirty = true;
        return this;
    }
    
    public NpcData addHologramLine(String line) {
        this.hologramLines.add(line);
        this.dirty = true;
        return this;
    }
    
    public NpcData removeHologramLine(int index) {
        if (index >= 0 && index < this.hologramLines.size()) {
            this.hologramLines.remove(index);
            this.dirty = true;
        }
        return this;
    }
    
    public NpcData setHologramLine(int index, String line) {
        if (index >= 0 && index < this.hologramLines.size()) {
            this.hologramLines.set(index, line);
            this.dirty = true;
        }
        return this;
    }
    
    public NpcData setPose(String pose) {
        this.pose = pose;
        this.dirty = true;
        return this;
    }
    
    public NpcData setScale(float scale) {
        this.scale = scale;
        this.dirty = true;
        return this;
    }
    
    public Set<NpcEffect> getEffects() {
        return effects;
    }
    
    public NpcData setEffects(Set<NpcEffect> effects) {
        this.effects = effects != null ? ConcurrentHashMap.newKeySet() : ConcurrentHashMap.newKeySet();
        if (effects != null) {
            this.effects.addAll(effects);
        }
        this.dirty = true;
        return this;
    }
    
    public NpcData addEffect(NpcEffect effect) {
        this.effects.add(effect);
        this.dirty = true;
        return this;
    }
    
    public NpcData removeEffect(NpcEffect effect) {
        this.effects.remove(effect);
        this.dirty = true;
        return this;
    }
    
    public boolean hasEffect(NpcEffect effect) {
        return effects.contains(effect);
    }
    
    public NpcData setActions(Map<ActionTrigger, List<NpcAction.NpcActionData>> actions) {
        this.actions = actions != null ? new ConcurrentHashMap<>(actions) : new ConcurrentHashMap<>();
        this.dirty = true;
        return this;
    }
    
    public NpcData addAction(ActionTrigger trigger, NpcAction.NpcActionData actionData) {
        this.actions.computeIfAbsent(trigger, k -> new ArrayList<>()).add(actionData);
        this.dirty = true;
        return this;
    }
    
    public NpcData setActions(ActionTrigger trigger, List<NpcAction.NpcActionData> actionList) {
        this.actions.put(trigger, new ArrayList<>(actionList));
        this.dirty = true;
        return this;
    }
    
    public NpcData clearActions(ActionTrigger trigger) {
        this.actions.remove(trigger);
        this.dirty = true;
        return this;
    }
    
    public NpcData clearAllActions() {
        this.actions.clear();
        this.dirty = true;
        return this;
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    // ==================== 序列化 ====================
    
    /**
     * 保存 NPC 数据到配置
     * 
     * @param section 配置节
     */
    public void saveToConfig(ConfigurationSection section) {
        // ID
        section.set("id", id);
        
        // 位置
        if (location != null && location.getWorld() != null) {
            section.set("location.world", location.getWorld().getName());
            section.set("location.x", location.getX());
            section.set("location.y", location.getY());
            section.set("location.z", location.getZ());
            section.set("location.yaw", location.getYaw());
            section.set("location.pitch", location.getPitch());
        }
        
        // 皮肤
        section.set("skin.name", skinName != null ? skinName : "");
        section.set("skin.mirror", skinMirror);
        section.set("skin.value", skinValue != null ? skinValue : "");
        section.set("skin.signature", skinSignature != null ? skinSignature : "");
        
        // 显示设置
        section.set("display-name", displayName);
        section.set("show-in-tab", showInTab);
        section.set("turn-to-player", turnToPlayer);
        section.set("collidable", collidable);
        
        // 发光效果
        section.set("glowing.enabled", glowing);
        section.set("glowing.color", glowingColor != null ? glowingColor.getConfigName() : "white");
        
        // 实体类型
        section.set("type", type != null ? type.name() : "PLAYER");
        
        // 装备
        for (Map.Entry<NpcEquipmentSlot, ItemStack> entry : equipment.entrySet()) {
            section.set("equipment." + entry.getKey().getConfigName(), entry.getValue());
        }
        
        // 全息文字
        section.set("hologram", hologramLines);
        
        // 姿势
        section.set("pose", pose);
        
        // 缩放
        section.set("scale", scale);
        
        // 效果
        List<String> effectList = effects.stream()
                .map(NpcEffect::getName)
                .collect(Collectors.toList());
        section.set("effects", effectList);
        
        // 可见距离
        section.set("visibility-distance", visibilityDistance);
        
        // 转向距离
        section.set("turn-distance", turnToPlayerDistance);
        
        // 动作
        for (Map.Entry<ActionTrigger, List<NpcAction.NpcActionData>> entry : actions.entrySet()) {
            String triggerName = entry.getKey().name().toLowerCase();
            for (int i = 0; i < entry.getValue().size(); i++) {
                NpcAction.NpcActionData actionData = entry.getValue().get(i);
                String path = "actions." + triggerName + "." + i;
                section.set(path + ".type", actionData.action().getName());
                section.set(path + ".value", actionData.value() != null ? actionData.value() : "");
                section.set(path + ".order", actionData.order());
            }
        }
    }
    
    /**
     * 从配置加载 NPC 数据
     * 
     * @param section 配置节
     * @return NPC 数据
     */
    public static NpcData fromConfig(ConfigurationSection section) {
        String id = section.getString("id", UUID.randomUUID().toString());
        String name = section.getName();
        
        // 位置
        String worldName = section.getString("location.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0); // 默认使用主世界
        }
        
        double x = section.getDouble("location.x");
        double y = section.getDouble("location.y");
        double z = section.getDouble("location.z");
        float yaw = (float) section.getDouble("location.yaw");
        float pitch = (float) section.getDouble("location.pitch");
        Location location = new Location(world, x, y, z, yaw, pitch);
        
        // 创建基础数据
        NpcData data = new NpcData(name, null, location);
        
        // 使用反射设置 final id 字段
        try {
            java.lang.reflect.Field idField = NpcData.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(data, id);
        } catch (Exception ignored) {
        }
        
        // 皮肤
        data.skinName = section.getString("skin.name", "");
        data.skinMirror = section.getBoolean("skin.mirror", false);
        data.skinValue = section.getString("skin.value", "");
        data.skinSignature = section.getString("skin.signature", "");
        
        // 显示设置
        data.displayName = section.getString("display-name", name);
        data.showInTab = section.getBoolean("show-in-tab", false);
        data.turnToPlayer = section.getBoolean("turn-to-player", false);
        data.collidable = section.getBoolean("collidable", false);
        
        // 发光效果
        data.glowing = section.getBoolean("glowing.enabled", false);
        String colorName = section.getString("glowing.color", "white");
        data.glowingColor = GlowingColor.fromConfigName(colorName);
        
        // 实体类型
        String typeName = section.getString("type", "PLAYER");
        try {
            data.type = EntityType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            data.type = EntityType.PLAYER;
        }
        
        // 装备
        ConfigurationSection equipSection = section.getConfigurationSection("equipment");
        if (equipSection != null) {
            for (String slotName : equipSection.getKeys(false)) {
                NpcEquipmentSlot slot = NpcEquipmentSlot.fromConfigName(slotName);
                if (slot != null) {
                    ItemStack item = equipSection.getItemStack(slotName);
                    if (item != null) {
                        data.equipment.put(slot, item);
                    }
                }
            }
        }
        
        // 全息文字
        List<String> lines = section.getStringList("hologram");
        data.hologramLines.addAll(lines);
        
        // 姿势
        data.pose = section.getString("pose", "STANDING");
        
        // 缩放
        data.scale = (float) section.getDouble("scale", 1.0);
        
        // 效果
        List<String> effectList = section.getStringList("effects");
        for (String effectName : effectList) {
            NpcEffect effect = NpcEffect.getByName(effectName);
            if (effect != null) {
                data.effects.add(effect);
            }
        }
        
        // 可见距离
        data.visibilityDistance = section.getInt("visibility-distance", -1);
        
        // 转向距离
        data.turnToPlayerDistance = section.getInt("turn-distance", -1);
        
        // 动作数据暂时存储为原始格式，后续由 NpcManager 处理
        ConfigurationSection actionsSection = section.getConfigurationSection("actions");
        if (actionsSection != null) {
            data.rawActionsConfig = actionsSection;
        }
        
        return data;
    }
    
    // 临时存储原始动作配置，用于后续解析
    private ConfigurationSection rawActionsConfig;
    
    public ConfigurationSection getRawActionsConfig() {
        return rawActionsConfig;
    }
    
    public void clearRawActionsConfig() {
        this.rawActionsConfig = null;
    }
    
    @Override
    public String toString() {
        return "NpcData{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", type=" + type +
                ", location=" + location +
                '}';
    }
}
