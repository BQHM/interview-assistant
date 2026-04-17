package com.interview.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 轻量字段元数据注解，用于给 DTO 字段补充中文名称和说明，
 * 方便开发、联调和阅读代码时快速理解字段语义。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldMeta {

    /**
     * 字段中文名称。
     */
    String name();

    /**
     * 字段说明。
     */
    String desc() default "";
}
