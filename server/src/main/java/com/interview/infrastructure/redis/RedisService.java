package com.interview.infrastructure.redis;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 文件功能说明
 * <p>负责 Redis 通用读写封装，值统一以字符串形式存放。</p>
 *
 * @author NobuNo
 * @date 2026-10-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    /**
     * 功能说明
     * <p>写入缓存并设置过期时间。</p>
     *
     * @param key   缓存键
     * @param value 字符串值
     * @param ttl   过期时间
     * @author NobuNo
     * @date 2026-10-26
     */
    public void set(String key, String value, Duration ttl) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        bucket.set(value, ttl);
    }

    /**
     * 功能说明
     * <p>读取缓存字符串。</p>
     *
     * @param key 缓存键
     * @return 缓存值，键不存在时返回 null
     * @author NobuNo
     * @date 2026-10-26
     */
    public String get(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.get();
    }

    /**
     * 功能说明
     * <p>删除缓存键。</p>
     *
     * @param key 缓存键
     * @return 是否删除成功
     * @author NobuNo
     * @date 2026-10-26
     */
    public boolean delete(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.delete();
    }

    /**
     * 功能说明
     * <p>判断缓存键是否存在。</p>
     *
     * @param key 缓存键
     * @return 是否存在
     * @author NobuNo
     * @date 2026-10-26
     */
    public boolean exists(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.isExists();
    }

    /**
     * 功能说明
     * <p>读取缓存，未命中时调用 loader 回源并写回缓存。</p>
     *
     * @param key 缓存键
     * @param ttl 过期时间
     * @param type 缓存对象类型
     * @param loader 回源方法
     * @param <T> 缓存对象类型
     * @return 缓存对象
     * @author NobuNo
     * @date 2026-10-26
     */
    public <T> T getOrLoad(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        String cachedJson = get(key);
        if (cachedJson != null) {
            try {
                T cached = objectMapper.readValue(cachedJson, type);
                log.info("缓存命中: key={}", key);
                return cached;
            } catch (JacksonException e) {
                log.warn("缓存内容解析失败，删除后回源: key={}", key, e);
                delete(key);
            }
        } else {
            log.info("缓存未命中: key={}", key);
        }

        T loaded = loader.get();
        if (loaded != null) {
            try {
                set(key, objectMapper.writeValueAsString(loaded), ttl);
            } catch (JacksonException e) {
                log.error("缓存写入失败: key={}", key, e);
            }
        }
        return loaded;
    }
}
