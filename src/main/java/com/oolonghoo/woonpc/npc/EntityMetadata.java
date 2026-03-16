package com.oolonghoo.woonpc.npc;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 实体元数据管理类
 * <p>
 * 支持所有实体类型的属性配置，包括姿势、发光、缩放、隐身、名称标签等。
 * 使用 SynchedEntityData.DataValue 创建数据值，通过数据包发送更新实体状态。
 * </p>
 *
 * @author oolongho
 * @since 2.0.0
 */
public class EntityMetadata {

    // ==================== 实体数据访问器缓存 ====================

    // Entity 基础属性
    private static EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID;
    private static EntityDataAccessor<Optional<net.minecraft.network.chat.Component>> DATA_CUSTOM_NAME;
    private static EntityDataAccessor<Boolean> DATA_CUSTOM_NAME_VISIBLE;
    private static EntityDataAccessor<Boolean> DATA_SILENT;
    private static EntityDataAccessor<Pose> DATA_POSE;
    private static EntityDataAccessor<Integer> DATA_TICKS_FROZEN;

    // Player 属性
    private static EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION;

    // Display 实体属性 (1.19.4+)
    private static EntityDataAccessor<Float> DATA_SCALE;
    private static EntityDataAccessor<Integer> DATA_GLOW_COLOR_OVERRIDE;

    // ArmorStand 属性
    private static EntityDataAccessor<Byte> DATA_ARMOR_STAND_FLAGS;

    // 初始化状态
    private static boolean initialized = false;

    // 实体标志位常量
    private static final byte FLAG_ON_FIRE = 0x01;
    private static final byte FLAG_SHIFT_KEY_DOWN = 0x02;
    private static final byte FLAG_INVISIBLE = 0x20;
    private static final byte FLAG_GLOWING = 0x40;

    // ArmorStand 标志位常量
    private static final byte ARMOR_STAND_FLAG_SMALL = 0x01;
    private static final byte ARMOR_STAND_FLAG_SHOW_ARMS = 0x04;

    static {
        initAccessors();
    }

    /**
     * 初始化实体数据访问器
     * 通过反射获取 NMS 实体类中的数据访问器
     */
    private static synchronized void initAccessors() {
        if (initialized) return;
        initialized = true;

        // Entity 基础属性
        DATA_SHARED_FLAGS_ID = getAccessor(Entity.class, "DATA_SHARED_FLAGS_ID", "f_20094_");
        DATA_CUSTOM_NAME = getAccessor(Entity.class, "DATA_CUSTOM_NAME", "f_19758_");
        DATA_CUSTOM_NAME_VISIBLE = getAccessor(Entity.class, "DATA_CUSTOM_NAME_VISIBLE", "f_19759_");
        DATA_SILENT = getAccessor(Entity.class, "DATA_SILENT", "f_19760_");
        DATA_POSE = getAccessor(Entity.class, "DATA_POSE", "f_19762_");
        DATA_TICKS_FROZEN = getAccessor(Entity.class, "DATA_TICKS_FROZEN", "f_19763_");

        // Player 属性
        DATA_PLAYER_MODE_CUSTOMISATION = getAccessor(net.minecraft.world.entity.player.Player.class,
                "DATA_PLAYER_MODE_CUSTOMISATION", "f_35838_");

        // ArmorStand 属性
        DATA_ARMOR_STAND_FLAGS = getAccessor(ArmorStand.class, "DATA_CLIENT_FLAGS", "f_31558_");

        // Display 实体属性 (尝试获取，可能不存在于旧版本)
        DATA_SCALE = getAccessor(net.minecraft.world.entity.Display.class, "DATA_SCALE", "f_270603_");
        DATA_GLOW_COLOR_OVERRIDE = getAccessor(net.minecraft.world.entity.Display.class,
                "DATA_GLOW_COLOR_OVERRIDE", "f_271437_");
    }

