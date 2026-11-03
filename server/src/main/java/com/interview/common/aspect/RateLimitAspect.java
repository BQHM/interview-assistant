package com.interview.common.aspect;

import com.interview.common.annotation.RateLimit;
import com.interview.common.exception.RateLimitExceededException;
import com.interview.infrastructure.redis.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Locale;

/**
 * 文件功能说明
 * <p>负责拦截带 @RateLimit 注解的方法并按固定窗口计数限流。</p>
 *
 * @author NobuNo
 * @date 2026-11-01
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "ratelimit:";
    private static final String UNKNOWN_VALUE = "unknown";
    private static final String ANONYMOUS_VALUE = "anonymous";

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
        String key = buildKey(joinPoint, rateLimit);
        Duration window = Duration.ofMillis(rateLimit.timeUnit().toMillis(rateLimit.interval()));
        if (!rateLimitService.tryAcquire(key, rateLimit.count(), window)) {
            throw new RateLimitExceededException();
        }
        return joinPoint.proceed();
    }

    /**
     * 功能说明
     * <p>按“类名.方法名:维度:维度取值”的规则拼接限流键。</p>
     *
     * @param joinPoint 目标方法连接点
     * @param rateLimit 目标方法上的限流注解
     * @return 限流键
     * @author NobuNo
     * @date 2026-11-01
     */
    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        String methodKey = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        String dimensionName = rateLimit.dimension().name().toLowerCase(Locale.ROOT);// 小知识：土耳其语环境下，"IP".toLowerCase()会变成"ıp"（无点的 ı）——key就莫名其妙变了
        return KEY_PREFIX + methodKey + ":" + dimensionName + ":" + resolveDimensionValue(rateLimit.dimension());
    }

    /**
     * 功能说明
     * <p>获取客户端 IP，并把 IPv6 的冒号替换成下划线，避免破坏限流键的分段。</p>
     *
     * @return 客户端 IP，取不到时返回 unknown
     * @author NobuNo
     * @date 2026-11-01
     */
    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return UNKNOWN_VALUE;
        }
        return attributes.getRequest().getRemoteAddr().replace(':', '_');
    }

    /**
     * 功能说明
     * <p>根据限流维度取出对应的限流标识。</p>
     *
     * @param dimension 限流维度
     * @return 该维度下的限流标识
     * @author NobuNo
     * @date 2026-11-01
     */
    private String resolveDimensionValue(RateLimit.Dimension dimension) {
        return switch (dimension) {
            case GLOBAL -> "global";
            case IP -> resolveClientIp();
            case USER -> ANONYMOUS_VALUE;
        };
    }

}
