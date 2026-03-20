// 包声明：公共类所在包
package com.lifereview.common;

// Lombok：生成全参构造器
import lombok.AllArgsConstructor;
// Lombok：生成 getter/setter/toString/equals/hashCode
import lombok.Data;
// Lombok：生成无参构造器
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应封装。
 * success：是否成功。message：提示信息，成功时多为 "ok"。data：业务数据，失败时可为 null。
 * 提供 ok(data) 和 fail(message) 静态工厂方法。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    // 请求是否成功
    private boolean success;
    // 提示信息，成功时多为 "ok"，失败时为错误描述
    private String message;
    // 业务数据，失败时可为 null
    private T data;

    /** 成功响应：success=true，message="ok"，携带业务数据 */
    public static <T> ApiResponse<T> ok(T data) {
        // 构造成功响应对象
        return new ApiResponse<>(true, "ok", data);
    }

    /** 失败响应：success=false，message 为错误信息，data 为 null */
    public static <T> ApiResponse<T> fail(String message) {
        // 构造失败响应对象
        return new ApiResponse<>(false, message, null);
    }
}
