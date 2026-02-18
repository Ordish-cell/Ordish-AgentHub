package com.ordish.ai.common;

import lombok.Data;

@Data
public class CommonResult<T> {
    private Integer code;
    private String message;
    private T data;

    // 成功时的快速返回
    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // 失败时的快速返回
    public static <T> CommonResult<T> error(Integer code, String message) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}