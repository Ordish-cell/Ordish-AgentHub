package com.ordish.ai.aspect;

import com.ordish.ai.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    // 面试官问：为什么用Lua脚本？
    // 回答：因为检查次数和增加次数必须是“原子操作”，否则高并发下会超卖/失效。Lua脚本在Redis中执行是单线程原子性的。
    private static final String LUA_SCRIPT =
            "local key = KEYS[1] " +
                    "local count = tonumber(ARGV[1]) " +
                    "local time = tonumber(ARGV[2]) " +
                    "local current = redis.call('get', key) " +
                    "if current and tonumber(current) >= count then " +
                    "   return 0 " + // 超过限制
                    "end " +
                    "current = redis.call('incr', key) " +
                    "if tonumber(current) == 1 then " +
                    "   redis.call('expire', key, time) " + // 第一次访问，设置过期时间
                    "end " +
                    "return 1 "; // 允许访问

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 1. 获取请求的真实 IP 地址
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getRemoteAddr();

        // 2. 拼接 Redis Key (格式：RateLimit:接口名:IP)
        String methodName = joinPoint.getSignature().getName();
        String redisKey = "RateLimit:" + methodName + ":" + ip;

        // 3. 执行 Lua 脚本
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(redisKey),
                String.valueOf(rateLimit.count()),
                String.valueOf(rateLimit.time()));

        // 4. 判断是否被限流
        if (result == null || result == 0L) {
            log.warn("触发限流，IP: {}, 接口: {}", ip, methodName);
            throw new RuntimeException("您的访问过于频繁，请稍后再试！"); // 这里抛出的异常会被前面的 GlobalExceptionHandler 优雅拦截
        }

        // 5. 放行，执行原本的方法
        return joinPoint.proceed();
    }
}