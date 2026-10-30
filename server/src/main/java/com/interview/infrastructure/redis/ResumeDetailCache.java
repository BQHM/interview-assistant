package com.interview.infrastructure.redis;

import java.time.Duration;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.interview.modules.resume.model.dto.ResumeDetailDTO;

/**
 * 文件功能说明
 * <p>负责简历详情的缓存读写。</p>
 *
 * @author NobuNo
 * @date 2026-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeDetailCache {

    /**
     * 缓存键前缀，格式为：模块:对象:标识。
     */
    private static final String KEY_PREFIX = "resume:detail:";

    /**
     * 缓存有效期。
     * 简历详情是数据库投影，丢失可随时重建，因此不设长 TTL。
     */
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisService redisService;

    /**
     * 功能说明
     * <p>读取简历详情缓存，未命中时回源数据库。</p>
     *
     * @param resumeId 简历编号
     * @param loader 回源方法
     * @return 简历详情
     * @author NobuNo
     * @date 2026-10-28
     */
    public ResumeDetailDTO getOrLoad(Long resumeId, Supplier<ResumeDetailDTO> loader) {
        return redisService.getOrLoad(buildKey(resumeId), TTL, ResumeDetailDTO.class, loader);
    }

    /**
     * 功能说明
     * <p>清除简历详情缓存。</p>
     *
     * @param resumeId 简历编号
     * @author NobuNo
     * @date 2026-10-28
     */
    public void evict(Long resumeId) {
        redisService.delete(buildKey(resumeId));
        log.info("清除简历详情缓存: resumeId={}", resumeId);
    }

    /**
     * 功能说明
     * <p>拼接简历详情缓存键。</p>
     *
     * @param resumeId 简历编号
     * @return 缓存键
     * @author NobuNo
     * @date 2026-10-28
     */
    private String buildKey(Long resumeId) {
        return KEY_PREFIX + resumeId;
    }
}