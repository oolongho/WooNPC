package com.oolonghoo.woonpc.npc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NpcPose 姿势测试")
class NpcPoseTest {

    @Test
    @DisplayName("获取配置名称")
    void testGetConfigName() {
        assertEquals("standing", NpcPose.STANDING.getConfigName());
        assertEquals("sleeping", NpcPose.SLEEPING.getConfigName());
        assertEquals("swimming", NpcPose.SWIMMING.getConfigName());
        assertEquals("crouching", NpcPose.CROUCHING.getConfigName());
        assertEquals("sitting", NpcPose.SITTING.getConfigName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"standing", "STANDING", "Standing"})
    @DisplayName("从配置名称解析 - standing")
    void testFromConfigName_Standing(String input) {
        assertEquals(NpcPose.STANDING, NpcPose.fromConfigName(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"crouching", "CROUCHING", "Crouching"})
    @DisplayName("从配置名称解析 - crouching")
    void testFromConfigName_Crouching(String input) {
        assertEquals(NpcPose.CROUCHING, NpcPose.fromConfigName(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sitting", "SITTING", "Sitting"})
    @DisplayName("从配置名称解析 - sitting")
    void testFromConfigName_Sitting(String input) {
        assertEquals(NpcPose.SITTING, NpcPose.fromConfigName(input));
    }

    @Test
    @DisplayName("无效配置名称返回默认值")
    void testFromConfigName_Invalid() {
        assertEquals(NpcPose.STANDING, NpcPose.fromConfigName("invalid"));
        assertEquals(NpcPose.STANDING, NpcPose.fromConfigName(""));
        assertEquals(NpcPose.STANDING, NpcPose.fromConfigName(null));
    }

    @Test
    @DisplayName("从名称解析（支持枚举名称和配置名称）")
    void testGetByName() {
        assertEquals(NpcPose.STANDING, NpcPose.getByName("standing"));
        assertEquals(NpcPose.STANDING, NpcPose.getByName("STANDING"));
        assertEquals(NpcPose.CROUCHING, NpcPose.getByName("crouching"));
        assertEquals(NpcPose.CROUCHING, NpcPose.getByName("CROUCHING"));
        assertEquals(NpcPose.SITTING, NpcPose.getByName("sitting"));
        assertEquals(NpcPose.SITTING, NpcPose.getByName("SITTING"));
    }

    @Test
    @DisplayName("无效名称返回默认值")
    void testGetByName_Invalid() {
        assertEquals(NpcPose.STANDING, NpcPose.getByName("invalid"));
        assertEquals(NpcPose.STANDING, NpcPose.getByName(""));
        assertEquals(NpcPose.STANDING, NpcPose.getByName(null));
    }

    @Test
    @DisplayName("枚举值数量")
    void testValuesCount() {
        assertTrue(NpcPose.values().length >= 5);
    }
}
