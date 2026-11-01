package com.interview.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 文件功能说明
 * <p>负责基于 Redis 的固定窗口限流计数。</p>
 *
 * @author NobuNo
 * @date 2026-11-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedissonClient redissonClient;

    /**
     * 功能说明
     * <p>判断本次请求是否允许通过。固定窗口实现，INCR 与 EXPIRE 非原子，后续替换为 Lua 脚本。</p>
     *
     * @param key    限流键
     * @param limit  窗口内允许的最大次数
     * @param window 窗口大小
     * @return 是否允许通过
     * @author NobuNo
     * @date 2026-11-01
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        RAtomicLong counter = redissonClient.getAtomicLong(key);

        long current = counter.incrementAndGet();
        if (current == 1) {
            counter.expire(window);// 给请求设置窗口时间
        }

        if (current > limit) {
            log.warn("触发限流: key={}, current={}, limit={}", key, current, limit);
            return false;
        }
        return true;
    }
}
