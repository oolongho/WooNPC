package com.oolonghoo.woonpc.npc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NpcEffect 效果测试")
class NpcEffectTest {

    @Test
    @DisplayName("获取名称")
    void testGetName() {
        assertEquals("on_fire", NpcEffect.ON_FIRE.getName());
        assertEquals("invisible", NpcEffect.INVISIBLE.getName());
        assertEquals("shaking", NpcEffect.SHAKING.getName());
        assertEquals("silent", NpcEffect.SILENT.getName());
    }

    @Test
    @DisplayName("获取显示名称")
    void testGetDisplayName() {
        assertEquals("着火", NpcEffect.ON_FIRE.getDisplayName());
        assertEquals("隐形", NpcEffect.INVISIBLE.getDisplayName());
        assertEquals("冷颤", NpcEffect.SHAKING.getDisplayName());
        assertEquals("静音", NpcEffect.SILENT.getDisplayName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"on_fire", "ON_FIRE", "On_Fire"})
    @DisplayName("从名称解析 - on_fire")
    void testGetByName_OnFire(String input) {
        assertEquals(NpcEffect.ON_FIRE, NpcEffect.getByName(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invisible", "INVISIBLE", "Invisible"})
    @DisplayName("从名称解析 - invisible")
    void testGetByName_Invisible(String input) {
        assertEquals(NpcEffect.INVISIBLE, NpcEffect.getByName(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"shaking", "SHAKING", "Shaking"})
    @DisplayName("从名称解析 - shaking")
    void testGetByName_Shaking(String input) {
        assertEquals(NpcEffect.SHAKING, NpcEffect.getByName(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"silent", "SILENT", "Silent"})
    @DisplayName("从名称解析 - silent")
    void testGetByName_Silent(String input) {
        assertEquals(NpcEffect.SILENT, NpcEffect.getByName(input));
    }

    @Test
    @DisplayName("无效名称返回 null")
    void testGetByName_Invalid() {
        assertNull(NpcEffect.getByName("invalid"));
        assertNull(NpcEffect.getByName(""));
        assertNull(NpcEffect.getByName(null));
    }

    @Test
    @DisplayName("获取所有名称")
    void testGetNames() {
        String[] names = NpcEffect.getNames();
        
        assertEquals(4, names.length);
        assertArrayEquals(new String[]{"on_fire", "invisible", "shaking", "silent"}, names);
    }

    @Test
    @DisplayName("枚举值数量")
    void testValuesCount() {
        assertEquals(4, NpcEffect.values().length);
    }
}
