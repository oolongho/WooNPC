package com.oolonghoo.woonpc.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * 简单的速率限制器
 * 用于限制网络请求频率
 * o
 */
public class RateLimiter {
    
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private final long minIntervalMs;
    private final int maxRequestsPerInterval;
    private final Map<String, Integer> requestCount = new ConcurrentHashMap<>();
    
    /**
     * 创建速率限制器
     * 
     * @param minIntervalMs 最小请求间隔（毫秒）
     * @param maxRequestsPerInterval 每个时间窗口内最大请求数
     */
    public RateLimiter(long minIntervalMs, int maxRequestsPerInterval) {
        this.minIntervalMs = minIntervalMs;
        this.maxRequestsPerInterval = maxRequestsPerInterval;
    }
    
    /**
     * 创建固定频率的速率限制器
     * 
     * @param maxRequests 最大请求数
     * @param timeUnit 时间单位
     * @return 速率限制器
     */
    public static RateLimiter create(int maxRequests, TimeUnit timeUnit) {
        long intervalMs = timeUnit.toMillis(1);
        return new RateLimiter(intervalMs / maxRequests, maxRequests);
    }
    
    /**
     * 尝试获取请求许可
     * 
     * @param key 请求标识（如 API 名称、玩家名等）
     * @return 是否允许请求
     */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        
        // 清理过期的计数
        requestCount.compute(key, (k, count) -> {
            if (count == null || count >= maxRequestsPerInterval) {
                // 重置计数
                lastRequestTime.put(key, now);
                return 1;
            }
            return count + 1;
        });
        
        // 检查时间间隔
        Long lastTime = lastRequestTime.get(key);
        if (lastTime == null) {
            lastRequestTime.put(key, now);
            return true;
        }
        
        long elapsed = now - lastTime;
        if (elapsed < minIntervalMs) {
            // 时间间隔太短，拒绝请求
            return false;
        }
        
        // 允许请求
        lastRequestTime.put(key, now);
        return true;
    }
    
    /**
     * 等待并获取请求许可（阻塞）
     * 
     * @param key 请求标识
     * @throws InterruptedException 如果等待被中断
     */
    @SuppressWarnings("java:S2142") // 短暂睡眠用于限流重试，中断不影响业务逻辑
    public void acquire(String key) throws InterruptedException {
        while (!tryAcquire(key)) {
            // 等待一小段时间后重试
            Thread.sleep(50);
        }
    }
    
    /**
     * 清除指定 key 的限制
     * 
     * @param key 请求标识
     */
    public void reset(String key) {
        lastRequestTime.remove(key);
        requestCount.remove(key);
    }
    
    /**
     * 清除所有限制
     */
    public void resetAll() {
        lastRequestTime.clear();
        requestCount.clear();
    }
    
    /**
     * 获取剩余等待时间（毫秒）
     * 
     * @param key 请求标识
     * @return 剩余等待时间，如果不需要等待则返回 0
     */
    public long getRemainingWaitMs(String key) {
        Long lastTime = lastRequestTime.get(key);
        if (lastTime == null) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - lastTime;
        return Math.max(0, minIntervalMs - elapsed);
    }
}
