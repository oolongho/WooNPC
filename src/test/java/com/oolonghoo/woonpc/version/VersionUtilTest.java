package com.oolonghoo.woonpc.version;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("VersionUtil 版本工具类测试")
@SuppressWarnings({"java:S1172", "java:S1607"})
class VersionUtilTest {

    @Test
    @DisplayName("获取支持的版本列表")
    void testGetSupportedVersions() {
        var versions = VersionUtil.getSupportedVersions();
        
        assertNotNull(versions);
        assertFalse(versions.isEmpty());
        assertEquals(10, versions.size());
        assertTrue(versions.contains("1.21.2"));
        assertTrue(versions.contains("1.21.11"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11"})
    @DisplayName("支持的版本应该返回 true")
    void testIsVersionSupported_True(String version) {
        assertTrue(VersionUtil.isVersionSupported(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.20.4", "1.21.0", "1.21.1", "1.22.0", "2.0.0", "", "invalid", "1.21", "1.21.12"})
    @DisplayName("不支持的版本应该返回 false")
    void testIsVersionSupported_False(String version) {
        assertFalse(VersionUtil.isVersionSupported(version));
    }

    @Test
    @DisplayName("null 版本应该返回 false")
    void testIsVersionSupported_Null() {
        assertFalse(VersionUtil.isVersionSupported(null));
    }

    @Test
    @DisplayName("获取最小和最大版本")
    void testMinMaxVersions() {
        assertEquals("1.21.2", VersionUtil.MIN_VERSION);
        assertEquals("1.21.11", VersionUtil.MAX_VERSION);
    }

    @Test
    @DisplayName("版本范围枚举测试")
    void testVersionRange() {
        var earlyRange = VersionUtil.VersionRange.EARLY;
        assertEquals(2, earlyRange.getMinPatch());
        assertEquals(3, earlyRange.getMaxPatch());
        
        var latestRange = VersionUtil.VersionRange.LATEST;
        assertEquals(9, latestRange.getMinPatch());
        assertEquals(11, latestRange.getMaxPatch());
    }

    @ParameterizedTest
    @MethodSource("patchToRangeProvider")
    @DisplayName("根据补丁版本获取版本范围")
    void testFromPatch(int patch, VersionUtil.VersionRange expected) {
        assertEquals(expected, VersionUtil.VersionRange.fromPatch(patch));
    }

    static Stream<Arguments> patchToRangeProvider() {
        return Stream.of(
            Arguments.of(2, VersionUtil.VersionRange.EARLY),
            Arguments.of(3, VersionUtil.VersionRange.EARLY),
            Arguments.of(5, VersionUtil.VersionRange.MIDDLE),
            Arguments.of(6, VersionUtil.VersionRange.LATE),
            Arguments.of(7, VersionUtil.VersionRange.LATE),
            Arguments.of(8, VersionUtil.VersionRange.LATE),
            Arguments.of(9, VersionUtil.VersionRange.LATEST),
            Arguments.of(10, VersionUtil.VersionRange.LATEST),
            Arguments.of(11, VersionUtil.VersionRange.LATEST),
            Arguments.of(12, VersionUtil.VersionRange.LATEST)
        );
    }
}
