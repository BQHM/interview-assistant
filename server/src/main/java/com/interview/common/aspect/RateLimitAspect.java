package com.interview.common.aspect;

import com.interview.common.annotation.RateLimit;
import com.interview.common.exception.RateLimitExceededException;
import com.interview.infrastructure.redis.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    /**
     * 功能说明
     * <p>执行目标方法前先做限流检查，超限时抛出限流异常。</p>
     *
     * @param joinPoint 目标方法连接点
     * @param rateLimit 目标方法上的限流注解
     * @return 目标方法的返回值
     * @throws Throwable 目标方法执行时抛出的异常
     * @author NobuNo
     * @date 2026-11-01
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = "ratelimit:temp";
        Duration window = Duration.ofMillis(rateLimit.timeUnit().toMillis(rateLimit.interval()));
        if (!rateLimitService.tryAcquire(key, rateLimit.count(), window)) {
            throw new RateLimitExceededException();
        }
        return joinPoint.proceed();
    }
}
