package com.oolonghoo.woonpc.version;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VersionAdapterFactory 版本适配器工厂测试")
class VersionAdapterFactoryTest {

    @BeforeEach
    void setUp() {
        VersionAdapterFactory.reset();
    }

    @Test
    @DisplayName("检查支持的版本")
    void testIsVersionSupported() {
        assertTrue(VersionAdapterFactory.isVersionSupported("1.21.2"));
        assertTrue(VersionAdapterFactory.isVersionSupported("1.21.3"));
        assertTrue(VersionAdapterFactory.isVersionSupported("1.21.4"));
        assertTrue(VersionAdapterFactory.isVersionSupported("1.21.5"));
        assertTrue(VersionAdapterFactory.isVersionSupported("1.21.6"));
        assertTrue(VersionAdapterFactory.isVersionSupported("1.21.7"));
        assertTrue(VersionAdapterFactory.isVersionSupported("1.21.8"));
        assertTrue(VersionAdapterFactory.isVersionSupported("1.21.9"));
        assertTrue(VersionAdapterFactory.isVersionSupported("1.21.10"));
        assertTrue(VersionAdapterFactory.isVersionSupported("1.21.11"));
        
        assertFalse(VersionAdapterFactory.isVersionSupported("1.20.4"));
        assertFalse(VersionAdapterFactory.isVersionSupported("1.21.1"));
        assertFalse(VersionAdapterFactory.isVersionSupported("1.22.0"));
        assertFalse(VersionAdapterFactory.isVersionSupported("invalid"));
    }

    @Test
    @DisplayName("获取指定版本的适配器")
    void testGetAdapterByVersion() {
        assertNotNull(VersionAdapterFactory.getAdapter("1.21.2"));
        assertNotNull(VersionAdapterFactory.getAdapter("1.21.3"));
        assertNotNull(VersionAdapterFactory.getAdapter("1.21.4"));
        assertNotNull(VersionAdapterFactory.getAdapter("1.21.5"));
        assertNotNull(VersionAdapterFactory.getAdapter("1.21.6"));
        assertNotNull(VersionAdapterFactory.getAdapter("1.21.9"));
        assertNotNull(VersionAdapterFactory.getAdapter("1.21.11"));
        
        assertNull(VersionAdapterFactory.getAdapter("1.20.4"));
        assertNull(VersionAdapterFactory.getAdapter("invalid"));
    }

    @Test
    @DisplayName("适配器缓存测试")
    void testAdapterCaching() {
        VersionAdapter adapter1 = VersionAdapterFactory.getAdapter("1.21.11");
        VersionAdapter adapter2 = VersionAdapterFactory.getAdapter("1.21.11");
        
        assertSame(adapter1, adapter2, "相同版本应该返回缓存的适配器实例");
    }

    @Test
    @DisplayName("重置工厂状态")
    void testReset() {
        VersionAdapter adapter1 = VersionAdapterFactory.getAdapter("1.21.11");
        assertFalse(VersionAdapterFactory.isInitialized());
        
        VersionAdapterFactory.reset();
        
        assertFalse(VersionAdapterFactory.isInitialized());
    }

    @Test
    @DisplayName("不支持的版本异常")
    void testUnsupportedVersionException() {
        VersionAdapterFactory.UnsupportedVersionException exception =
                new VersionAdapterFactory.UnsupportedVersionException("1.0.0");
        
        assertEquals("1.0.0", exception.getVersion());
        assertTrue(exception.getMessage().contains("1.0.0"));
        assertTrue(exception.getMessage().contains("Unsupported"));
    }

    @Test
    @DisplayName("获取调试信息")
    void testGetDebugInfo() {
        String debugInfo = VersionAdapterFactory.getDebugInfo();
        
        assertNotNull(debugInfo);
        assertTrue(debugInfo.contains("VersionAdapterFactory"));
        assertTrue(debugInfo.contains("Current Version"));
        assertTrue(debugInfo.contains("Is Supported"));
        assertTrue(debugInfo.contains("Supported Versions"));
    }

    @Test
    @DisplayName("版本适配器共享测试")
    void testSharedAdapters() {
        VersionAdapter adapter21_2 = VersionAdapterFactory.getAdapter("1.21.2");
        VersionAdapter adapter21_3 = VersionAdapterFactory.getAdapter("1.21.3");
        
        assertSame(adapter21_2.getClass(), adapter21_3.getClass(), 
                "1.21.2 和 1.21.3 应该使用相同的适配器类");
        
        VersionAdapter adapter21_6 = VersionAdapterFactory.getAdapter("1.21.6");
        VersionAdapter adapter21_7 = VersionAdapterFactory.getAdapter("1.21.7");
        VersionAdapter adapter21_8 = VersionAdapterFactory.getAdapter("1.21.8");
        
        assertSame(adapter21_6.getClass(), adapter21_7.getClass(), 
                "1.21.6, 1.21.7, 1.21.8 应该使用相同的适配器类");
        assertSame(adapter21_6.getClass(), adapter21_8.getClass(), 
                "1.21.6, 1.21.7, 1.21.8 应该使用相同的适配器类");
    }
}
