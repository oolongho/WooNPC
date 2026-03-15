package com.oolonghoo.woonpc.version.impl;

import javax.annotation.Nonnull;

/**
 * Minecraft 1.21.9 / 1.21.10 版本适配器
 * <p>
 * 这两个版本使用相同的实现，主要特点：
 * <ul>
 *   <li>需要使用 FakeSynchronizer</li>
 *   <li>支持 Happy Ghast</li>
 *   <li>使用 Identifier 替代 ResourceLocation</li>
 *   <li>使用旧的 PropertyMap 构造方式</li>
 * </ul>
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class VersionAdapter_1_21_9 extends AbstractVersionAdapter {

    @Override
    @Nonnull
    public String getMcVersion() {
        return "1.21.9";
    }

    @Override
    public boolean supportsHappyGhast() {
        return true; // 1.21.9+ 支持 Happy Ghast
    }

    @Override
    public boolean supportsNewPropertyMap() {
        return false; // 使用旧的 PropertyMap 构造方式
    }

    @Override
    public boolean requiresFakeSynchronizer() {
        return true; // 需要 FakeSynchronizer
    }
}
