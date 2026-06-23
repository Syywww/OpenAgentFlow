package com.openagentflow.api;

import java.time.LocalDateTime;

/**
 * 统一接口响应对象。
 *
 * @param <T> 业务数据类型
 */
public class ApiResponse<T> {

    /** 是否请求成功。 */
    private Boolean success;

    /** 业务响应码。 */
    private String code;

    /** 响应提示信息。 */
    private String message;

    /** 业务响应数据。 */
    private T data;

    /** 服务端响应时间。 */
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 构造成功响应。
     *
     * @param data 业务数据
     * @param <T> 数据类型
     * @return 统一成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        // 成功响应统一使用 0，便于前端做通用拦截。
        response.setSuccess(true);
        response.setCode("0");
        response.setMessage("操作成功");
        response.setData(data);
        return response;
    }

    /**
     * 构造失败响应。
     *
     * @param code 业务错误码
     * @param message 错误说明
     * @param <T> 数据类型
     * @return 统一失败响应
     */
    public static <T> ApiResponse<T> fail(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        // 失败响应不携带业务数据，前端只展示错误提示。
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
