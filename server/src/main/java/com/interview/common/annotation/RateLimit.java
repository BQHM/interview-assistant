package com.interview.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 文件功能说明
 * <p>负责声明方法级限流规则，由限流切面读取并执行。</p>
 *
 * @author NobuNo
 * @date 2026-11-01
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 窗口内允许的最大请求次数
     *
     * @return 最大请求次数
     */
    int count() default 10;

    long interval() default 1;

    TimeUnit timeUnit() default TimeUnit.MINUTES;

    Dimension dimension() default Dimension.IP;

    enum Dimension {
        GLOBAL,
        IP,
        USER
    }
}
