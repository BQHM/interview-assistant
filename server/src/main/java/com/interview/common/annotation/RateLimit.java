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

    /**
     * 窗口长度，与 timeUnit 组合成一个完整的时间窗口
     *
     * @return 窗口长度
     */
    long interval() default 1;

    /**
     * 窗口长度对应的时间单位
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    /**
     * 限流维度，决定限流键按什么粒度区分请求方
     *
     * @return 限流维度
     */
    Dimension dimension() default Dimension.IP;

    /**
     * 限流维度
     */
    enum Dimension {

        /**
         * 全局限流，所有请求共用同一个计数器
         */
        GLOBAL,

        /**
         * 按客户端 IP 限流
         */
        IP,

        /**
         * 按用户限流，取不到用户标识时归入 anonymous
         */
        USER
    }
}
