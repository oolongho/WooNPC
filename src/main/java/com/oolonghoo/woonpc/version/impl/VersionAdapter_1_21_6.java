package com.oolonghoo.woonpc.version.impl;

import javax.annotation.Nonnull;

/**
 * Minecraft 1.21.6 / 1.21.7 / 1.21.8 版本适配器
 * <p>
 * 这三个版本使用相同的实现，主要特点：
 * <ul>
 *   <li>引入 Happy Ghast 实体</li>
 *   <li>使用 ResourceLocation</li>
 *   <li>不需要 FakeSynchronizer</li>
 *   <li>使用旧的 PropertyMap 构造方式</li>
 * </ul>
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class VersionAdapter_1_21_6 extends AbstractVersionAdapter {

    @Override
    @Nonnull
    public String getMcVersion() {
        return "1.21.6";
    }

    @Override
    public boolean supportsHappyGhast() {
        return true; // 1.21.6+ 支持 Happy Ghast
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
