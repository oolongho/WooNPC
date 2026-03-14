package com.oolonghoo.woonpc.util;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings({"java:S3457", "java:S1444"})
public final class PacketFactory {

    private PacketFactory() {
    }
    
    private static Method gameProfileIdMethod;
    private static Method gameProfileNameMethod;
    private static Constructor<?> gameProfileConstructorWithProps;
    private static Constructor<?> propertyMapConstructor;
    
    static {
        try {
            gameProfileIdMethod = GameProfile.class.getMethod("getId");
        } catch (NoSuchMethodException e) {
            try {
                gameProfileIdMethod = GameProfile.class.getMethod("id");
            } catch (NoSuchMethodException ex) {
                Bukkit.getLogger().warning("[WooNPC] 无法获取 GameProfile.getId 方法：" + ex.getMessage());
            }
        }
        try {
            gameProfileNameMethod = GameProfile.class.getMethod("getName");
        } catch (NoSuchMethodException e) {
            try {
                gameProfileNameMethod = GameProfile.class.getMethod("name");
            } catch (NoSuchMethodException ex) {
                Bukkit.getLogger().warning("[WooNPC] 无法获取 GameProfile.getName 方法：" + ex.getMessage());
            }
        }
        try {
            gameProfileConstructorWithProps = GameProfile.class.getConstructor(UUID.class, String.class, PropertyMap.class);
        } catch (NoSuchMethodException e) {
            gameProfileConstructorWithProps = null;
        }
        try {
            propertyMapConstructor = PropertyMap.class.getConstructor(ImmutableMultimap.class);
        } catch (NoSuchMethodException e) {
            propertyMapConstructor = null;
        }
    }
    
