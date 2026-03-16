package com.oolonghoo.woonpc.version.impl;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.bukkit.Bukkit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Minecraft 1.21.11 版本适配器
 * <p>
 * 最新版本的主要特点：
 * <ul>
 *   <li>使用新的 PropertyMap 构造方式（ImmutableMultimap）</li>
 *   <li>需要使用 FakeSynchronizer</li>
 *   <li>支持 Happy Ghast</li>
 *   <li>使用 Identifier 替代 ResourceLocation</li>
 *   <li>支持新的 PlayerInfoEntry 构造方式</li>
 * </ul>
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
@SuppressWarnings("java:S3457")
public class VersionAdapter_1_21_11 extends AbstractVersionAdapter {

    @Override
    @Nonnull
    public String getMcVersion() {
        return "1.21.11";
    }

    @Override
    public boolean supportsHappyGhast() {
        return true; // 1.21.11 支持 Happy Ghast
    }

    @Override
    public boolean supportsNewPropertyMap() {
        return true; // 使用新的 PropertyMap 构造方式
    }

    @Override
    public boolean requiresFakeSynchronizer() {
        return true; // 需要 FakeSynchronizer
    }

    /**
     * 创建带皮肤的 GameProfile（使用新的 PropertyMap 构造方式）
     */
    @Override
    @Nonnull
    public GameProfile createGameProfileWithSkin(@Nonnull UUID uuid, @Nonnull String name,
                                                 @Nullable String skinValue, @Nullable String skinSignature) {
        if (skinValue == null || skinValue.isEmpty()) {
            return new GameProfile(uuid, name);
        }

        try {
            // 1.21.11 使用新的 PropertyMap 构造方式
            PropertyMap propertyMap = new PropertyMap(
                    ImmutableMultimap.of(
                            "textures",
                            new Property("textures", skinValue, skinSignature)
                    )
            );
            return new GameProfile(uuid, name, propertyMap);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[WooNPC] Failed to create GameProfile with skin: " + e.getMessage());
            return new GameProfile(uuid, name);
        }
    }

    /**
     * 创建玩家信息条目（1.21.11 特定格式）
     */
    @Override
    protected ClientboundPlayerInfoUpdatePacket.Entry createPlayerInfoEntry(java.util.UUID uuid, GameProfile profile,
                                                                            boolean showInTab, Component displayName) {
        return new ClientboundPlayerInfoUpdatePacket.Entry(
                uuid,
                profile,
                showInTab,
                0,
                net.minecraft.world.level.GameType.SURVIVAL,
                displayName,
                true,
                -1,
                null  // RemoteChatSession.Data
        );
    }
}
