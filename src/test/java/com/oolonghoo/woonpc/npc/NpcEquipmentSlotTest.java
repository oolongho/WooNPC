package com.oolonghoo.woonpc.npc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NpcEquipmentSlot 装备槽位测试")
class NpcEquipmentSlotTest {

    @Test
    @DisplayName("获取 NMS 槽位名称")
    void testGetNmsName() {
        assertEquals("mainhand", NpcEquipmentSlot.MAIN_HAND.getNmsName());
        assertEquals("offhand", NpcEquipmentSlot.OFF_HAND.getNmsName());
        assertEquals("head", NpcEquipmentSlot.HEAD.getNmsName());
        assertEquals("chest", NpcEquipmentSlot.CHEST.getNmsName());
        assertEquals("legs", NpcEquipmentSlot.LEGS.getNmsName());
        assertEquals("feet", NpcEquipmentSlot.FEET.getNmsName());
        assertEquals("body", NpcEquipmentSlot.BODY.getNmsName());
    }

    @Test
    @DisplayName("获取配置名称")
    void testGetConfigName() {
        assertEquals("mainhand", NpcEquipmentSlot.MAIN_HAND.getConfigName());
        assertEquals("offhand", NpcEquipmentSlot.OFF_HAND.getConfigName());
        assertEquals("head", NpcEquipmentSlot.HEAD.getConfigName());
        assertEquals("chest", NpcEquipmentSlot.CHEST.getConfigName());
        assertEquals("legs", NpcEquipmentSlot.LEGS.getConfigName());
        assertEquals("feet", NpcEquipmentSlot.FEET.getConfigName());
        assertEquals("body", NpcEquipmentSlot.BODY.getConfigName());
    }

    @Test
    @DisplayName("从配置名称解析")
    void testFromConfigName() {
        assertEquals(NpcEquipmentSlot.MAIN_HAND, NpcEquipmentSlot.fromConfigName("mainhand"));
        assertEquals(NpcEquipmentSlot.MAIN_HAND, NpcEquipmentSlot.fromConfigName("MAINHAND"));
        assertEquals(NpcEquipmentSlot.MAIN_HAND, NpcEquipmentSlot.fromConfigName("MainHand"));
        assertEquals(NpcEquipmentSlot.OFF_HAND, NpcEquipmentSlot.fromConfigName("offhand"));
        assertEquals(NpcEquipmentSlot.HEAD, NpcEquipmentSlot.fromConfigName("head"));
        assertEquals(NpcEquipmentSlot.CHEST, NpcEquipmentSlot.fromConfigName("chest"));
        assertEquals(NpcEquipmentSlot.LEGS, NpcEquipmentSlot.fromConfigName("legs"));
        assertEquals(NpcEquipmentSlot.FEET, NpcEquipmentSlot.fromConfigName("feet"));
        assertEquals(NpcEquipmentSlot.BODY, NpcEquipmentSlot.fromConfigName("body"));
    }

    @Test
    @DisplayName("无效配置名称返回 null")
    void testFromConfigName_Invalid() {
        assertNull(NpcEquipmentSlot.fromConfigName("invalid"));
        assertNull(NpcEquipmentSlot.fromConfigName(""));
        assertNull(NpcEquipmentSlot.fromConfigName(null));
    }

    @Test
    @DisplayName("从索引解析")
    void testFromIndex() {
        assertEquals(NpcEquipmentSlot.MAIN_HAND, NpcEquipmentSlot.fromIndex(0));
        assertEquals(NpcEquipmentSlot.OFF_HAND, NpcEquipmentSlot.fromIndex(1));
        assertEquals(NpcEquipmentSlot.HEAD, NpcEquipmentSlot.fromIndex(2));
        assertEquals(NpcEquipmentSlot.CHEST, NpcEquipmentSlot.fromIndex(3));
        assertEquals(NpcEquipmentSlot.LEGS, NpcEquipmentSlot.fromIndex(4));
        assertEquals(NpcEquipmentSlot.FEET, NpcEquipmentSlot.fromIndex(5));
        assertEquals(NpcEquipmentSlot.BODY, NpcEquipmentSlot.fromIndex(6));
    }

    @Test
    @DisplayName("无效索引返回 null")
    void testFromIndex_Invalid() {
        assertNull(NpcEquipmentSlot.fromIndex(-1));
        assertNull(NpcEquipmentSlot.fromIndex(7));
        assertNull(NpcEquipmentSlot.fromIndex(100));
    }

    @Test
    @DisplayName("枚举值数量")
    void testValuesCount() {
        assertEquals(7, NpcEquipmentSlot.values().length);
    }
}
