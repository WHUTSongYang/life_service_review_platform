package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 统一捕获业务异常和参数校验异常，返回 ApiResponse.fail 格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常（如参数错误、权限不足等）
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleBusinessException(IllegalArgumentException e) {
        return ApiResponse.fail(e.getMessage());
    }

    // 处理 @Valid 参数校验异常（如 @NotNull、@NotBlank 等）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidateException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数错误";
        return ApiResponse.fail(msg);
    }
}
