package com.openagentflow.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * IDEA控制台请求上下文日志过滤器。
 *
 * <p>启动阶段仅保留平台启动摘要，后台空闲阶段不输出普通日志；只有携带请求ID的HTTP请求及其异步链路才输出。
 * 后台任务即使携带Trace ID也不会刷屏；没有请求上下文的ERROR仍然保留，便于定位严重故障。</p>
 */
public class RequestContextConsoleLogFilter extends Filter<ILoggingEvent> {

    /** 允许在无请求上下文时输出的平台启动摘要Logger。 */
    private static final String STARTUP_LOGGER = StartupLogRunner.class.getName();

    /**
     * 根据MDC请求ID决定是否写入控制台。
     *
     * @param event Logback日志事件
     * @return 过滤决定
     */
    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event == null) {
            return FilterReply.DENY;
        }
        // 启动摘要包含服务地址、端口和依赖配置，开发环境需要始终可见。
        if (STARTUP_LOGGER.equals(event.getLoggerName())) {
            return FilterReply.NEUTRAL;
        }
        Map<String, String> context = event.getMDCPropertyMap();
        if (StringUtils.hasText(context.get("requestId"))) {
            return FilterReply.NEUTRAL;
        }
        // 启动失败、配置错误等严重问题必须保留，否则IDEA只能看到进程退出而没有原因。
        return event.getLevel().isGreaterOrEqual(Level.ERROR) ? FilterReply.NEUTRAL : FilterReply.DENY;
    }
}