    private static UUID getProfileId(GameProfile profile) {
        if (profile == null) return null;
        try {
            if (gameProfileIdMethod != null) {
                return (UUID) gameProfileIdMethod.invoke(profile);
            }
        } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            Bukkit.getLogger().warning("[WooNPC] 获取 GameProfile ID 失败：" + e.getMessage());
        }
        return null;
    }
    
    private static String getProfileName(GameProfile profile) {
        if (profile == null) return "";
        try {
            if (gameProfileNameMethod != null) {
                return (String) gameProfileNameMethod.invoke(profile);
            }
        } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            Bukkit.getLogger().warning("[WooNPC] 获取 GameProfile 名称失败：" + e.getMessage());
        }
        return "";
    }
    
    private static GameProfile createGameProfileWithSkin(UUID uuid, String name, String skinValue, String skinSignature) {
        try {
            if (propertyMapConstructor != null && gameProfileConstructorWithProps != null) {
                PropertyMap propertyMap = (PropertyMap) propertyMapConstructor.newInstance(
                        ImmutableMultimap.of("textures", new Property("textures", skinValue, skinSignature))
                );
                return (GameProfile) gameProfileConstructorWithProps.newInstance(uuid, name, propertyMap);
            }
        } catch (InstantiationException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            Bukkit.getLogger().warning("[WooNPC] 创建带皮肤的 GameProfile 失败：" + e.getMessage());
        }
        // 回退到基本构造函数
        return new GameProfile(uuid, name);
    }

    public static ClientboundPlayerInfoUpdatePacket createPlayerInfoAddPacket(GameProfile profile, String displayName, boolean showInTab) {
        EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
        actions.add(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);
        actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
        if (showInTab) {
            actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED);
        }

        Component displayComponent = Component.literal(displayName);
        
        UUID profileId = getProfileId(profile);
        
        ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                profileId != null ? profileId : UUID.randomUUID(),
                profile,
                showInTab,
                0,
                net.minecraft.world.level.GameType.SURVIVAL,
                displayComponent,
                true,
                -1,
                null
        );

        return new ClientboundPlayerInfoUpdatePacket(actions, List.of(entry));
    }

    public static ClientboundPlayerInfoUpdatePacket createPlayerInfoAddPacket(GameProfile profile, String displayName, boolean showInTab, String skinValue, String skinSignature) {
        UUID profileId = getProfileId(profile);
        String profileName = getProfileName(profile);
        GameProfile profileWithSkin = createGameProfileWithSkin(
                profileId != null ? profileId : UUID.randomUUID(),
                profileName,
                skinValue,
                skinSignature
        );
        
        return createPlayerInfoAddPacket(profileWithSkin, displayName, showInTab);
    }

    public static ClientboundPlayerInfoRemovePacket createPlayerInfoRemovePacket(UUID uuid) {
        return new ClientboundPlayerInfoRemovePacket(List.of(uuid));
    }

    public static ClientboundAddEntityPacket createAddEntityPacket(int entityId, UUID uuid, Location location, org.bukkit.entity.EntityType type) {
        Optional<net.minecraft.core.Holder.Reference<EntityType<?>>> entityTypeReference = 
                BuiltInRegistries.ENTITY_TYPE.get(CraftNamespacedKey.toMinecraft(type.getKey()));
        
        if (entityTypeReference.isEmpty()) {
            throw new IllegalArgumentException("Unknown entity type: " + type);
        }
        
        EntityType<?> nmsType = entityTypeReference.get().value();
        
        return new ClientboundAddEntityPacket(
                entityId,
                uuid,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw(),
                nmsType,
                0,
                Vec3.ZERO,
                location.getYaw()
        );
    }

    public static ClientboundAddEntityPacket createAddEntityPacket(int entityId, UUID uuid, Location location, EntityType<?> type) {
        return new ClientboundAddEntityPacket(
                entityId,
                uuid,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw(),
                type,
                0,
                Vec3.ZERO,
                location.getYaw()
        );
    }

    public static ClientboundRemoveEntitiesPacket createRemoveEntitiesPacket(int... entityIds) {
        return new ClientboundRemoveEntitiesPacket(Arrays.stream(entityIds).toArray());
    }

    public static ClientboundRemoveEntitiesPacket createRemoveEntitiesPacket(List<Integer> entityIds) {
        return new ClientboundRemoveEntitiesPacket(entityIds.stream().mapToInt(Integer::intValue).toArray());
    }

    public static ClientboundBundlePacket createBundlePacket(List<Packet<?>> packets) {
        @SuppressWarnings("unchecked")
        List<Packet<? super ClientGamePacketListener>> castedPackets = 
                (List<Packet<? super ClientGamePacketListener>>) (List<?>) packets;
        return new ClientboundBundlePacket(castedPackets);
    }

    public static ClientboundTeleportEntityPacket createTeleportPacket(int entityId, Location location, float yaw, float pitch) {
        return new ClientboundTeleportEntityPacket(
                entityId,
                new PositionMoveRotation(
                        new Vec3(location.getX(), location.getY(), location.getZ()),
                        Vec3.ZERO,
                        yaw,
                        pitch
                ),
                Set.of(),
                false
        );
    }

    public static ClientboundRotateHeadPacket createHeadRotationPacket(Entity entity, float yaw) {
        float angleMultiplier = 256f / 360f;
        return new ClientboundRotateHeadPacket(entity, (byte) (yaw * angleMultiplier));
    }

    public static ClientboundRotateHeadPacket createHeadRotationPacket(int entityId, float yaw) {
        throw new UnsupportedOperationException("Use createHeadRotationPacket(Entity, float) instead");
    }

    public static ClientboundSetEquipmentPacket createEquipmentPacket(int entityId, Map<EquipmentSlot, org.bukkit.inventory.ItemStack> equipment) {
        List<Pair<EquipmentSlot, ItemStack>> equipmentList = new ArrayList<>();
        
        for (Map.Entry<EquipmentSlot, org.bukkit.inventory.ItemStack> entry : equipment.entrySet()) {
            ItemStack nmsItem = CraftItemStack.asNMSCopy(entry.getValue());
            equipmentList.add(new Pair<>(entry.getKey(), nmsItem));
        }
        
        return new ClientboundSetEquipmentPacket(entityId, equipmentList);
    }

    public static ClientboundSetEquipmentPacket createEquipmentPacketNMS(int entityId, Map<EquipmentSlot, ItemStack> equipment) {
        List<Pair<EquipmentSlot, ItemStack>> equipmentList = new ArrayList<>();
        
        for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
            equipmentList.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        
        return new ClientboundSetEquipmentPacket(entityId, equipmentList);
    }

    @SuppressWarnings("deprecation")
    public static ClientboundSetPlayerTeamPacket createTeamPacket(String teamName, String playerName, ChatColor color, boolean create) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
        team.getPlayers().clear();
        team.getPlayers().add(playerName);
        
        net.minecraft.ChatFormatting chatFormatting = convertToNMSChatFormatting(color);
        team.setColor(chatFormatting);
        
        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, create);
    }

    @SuppressWarnings("deprecation")
    public static ClientboundSetPlayerTeamPacket createTeamPacket(String teamName, String playerName, ChatColor color, 
                                                                   boolean create, Team.CollisionRule collisionRule, 
                                                                   Team.Visibility nameTagVisibility) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
        team.getPlayers().clear();
        team.getPlayers().add(playerName);
        
        net.minecraft.ChatFormatting chatFormatting = convertToNMSChatFormatting(color);
        team.setColor(chatFormatting);
        
        if (collisionRule != null) {
            team.setCollisionRule(collisionRule);
        }
        
        if (nameTagVisibility != null) {
            team.setNameTagVisibility(nameTagVisibility);
        }
        
        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, create);
    }

    public static ClientboundSetPlayerTeamPacket createRemoveTeamPacket(String teamName) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
        return ClientboundSetPlayerTeamPacket.createRemovePacket(team);
    }

    public static void sendPacket(Player player, Object packet) {
        if (player == null || packet == null) {
            return;
        }
        
        net.minecraft.server.level.ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        serverPlayer.connection.send((Packet<?>) packet);
    }

    public static void sendPackets(Player player, List<Object> packets) {
        if (player == null || packets == null || packets.isEmpty()) {
            return;
        }
        
        net.minecraft.server.level.ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        for (Object packet : packets) {
            if (packet instanceof Packet) {
                serverPlayer.connection.send((Packet<?>) packet);
            }
        }
    }

    public static void sendBundlePacket(Player player, List<Packet<?>> packets) {
        if (player == null || packets == null || packets.isEmpty()) {
            return;
        }
        
        net.minecraft.server.level.ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        ClientboundBundlePacket bundlePacket = createBundlePacket(packets);
        serverPlayer.connection.send(bundlePacket);
    }

    public static ClientboundSetEntityDataPacket createEntityDataPacket(int entityId, List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> dataItems) {
        return new ClientboundSetEntityDataPacket(entityId, dataItems);
    }

    public static ClientboundAnimatePacket createAnimatePacket(int entityId, int animationId) {
        throw new UnsupportedOperationException("Use entity-based animate packet creation");
    }

    public static ClientboundAnimatePacket createAnimatePacket(Entity entity, int animationId) {
        return new ClientboundAnimatePacket(entity, animationId);
    }

    @SuppressWarnings("deprecation")
    private static net.minecraft.ChatFormatting convertToNMSChatFormatting(ChatColor color) {
        if (color == null) {
            return net.minecraft.ChatFormatting.WHITE;
        }
        
        return switch (color) {
            case BLACK -> net.minecraft.ChatFormatting.BLACK;
            case DARK_BLUE -> net.minecraft.ChatFormatting.DARK_BLUE;
            case DARK_GREEN -> net.minecraft.ChatFormatting.DARK_GREEN;
            case DARK_AQUA -> net.minecraft.ChatFormatting.DARK_AQUA;
            case DARK_RED -> net.minecraft.ChatFormatting.DARK_RED;
            case DARK_PURPLE -> net.minecraft.ChatFormatting.DARK_PURPLE;
            case GOLD -> net.minecraft.ChatFormatting.GOLD;
            case GRAY -> net.minecraft.ChatFormatting.GRAY;
            case DARK_GRAY -> net.minecraft.ChatFormatting.DARK_GRAY;
            case BLUE -> net.minecraft.ChatFormatting.BLUE;
            case GREEN -> net.minecraft.ChatFormatting.GREEN;
            case AQUA -> net.minecraft.ChatFormatting.AQUA;
            case RED -> net.minecraft.ChatFormatting.RED;
            case LIGHT_PURPLE -> net.minecraft.ChatFormatting.LIGHT_PURPLE;
            case YELLOW -> net.minecraft.ChatFormatting.YELLOW;
            case WHITE -> net.minecraft.ChatFormatting.WHITE;
            default -> net.minecraft.ChatFormatting.WHITE;
        };
    }

    public static net.minecraft.server.level.ServerPlayer getServerPlayer(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    public static ItemStack toNMSItemStack(org.bukkit.inventory.ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    public static org.bukkit.inventory.ItemStack toBukkitItemStack(ItemStack itemStack) {
        return CraftItemStack.asBukkitCopy(itemStack);
    }
}
