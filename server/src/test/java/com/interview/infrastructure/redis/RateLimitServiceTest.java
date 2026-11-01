package com.interview.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 文件功能说明
 * <p>负责验证固定窗口限流的计数、拒绝与窗口重置行为。</p>
 *
 * @author NobuNo
 * @date 2026-11-01
 */
@SpringBootTest
class RateLimitServiceTest {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 功能说明
     * <p>限额内的请求应全部放行。</p>
     */
    @Test
    void tryAcquire_shouldAllowRequestWithinLimit() {
        String key = "test:ratelimit:" + UUID.randomUUID();
        int limit = 3;
        Duration window = Duration.ofMinutes(1);

        assertThat(rateLimitService.tryAcquire(key, limit, window)).isTrue();
        assertThat(rateLimitService.tryAcquire(key, limit, window)).isTrue();
        assertThat(rateLimitService.tryAcquire(key, limit, window)).isTrue();
    }

    /**
     * 功能说明
     * <p>超过限额的请求应被拒绝。</p>
     */
    @Test
    void tryAcquire_shouldRejectRequestOverLimit() {
        String key = "test:ratelimit:" + UUID.randomUUID();
        int limit = 3;
        Duration window = Duration.ofMinutes(1);

        for (int requestIndex = 0; requestIndex < limit; requestIndex++) {
            rateLimitService.tryAcquire(key, limit, window);
        }

        assertThat(rateLimitService.tryAcquire(key, limit, window)).isFalse();
    }

    /**
     * 功能说明
     * <p>窗口到期后计数应重新开始。</p>
     */
    @Test
    void tryAcquire_shouldResetCountAfterWindowExpired() throws InterruptedException {
        String key = "test:ratelimit:" + UUID.randomUUID();
        int limit = 1;
        Duration window = Duration.ofSeconds(1);

        assertThat(rateLimitService.tryAcquire(key, limit, window)).isTrue();
        assertThat(rateLimitService.tryAcquire(key, limit, window)).isFalse();

        Thread.sleep(1200);

        assertThat(rateLimitService.tryAcquire(key, limit, window)).isTrue();
    }

    /**
     * 功能说明
     * <p>后续请求不应重置窗口过期时间，避免窗口被无限延长。</p>
     */
    @Test
    void tryAcquire_shouldNotResetTtlOnSubsequentRequest() throws InterruptedException {
        String key = "test:ratelimit:" + UUID.randomUUID();
        int limit = 10;
        Duration window = Duration.ofSeconds(10);

        rateLimitService.tryAcquire(key, limit, window);
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        long firstTtl = counter.remainTimeToLive();

        Thread.sleep(600);

        rateLimitService.tryAcquire(key, limit, window);
        long secondTtl = counter.remainTimeToLive();

        assertThat(secondTtl).isLessThan(firstTtl);
    }
}
