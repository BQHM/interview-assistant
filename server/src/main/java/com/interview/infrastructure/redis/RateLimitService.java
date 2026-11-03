package com.interview.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 文件功能说明
 * <p>负责基于 Redis 与 Lua 脚本的滑动窗口限流。</p>
 *
 * @author NobuNo
 * @date 2026-11-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String LUA_SCRIPT;

    private final RedissonClient redissonClient;

    static {
        try {
            LUA_SCRIPT = new String(new ClassPathResource("scripts/rate_limit.lua").getContentAsByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("加载限流 Lua 脚本失败", e);
        }
    }

    /**
     * 功能说明
     * <p>判断本次请求是否允许通过。Lua 脚本内原子完成过期记录清理、计数与写入。</p>
     *
     * @param key    限流键
     * @param limit  窗口内允许的最大次数
     * @param window 窗口大小
     * @return 是否允许通过
     * @author NobuNo
     * @date 2026-11-01
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
        long windowMs = window.toMillis();
        Object result = rScript.eval(
                // 脚本会执行 ZADD 和 PEXPIRE，属于写操作，必须 READ_WRITE；只读脚本才用 READ_ONLY
                RScript.Mode.READ_WRITE,
                // 直接传脚本内容而不是 SHA，Redisson 自己管理 SHA 缓存并处理脚本不存在的回退
                LUA_SCRIPT,
                // Lua 的 return 1 或 return 0 会被转换成 Long，不要写成 INTEGER
                RScript.ReturnType.LONG,
                // KEYS，按顺序对应脚本里的 KEYS[1]
                List.of(key),
                // ARGV[1]：当前时间戳（毫秒），脚本用它算出需要清理的过期记录
                String.valueOf(System.currentTimeMillis()),
                // ARGV[2]：窗口长度（毫秒），决定往前看多远
                String.valueOf(windowMs),
                // ARGV[3]：窗口内允许的最大次数
                String.valueOf(limit),
                // ARGV[4]：请求唯一标识，作为 ZSet 成员，避免同一毫秒的请求互相覆盖
                UUID.randomUUID().toString()
        );

        boolean acquired = Long.valueOf(1L).equals(result);
        if (!acquired) {
            log.warn("触发限流: key={}, limit={}, windowMs={}", key, limit, windowMs);
        }
        return acquired;
    }
}
