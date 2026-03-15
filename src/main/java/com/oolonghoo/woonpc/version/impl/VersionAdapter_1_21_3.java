package com.oolonghoo.woonpc.version.impl;

import javax.annotation.Nonnull;

/**
 * Minecraft 1.21.2 / 1.21.3 版本适配器
 * <p>
 * 这两个版本使用相同的实现，主要特点：
 * <ul>
 *   <li>使用 ResourceLocation 而非 Identifier</li>
 *   <li>不需要 FakeSynchronizer</li>
 *   <li>使用旧的 PropertyMap 构造方式</li>
 * </ul>
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class VersionAdapter_1_21_3 extends AbstractVersionAdapter {

    @Override
    @Nonnull
    public String getMcVersion() {
        return "1.21.3";
    }

    @Override
    public boolean supportsHappyGhast() {
        return false; // 1.21.2/1.21.3 不支持 Happy Ghast
    }

    @Override
    public boolean supportsNewPropertyMap() {
        return false; // 使用旧的 PropertyMap 构造方式
    }

    @Override
    public boolean requiresFakeSynchronizer() {
        return false; // 不需要 FakeSynchronizer
    }
}
