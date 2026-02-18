package com.ordish.ai.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 拦截我们自定义的限流异常（等下会写）
    @ExceptionHandler(RuntimeException.class)
    public CommonResult<String> handleRuntimeException(RuntimeException e) {
        log.error("系统运行异常: {}", e.getMessage());
        return CommonResult.error(500, e.getMessage());
    }

    // 拦截所有未知异常（兜底）
    @ExceptionHandler(Exception.class)
    public CommonResult<String> handleException(Exception e) {
        log.error("系统未知异常", e);
        return CommonResult.error(500, "服务器开小差了，请稍后再试");
    }
}