    /**
     * 通过反射获取实体数据访问器
     *
     * @param clazz         实体类
     * @param fieldName     字段名（Mojang 映射名）
     * @param srgName       字段名（SRG/中间名）
     * @param <T>           数据类型
     * @return              数据访问器
     */
    @SuppressWarnings("unchecked")
    private static <T> EntityDataAccessor<T> getAccessor(Class<?> clazz, String fieldName, String srgName) {
        try {
            // 尝试 Mojang 映射名
            Field field = findField(clazz, fieldName, srgName);
            if (field != null) {
                field.setAccessible(true);
                return (EntityDataAccessor<T>) field.get(null);
            }
        } catch (ReflectiveOperationException | SecurityException e) {
            Bukkit.getLogger().warning(() -> "[WooNPC] Failed to get accessor " + fieldName + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * 查找字段（支持多个名称）
     */
    private static Field findField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // 尝试下一个名称
            }
        }
        // 尝试在父类中查找
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return findField(superClass, names);
        }
        return null;
    }

    // ==================== 元数据构建 ====================

    /**
     * 构建实体元数据值列表
     *
     * @param data      NPC 数据
     * @param entityId  实体 ID
     * @return          数据值列表
     */
    public static List<SynchedEntityData.DataValue<?>> buildMetadata(NpcData data, int entityId) {
        List<SynchedEntityData.DataValue<?>> values = new ArrayList<>();

        // 基础实体标志
        buildEntityFlags(data, values);

        // 姿势
        buildPose(data, values);

        // 自定义名称
        buildCustomName(data, values);

        // 效果（着火、隐身、颤抖等）
        buildEffects(data, values);

        // 玩家特有属性
        if (data.getType() == EntityType.PLAYER) {
            buildPlayerSpecific(data, values);
        }

        // Display 实体属性（缩放等）
        if (isDisplayEntity(data.getType())) {
            buildDisplayAttributes(data, values);
        }

        // ArmorStand 属性
        if (data.getType() == EntityType.ARMOR_STAND) {
            buildArmorStandAttributes(data, values);
        }

        return values;
    }

    /**
     * 构建实体基础标志
     */
    private static void buildEntityFlags(NpcData data, List<SynchedEntityData.DataValue<?>> values) {
        if (DATA_SHARED_FLAGS_ID == null) return;

        byte flags = 0;

        // 着火效果
        if (data.hasEffect(NpcEffect.ON_FIRE)) {
            flags |= FLAG_ON_FIRE;
        }

        // 隐身效果
        if (data.hasEffect(NpcEffect.INVISIBLE)) {
            flags |= FLAG_INVISIBLE;
        }

        // 发光效果
        if (data.isGlowing()) {
            flags |= FLAG_GLOWING;
        }

        // 蹲下姿势
        if ("crouching".equalsIgnoreCase(data.getPose()) || "sneaking".equalsIgnoreCase(data.getPose())) {
            flags |= FLAG_SHIFT_KEY_DOWN;
        }

        values.add(SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID, flags));
    }

    /**
     * 构建姿势数据
     */
    private static void buildPose(NpcData data, List<SynchedEntityData.DataValue<?>> values) {
        if (DATA_POSE == null) return;

        String poseStr = data.getPose();
        if (poseStr == null || poseStr.isEmpty() || "sitting".equalsIgnoreCase(poseStr)) {
            return;
        }

        try {
            Pose pose = Pose.valueOf(poseStr.toUpperCase());
            values.add(SynchedEntityData.DataValue.create(DATA_POSE, pose));
        } catch (IllegalArgumentException e) {
            // 忽略无效姿势
            Bukkit.getLogger().fine(() -> "[WooNPC] Invalid pose: " + poseStr);
        }
    }

    /**
     * 构建自定义名称数据
     */
    private static void buildCustomName(NpcData data, List<SynchedEntityData.DataValue<?>> values) {
        String displayName = data.getDisplayName();

        // 处理空名称标签
        if ("<empty>".equalsIgnoreCase(displayName)) {
            if (DATA_CUSTOM_NAME_VISIBLE != null) {
                values.add(SynchedEntityData.DataValue.create(DATA_CUSTOM_NAME_VISIBLE, false));
            }
            return;
        }

        // 设置自定义名称
        if (displayName != null && !displayName.isEmpty() && DATA_CUSTOM_NAME != null) {
            net.minecraft.network.chat.Component nameComponent =
                    net.minecraft.network.chat.Component.literal(displayName);
            values.add(SynchedEntityData.DataValue.create(DATA_CUSTOM_NAME, Optional.of(nameComponent)));

            if (DATA_CUSTOM_NAME_VISIBLE != null) {
                values.add(SynchedEntityData.DataValue.create(DATA_CUSTOM_NAME_VISIBLE, true));
            }
        }
    }

    /**
     * 构建效果数据
     */
    private static void buildEffects(NpcData data, List<SynchedEntityData.DataValue<?>> values) {
        // 静音效果
        if (data.hasEffect(NpcEffect.SILENT) && DATA_SILENT != null) {
            values.add(SynchedEntityData.DataValue.create(DATA_SILENT, true));
        }

        // 颤抖效果（冻结）
        if (data.hasEffect(NpcEffect.SHAKING) && DATA_TICKS_FROZEN != null) {
            // 设置冻结时间，使其显示颤抖效果
            values.add(SynchedEntityData.DataValue.create(DATA_TICKS_FROZEN, Integer.MAX_VALUE));
        }
    }

    /**
     * 构建玩家特有属性
     */
    @SuppressWarnings("java:S1172")
    private static void buildPlayerSpecific(NpcData data, List<SynchedEntityData.DataValue<?>> values) {
        if (DATA_PLAYER_MODE_CUSTOMISATION == null) return;

        // 皮肤层标志
        // 0x01 = Cape (披风)
        // 0x02 = Jacket (上衣)
        // 0x04 = Left Sleeve (左袖)
        // 0x08 = Right Sleeve (右袖)
        // 0x10 = Left Pants Leg (左裤腿)
        // 0x20 = Right Pants Leg (右裤腿)
        // 0x40 = Hat (帽子)
        byte skinLayers = 0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40;

        values.add(SynchedEntityData.DataValue.create(DATA_PLAYER_MODE_CUSTOMISATION, skinLayers));
    }

    /**
     * 构建 Display 实体属性
     */
    private static void buildDisplayAttributes(NpcData data, List<SynchedEntityData.DataValue<?>> values) {
        // 缩放
        float scale = data.getScale();
        if (scale != 1.0f && DATA_SCALE != null) {
            // 限制缩放范围
            scale = Math.max(0.1f, Math.min(10.0f, scale));
            values.add(SynchedEntityData.DataValue.create(DATA_SCALE, scale));
        }

        // 发光颜色覆盖（用于非玩家实体的发光颜色）
        if (data.isGlowing() && !data.getGlowingColor().isDisabled() && DATA_GLOW_COLOR_OVERRIDE != null) {
            net.kyori.adventure.text.format.NamedTextColor color = data.getGlowingColor().getAdventureColor();
            if (color != null) {
                // 转换为 RGB 颜色值
                int rgb = color.value();
                values.add(SynchedEntityData.DataValue.create(DATA_GLOW_COLOR_OVERRIDE, rgb));
            }
        }
    }

    /**
     * 构建 ArmorStand 属性
     */
    private static void buildArmorStandAttributes(NpcData data, List<SynchedEntityData.DataValue<?>> values) {
        if (DATA_ARMOR_STAND_FLAGS == null) return;

        byte flags = 0;

        // 默认显示手臂
        flags |= ARMOR_STAND_FLAG_SHOW_ARMS;

        // 小型盔甲架
        if (data.getScale() < 0.5f) {
            flags |= ARMOR_STAND_FLAG_SMALL;
        }

        values.add(SynchedEntityData.DataValue.create(DATA_ARMOR_STAND_FLAGS, flags));
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查是否为 Display 实体类型
     */
    private static boolean isDisplayEntity(EntityType type) {
        return type == EntityType.TEXT_DISPLAY
                || type == EntityType.BLOCK_DISPLAY
                || type == EntityType.ITEM_DISPLAY;
    }

    /**
     * 创建姿势数据值
     *
     * @param pose 姿势
     * @return 数据值，如果访问器不可用则返回 null
     */
    public static SynchedEntityData.DataValue<?> createPoseValue(Pose pose) {
        if (DATA_POSE == null) return null;
        return SynchedEntityData.DataValue.create(DATA_POSE, pose);
    }

    /**
     * 创建自定义名称数据值
     *
     * @param name    名称
     * @param visible 是否可见
     * @return 数据值列表
     */
    public static List<SynchedEntityData.DataValue<?>> createCustomNameValues(String name, boolean visible) {
        List<SynchedEntityData.DataValue<?>> values = new ArrayList<>();

        if (DATA_CUSTOM_NAME != null && name != null) {
            net.minecraft.network.chat.Component nameComponent =
                    net.minecraft.network.chat.Component.literal(name);
            values.add(SynchedEntityData.DataValue.create(DATA_CUSTOM_NAME, Optional.of(nameComponent)));
        }

        if (DATA_CUSTOM_NAME_VISIBLE != null) {
            values.add(SynchedEntityData.DataValue.create(DATA_CUSTOM_NAME_VISIBLE, visible));
        }

        return values;
    }

    /**
     * 创建发光效果数据值
     *
     * @param glowing 是否发光
     * @return 数据值，如果访问器不可用则返回 null
     */
    public static SynchedEntityData.DataValue<?> createGlowingValue(boolean glowing) {
        if (DATA_SHARED_FLAGS_ID == null) return null;

        // 注意：这里只设置发光位，实际使用时需要考虑其他标志位
        byte flags = glowing ? FLAG_GLOWING : 0;
        return SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID, flags);
    }

    /**
     * 创建隐身效果数据值
     *
     * @param invisible 是否隐身
     * @return 数据值，如果访问器不可用则返回 null
     */
    public static SynchedEntityData.DataValue<?> createInvisibleValue(boolean invisible) {
        if (DATA_SHARED_FLAGS_ID == null) return null;

        byte flags = invisible ? FLAG_INVISIBLE : 0;
        return SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID, flags);
    }

    /**
     * 创建着火效果数据值
     *
     * @param onFire 是否着火
     * @return 数据值，如果访问器不可用则返回 null
     */
    public static SynchedEntityData.DataValue<?> createOnFireValue(boolean onFire) {
        if (DATA_SHARED_FLAGS_ID == null) return null;

        byte flags = onFire ? FLAG_ON_FIRE : 0;
        return SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID, flags);
    }

    /**
     * 创建颤抖效果数据值
     *
     * @param shaking 是否颤抖
     * @return 数据值，如果访问器不可用则返回 null
     */
    public static SynchedEntityData.DataValue<?> createShakingValue(boolean shaking) {
        if (DATA_TICKS_FROZEN == null) return null;

        // 设置冻结时间来显示颤抖效果
        int ticks = shaking ? Integer.MAX_VALUE : 0;
        return SynchedEntityData.DataValue.create(DATA_TICKS_FROZEN, ticks);
    }

    /**
     * 创建缩放数据值
     *
     * @param scale 缩放值 (0.1 - 10.0)
     * @return 数据值，如果访问器不可用则返回 null
     */
    public static SynchedEntityData.DataValue<?> createScaleValue(float scale) {
        if (DATA_SCALE == null) return null;

        // 限制缩放范围
        scale = Math.max(0.1f, Math.min(10.0f, scale));
        return SynchedEntityData.DataValue.create(DATA_SCALE, scale);
    }

    /**
     * 创建皮肤层数据值
     *
     * @param layers 皮肤层标志
     * @return 数据值，如果访问器不可用则返回 null
     */
    public static SynchedEntityData.DataValue<?> createSkinLayersValue(byte layers) {
        if (DATA_PLAYER_MODE_CUSTOMISATION == null) return null;
        return SynchedEntityData.DataValue.create(DATA_PLAYER_MODE_CUSTOMISATION, layers);
    }

    /**
     * 获取默认皮肤层标志（显示所有层）
     *
     * @return 皮肤层标志
     */
    public static byte getDefaultSkinLayers() {
        return 0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40;
    }

    // ==================== 装备数据包构建 ====================

    /**
     * 构建装备数据包数据
     *
     * @param entityId  实体 ID
     * @param equipment 装备映射
     * @return 装备槽位列表（用于 ClientboundSetEquipmentPacket）
     */
    public static List<com.mojang.datafixers.util.Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>>
    buildEquipmentData(int entityId, Map<NpcEquipmentSlot, org.bukkit.inventory.ItemStack> equipment) {

        List<com.mojang.datafixers.util.Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>> equipmentList = new ArrayList<>();

        for (Map.Entry<NpcEquipmentSlot, org.bukkit.inventory.ItemStack> entry : equipment.entrySet()) {
            NpcEquipmentSlot slot = entry.getKey();
            org.bukkit.inventory.ItemStack bukkitItem = entry.getValue();

            if (bukkitItem == null || bukkitItem.getType().isAir()) {
                continue;
            }

            // 转换为 NMS 物品
            net.minecraft.world.item.ItemStack nmsItem = org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(bukkitItem);

            // 映射槽位
            net.minecraft.world.entity.EquipmentSlot nmsSlot = mapEquipmentSlot(slot);
            if (nmsSlot != null) {
                equipmentList.add(com.mojang.datafixers.util.Pair.of(nmsSlot, nmsItem));
            }
        }

        return equipmentList;
    }

    /**
     * 映射装备槽位
     */
    private static net.minecraft.world.entity.EquipmentSlot mapEquipmentSlot(NpcEquipmentSlot slot) {
        return switch (slot) {
            case MAIN_HAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFF_HAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            case CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case FEET -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case BODY -> net.minecraft.world.entity.EquipmentSlot.BODY;
        };
    }

    // ==================== 效果数据包构建 ====================

    /**
     * 效果数据持有者
     * 用于批量更新实体效果
     */
    public static class EffectData {
        private boolean onFire;
        private boolean invisible;
        private boolean shaking;
        private boolean silent;
        private boolean glowing;

        public EffectData onFire(boolean onFire) {
            this.onFire = onFire;
            return this;
        }

        public EffectData invisible(boolean invisible) {
            this.invisible = invisible;
            return this;
        }

        public EffectData shaking(boolean shaking) {
            this.shaking = shaking;
            return this;
        }

        public EffectData silent(boolean silent) {
            this.silent = silent;
            return this;
        }

        public EffectData glowing(boolean glowing) {
            this.glowing = glowing;
            return this;
        }

        /**
         * 构建效果数据值列表
         */
        public List<SynchedEntityData.DataValue<?>> buildValues() {
            List<SynchedEntityData.DataValue<?>> values = new ArrayList<>();

            // 构建标志位
            if (DATA_SHARED_FLAGS_ID != null) {
                byte flags = 0;
                if (onFire) flags |= FLAG_ON_FIRE;
                if (invisible) flags |= FLAG_INVISIBLE;
                if (glowing) flags |= FLAG_GLOWING;
                values.add(SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID, flags));
            }

            // 颤抖效果
            if (shaking && DATA_TICKS_FROZEN != null) {
                values.add(SynchedEntityData.DataValue.create(DATA_TICKS_FROZEN, Integer.MAX_VALUE));
            }

            // 静音效果
            if (silent && DATA_SILENT != null) {
                values.add(SynchedEntityData.DataValue.create(DATA_SILENT, true));
            }

            return values;
        }
    }

    /**
     * 创建效果数据构建器
     */
    public static EffectData effects() {
        return new EffectData();
    }

    // ==================== 访问器获取方法 ====================

    /**
     * 获取共享标志访问器
     */
    public static EntityDataAccessor<Byte> getSharedFlagsAccessor() {
        return DATA_SHARED_FLAGS_ID;
    }

    /**
     * 获取姿势访问器
     */
    public static EntityDataAccessor<Pose> getPoseAccessor() {
        return DATA_POSE;
    }

    /**
     * 获取自定义名称访问器
     */
    public static EntityDataAccessor<Optional<net.minecraft.network.chat.Component>> getCustomNameAccessor() {
        return DATA_CUSTOM_NAME;
    }

    /**
     * 获取自定义名称可见性访问器
     */
    public static EntityDataAccessor<Boolean> getCustomNameVisibleAccessor() {
        return DATA_CUSTOM_NAME_VISIBLE;
    }

    /**
     * 获取缩放访问器
     */
    public static EntityDataAccessor<Float> getScaleAccessor() {
        return DATA_SCALE;
    }

    /**
     * 获取玩家皮肤层访问器
     */
    public static EntityDataAccessor<Byte> getPlayerModeCustomisationAccessor() {
        return DATA_PLAYER_MODE_CUSTOMISATION;
    }

    /**
     * 获取冻结时间访问器
     */
    public static EntityDataAccessor<Integer> getTicksFrozenAccessor() {
        return DATA_TICKS_FROZEN;
    }

    /**
     * 获取静音访问器
     */
    public static EntityDataAccessor<Boolean> getSilentAccessor() {
        return DATA_SILENT;
    }
}
