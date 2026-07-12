package com.openagentflow.config;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * MyBatis数据库访问日志拦截器。
 *
 * <p>只打印带问号占位符的SQL、Mapper方法、耗时和结果数量，不打印参数值，避免密码、Token和API Key进入控制台。</p>
 */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        })
})
public class DatabaseQueryLoggingInterceptor implements Interceptor {

    /** 独立数据库日志分类。 */
    private static final Logger log = LoggerFactory.getLogger("com.openagentflow.database");

    /** 是否启用MyBatis SQL访问日志。 */
    private final boolean enabled;

    /** 慢SQL阈值，超过后使用WARN级别输出。 */
    private final long slowSqlMs;

    public DatabaseQueryLoggingInterceptor(
            @Value("${openagentflow.logging.sql-enabled:true}") boolean enabled,
            @Value("${openagentflow.logging.slow-sql-ms:1000}") long slowSqlMs) {
        this.enabled = enabled;
        this.slowSqlMs = Math.max(1L, slowSqlMs);
    }

    /**
     * 统计SQL执行时间并输出安全日志。
     *
     * @param invocation MyBatis调用上下文
     * @return SQL执行结果
     * @throws Throwable 原始数据库异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!enabled) {
            return invocation.proceed();
        }
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
        String sql = normalizeSql(statement.getBoundSql(parameter).getSql());
        long startedAt = System.nanoTime();
        try {
            Object result = invocation.proceed();
            long elapsedMs = elapsedMillis(startedAt);
            // 启动检查、定时巡检和Kafka后台任务没有HTTP请求ID，不向IDEA控制台输出成功SQL。
            if (!StringUtils.hasText(MDC.get("requestId"))) {
                return result;
            }
            String message = "SQL {} mapper={} durationMs={} rows={} sql={}";
            if (elapsedMs >= slowSqlMs) {
                log.warn(message, statement.getSqlCommandType(), statement.getId(), elapsedMs, resultSize(result), sql);
            } else {
                log.info(message, statement.getSqlCommandType(), statement.getId(), elapsedMs, resultSize(result), sql);
            }
            return result;
        } catch (Throwable throwable) {
            log.error("SQL FAILED command={} mapper={} durationMs={} sql={} error={}",
                    statement.getSqlCommandType(), statement.getId(), elapsedMillis(startedAt), sql, throwable.getMessage());
            throw throwable;
        }
    }

    /** 规整多行SQL，保持IDEA控制台一条访问占一行。 */
    private String normalizeSql(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }

    /** 计算纳秒起点到当前时间的毫秒数。 */
    private long elapsedMillis(long startedAt) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    /** 返回更新行数或查询结果数量。 */
    private int resultSize(Object result) {
        if (result instanceof Collection<?> collection) {
            return collection.size();
        }
        if (result instanceof Number number) {
            return number.intValue();
        }
        return result == null ? 0 : 1;
    }
}
