package com.ordish.ai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
public @interface RateLimit {
    int time() default 60; // 默认时间窗口（秒）
    int count() default 10; // 默认最大访问次数
}