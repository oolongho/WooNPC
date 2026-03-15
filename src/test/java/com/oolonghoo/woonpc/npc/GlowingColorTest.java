package com.oolonghoo.woonpc.npc;

import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlowingColor 发光颜色测试")
class GlowingColorTest {

    @Test
    @DisplayName("获取配置名称")
    void testGetConfigName() {
        assertEquals("disabled", GlowingColor.DISABLED.getConfigName());
        assertEquals("black", GlowingColor.BLACK.getConfigName());
        assertEquals("white", GlowingColor.WHITE.getConfigName());
        assertEquals("red", GlowingColor.RED.getConfigName());
        assertEquals("gold", GlowingColor.GOLD.getConfigName());
    }

    @Test
    @DisplayName("获取 Adventure 颜色")
    void testGetAdventureColor() {
        assertNull(GlowingColor.DISABLED.getAdventureColor());
        assertEquals(NamedTextColor.BLACK, GlowingColor.BLACK.getAdventureColor());
        assertEquals(NamedTextColor.WHITE, GlowingColor.WHITE.getAdventureColor());
        assertEquals(NamedTextColor.RED, GlowingColor.RED.getAdventureColor());
        assertEquals(NamedTextColor.GOLD, GlowingColor.GOLD.getAdventureColor());
    }

    @Test
    @DisplayName("检查是否禁用")
    void testIsDisabled() {
        assertTrue(GlowingColor.DISABLED.isDisabled());
        assertFalse(GlowingColor.WHITE.isDisabled());
        assertFalse(GlowingColor.RED.isDisabled());
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "DISABLED", "Disabled"})
    @DisplayName("从配置名称解析 - disabled")
    void testFromConfigName_Disabled(String input) {
        assertEquals(GlowingColor.DISABLED, GlowingColor.fromConfigName(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"white", "WHITE", "White"})
    @DisplayName("从配置名称解析 - white")
    void testFromConfigName_White(String input) {
        assertEquals(GlowingColor.WHITE, GlowingColor.fromConfigName(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"red", "RED", "Red"})
    @DisplayName("从配置名称解析 - red")
    void testFromConfigName_Red(String input) {
        assertEquals(GlowingColor.RED, GlowingColor.fromConfigName(input));
    }

    @Test
    @DisplayName("无效配置名称返回默认值")
    void testFromConfigName_Invalid() {
        assertEquals(GlowingColor.WHITE, GlowingColor.fromConfigName("invalid"));
        assertEquals(GlowingColor.DISABLED, GlowingColor.fromConfigName(""));
        assertEquals(GlowingColor.DISABLED, GlowingColor.fromConfigName(null));
    }

    @Test
    @DisplayName("从 Adventure 颜色解析")
    void testFromAdventureColor() {
        assertEquals(GlowingColor.DISABLED, GlowingColor.fromAdventureColor(null));
        assertEquals(GlowingColor.BLACK, GlowingColor.fromAdventureColor(NamedTextColor.BLACK));
        assertEquals(GlowingColor.WHITE, GlowingColor.fromAdventureColor(NamedTextColor.WHITE));
        assertEquals(GlowingColor.RED, GlowingColor.fromAdventureColor(NamedTextColor.RED));
    }

    @Test
    @DisplayName("枚举值数量")
    void testValuesCount() {
        assertEquals(17, GlowingColor.values().length);
    }

    @Test
    @DisplayName("所有颜色都有配置名称")
    void testAllHaveConfigName() {
        for (GlowingColor color : GlowingColor.values()) {
            assertNotNull(color.getConfigName());
            assertFalse(color.getConfigName().isEmpty());
        }
    }
}
