package com.openagentflow.exception;

/**
 * 业务异常。
 *
 * <p>用于表达可预期的业务失败，例如参数不合法、资源不存在、状态不允许等。</p>
 */
public class BusinessException extends RuntimeException {

    /** 业务错误码。 */
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
