package com.openagentflow.exception;

import com.openagentflow.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>所有 Controller 抛出的异常都会在这里转换成统一响应结构。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 全局异常日志记录器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常对象
     * @return 统一失败响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        LOGGER.warn("业务异常 code={} message={}", exception.getCode(), exception.getMessage());
        return ApiResponse.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理请求体参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("请求参数不合法");
        LOGGER.warn("请求体参数校验失败 message={}", message);
        return ApiResponse.fail("PARAM_INVALID", message);
    }

    /**
     * 处理路径参数校验异常。
     *
     * @param exception 路径参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        LOGGER.warn("请求参数校验失败 message={}", exception.getMessage());
        return ApiResponse.fail("PARAM_INVALID", exception.getMessage());
    }

    /**
     * 处理登录认证异常。
     *
     * @param exception 认证异常
     * @return 统一失败响应
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthenticationException(AuthenticationException exception) {
        // 用户名或密码错误时不暴露具体原因，避免被枚举账号。
        LOGGER.warn("登录认证失败");
        return ApiResponse.fail("LOGIN_FAILED", "用户名或密码错误");
    }

    /**
     * 处理未预期异常。
     *
     * @param exception 系统异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception exception) {
        // 对外只返回通用信息，详细堆栈写入后端日志便于排查。
        LOGGER.error("系统异常", exception);
        return ApiResponse.fail("SYSTEM_ERROR", "系统繁忙，请稍后重试");
    }
}
