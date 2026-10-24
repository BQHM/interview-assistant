package com.interview.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 文件功能说明
 * <p>负责验证 Redis 客户端装配与基础读写能力。</p>
 *
 * @author NobuNo
 * @date 2026-10-24
 */
@SpringBootTest
class RedisConnectivityTest {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 功能说明
     * <p>写入后应能读回同一个值。</p>
     */
    @Test
    void setAndGet_shouldWriteAndReadBackValue() {
        RBucket<String> bucket = redissonClient.getBucket("test:ping");
        bucket.set("pong", Duration.ofMinutes(1));

        assertThat(bucket.get()).isEqualTo("pong");
    }

    /**
     * 功能说明
     * <p>过期时间应真实生效。</p>
     */
    @Test
    void setAndGet_shouldApplyTtl() {
        RBucket<String> bucket = redissonClient.getBucket("test:ttl");
        bucket.set("temporary", Duration.ofMinutes(1));

        assertThat(bucket.remainTimeToLive()).isBetween(1L, Duration.ofMinutes(1).toMillis());
    }
}