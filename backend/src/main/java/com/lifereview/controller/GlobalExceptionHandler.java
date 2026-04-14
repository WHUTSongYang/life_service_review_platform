package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * <p>作用于所有控制器：将业务异常与参数校验异常转为统一 {@link ApiResponse} 失败格式。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 {@link IllegalArgumentException}（参数错误、权限提示等）。
     *
     * @param e 业务侧抛出的非法参数异常
     * @return {@code success=false} 且 message 为异常信息的响应体
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleBusinessException(IllegalArgumentException e) {
        return ApiResponse.fail(e.getMessage());
    }

    /**
     * 处理 {@code @Valid} 校验失败（如 {@code @NotNull}、{@code @NotBlank}）。
     *
     * @param e Spring 绑定的校验异常
     * @return {@code success=false}，优先返回首个字段错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidateException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数错误";
        return ApiResponse.fail(msg);
    }
}
