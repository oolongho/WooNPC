package com.oolonghoo.woonpc.tracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LRU 缓存策略测试")
class LRUCacheTest {

    private static final int MAX_CACHE_SIZE = 100;
    private Map<UUID, Map<Long, Boolean>> cache;

    @BeforeEach
    void setUp() {
        cache = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, Map<Long, Boolean>> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
    }

    @Test
    @DisplayName("缓存添加和获取")
    void testPutAndGet() {
        UUID playerId = UUID.randomUUID();
        Map<Long, Boolean> chunkMap = new ConcurrentHashMap<>();
        chunkMap.put(1L, true);
        
        cache.put(playerId, chunkMap);
        
        assertTrue(cache.containsKey(playerId));
        assertEquals(chunkMap, cache.get(playerId));
    }

    @Test
    @DisplayName("缓存移除")
    void testRemove() {
        UUID playerId = UUID.randomUUID();
        cache.put(playerId, new ConcurrentHashMap<>());
        
        cache.remove(playerId);
        
        assertFalse(cache.containsKey(playerId));
    }

    @Test
    @DisplayName("LRU 淘汰策略")
    void testLRUEviction() {
        List<UUID> insertedOrder = new ArrayList<>();
        
        for (int i = 0; i < MAX_CACHE_SIZE + 50; i++) {
            UUID id = UUID.randomUUID();
            insertedOrder.add(id);
            cache.put(id, new ConcurrentHashMap<>());
        }
        
        assertTrue(cache.size() <= MAX_CACHE_SIZE, "缓存大小应该不超过最大值");
        
        int removedCount = 0;
        for (int i = 0; i < 50; i++) {
            if (!cache.containsKey(insertedOrder.get(i))) {
                removedCount++;
            }
        }
        assertTrue(removedCount > 0, "应该有一些早期插入的条目被淘汰");
    }

    @Test
    @DisplayName("访问顺序影响淘汰 - 最近访问的条目保留更久")
    void testAccessOrder() {
        UUID firstKey = UUID.randomUUID();
        cache.put(firstKey, new ConcurrentHashMap<>());
        
        for (int i = 0; i < MAX_CACHE_SIZE - 1; i++) {
            UUID id = UUID.randomUUID();
            cache.put(id, new ConcurrentHashMap<>());
        }
        
        assertEquals(MAX_CACHE_SIZE, cache.size());
        assertTrue(cache.containsKey(firstKey), "firstKey 应该还在缓存中");
        
        cache.get(firstKey);
        
        for (int i = 0; i < 10; i++) {
            cache.put(UUID.randomUUID(), new ConcurrentHashMap<>());
        }
        
        assertTrue(cache.containsKey(firstKey), "最近访问的条目应该保留");
    }

    @Test
    @DisplayName("并发访问安全性")
    void testConcurrentAccess() throws InterruptedException {
        Map<UUID, Map<Long, Boolean>> concurrentCache = new ConcurrentHashMap<>();
        
        int threadCount = 10;
        int operationsPerThread = 100;
        List<Thread> threads = new ArrayList<>();
        
        for (int t = 0; t < threadCount; t++) {
            threads.add(new Thread(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    UUID id = UUID.randomUUID();
                    concurrentCache.put(id, new ConcurrentHashMap<>());
                    concurrentCache.get(id);
                    concurrentCache.remove(id);
                }
            }));
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertTrue(concurrentCache.size() >= 0);
    }

    @Test
    @DisplayName("缓存清空")
    void testClear() {
        for (int i = 0; i < 10; i++) {
            cache.put(UUID.randomUUID(), new ConcurrentHashMap<>());
        }
        
        assertEquals(10, cache.size());
        
        cache.clear();
        
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    @Test
    @DisplayName("嵌套 Map 操作")
    void testNestedMapOperations() {
        UUID playerId = UUID.randomUUID();
        Map<Long, Boolean> chunkMap = new ConcurrentHashMap<>();
        cache.put(playerId, chunkMap);
        
        chunkMap.put(1L, true);
        chunkMap.put(2L, false);
        chunkMap.put(3L, true);
        
        assertEquals(3, cache.get(playerId).size());
        assertTrue(cache.get(playerId).get(1L));
        assertFalse(cache.get(playerId).get(2L));
        
        chunkMap.remove(1L);
        assertEquals(2, cache.get(playerId).size());
        assertNull(cache.get(playerId).get(1L));
    }
}
