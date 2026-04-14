package com.lifereview.common;

// Lombok：生成全参构造器
import lombok.AllArgsConstructor;
// Lombok：生成 getter/setter/toString/equals/hashCode
import lombok.Data;
// Lombok：生成无参构造器
import lombok.NoArgsConstructor;

/**
 * 统一 HTTP API 响应封装（泛型载体）。
 * <p>所有接口宜使用本类型作为外层结构：{@code success} 表示业务是否成功；{@code message} 为人类可读提示（成功时常为 {@code "ok"}）；{@code data} 为具体业务负载，失败时通常为 {@code null}。</p>
 * <p>请优先使用静态工厂方法 {@link #ok(Object)} 与 {@link #fail(String)} 构造实例。访问器由 Lombok {@code @Data} 生成。</p>
 *
 * @param <T> {@code data} 字段的业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    /** 请求是否处理成功 */
    private boolean success;
    /** 提示文案：成功时多为 {@code "ok"}，失败时为错误说明 */
    private String message;
    /** 业务数据；失败时一般为 {@code null} */
    private T data;

    /**
     * 构造成功响应：{@code success == true}，{@code message} 固定为 {@code "ok"}，业务数据为传入值。
     *
     * @param <T> 业务数据类型
     * @param data 放入响应体的业务数据，允许为 {@code null}
     * @return 成功响应实例
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "ok", data);
    }

    /**
     * 构造失败响应：{@code success == false}，{@code data} 固定为 {@code null}。
     *
     * @param <T> 与调用方声明的响应泛型一致（占位）
     * @param message 错误说明，建议非空且便于用户或运维理解
     * @return 失败响应实例
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
