package com.oolonghoo.woonpc.version.impl;

import javax.annotation.Nonnull;

/**
 * Minecraft 1.21.5 版本适配器
 * <p>
 * 主要特点：
 * <ul>
 *   <li>使用 ResourceLocation</li>
 *   <li>不需要 FakeSynchronizer</li>
 *   <li>使用旧的 PropertyMap 构造方式</li>
 *   <li>添加了 Sniffer 等新实体</li>
 * </ul>
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class VersionAdapter_1_21_5 extends AbstractVersionAdapter {

    @Override
    @Nonnull
    public String getMcVersion() {
        return "1.21.5";
    }

    @Override
    public boolean supportsHappyGhast() {
        return false; // 1.21.5 不支持 Happy Ghast
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
