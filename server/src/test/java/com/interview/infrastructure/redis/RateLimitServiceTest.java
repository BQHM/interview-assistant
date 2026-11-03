package com.interview.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 文件功能说明
 * <p>负责验证滑动窗口限流的计数、拒绝、窗口重置与拒绝不计数行为。</p>
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
     * <p>被拒绝的请求不应写入窗口记录，集合内的记录数始终等于允许次数。</p>
     */
    @Test
    void tryAcquire_shouldNotRecordRejectedRequest() {
        String key = "test:ratelimit:" + UUID.randomUUID();
        int limit = 2;
        Duration window = Duration.ofMinutes(5);

        assertThat(rateLimitService.tryAcquire(key, limit, window)).isTrue();
        assertThat(rateLimitService.tryAcquire(key, limit, window)).isTrue();
        assertThat(rateLimitService.tryAcquire(key, limit, window)).isFalse();
        assertThat(rateLimitService.tryAcquire(key, limit, window)).isFalse();

        assertThat(redissonClient.getScoredSortedSet(key, StringCodec.INSTANCE).size()).isEqualTo(limit);
    }
}
