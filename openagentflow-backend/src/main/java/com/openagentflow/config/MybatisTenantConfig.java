package com.openagentflow.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.openagentflow.security.TenantIsolationPolicy;
import com.openagentflow.security.WorkspaceContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** MyBatis-Plus 全局工作空间隔离配置。 */
@Configuration
public class MybatisTenantConfig {

    /** 对已纳管核心表自动追加 workspace_id 条件，并在插入时自动补齐租户字段。 */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new StringValue(WorkspaceContextHolder.current());
            }

            @Override
            public String getTenantIdColumn() {
                return "workspace_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 后台任务没有请求租户上下文，必须依赖任务载荷中的可信 workspaceId 显式隔离。
                return !StringUtils.hasText(WorkspaceContextHolder.current())
                        || !TenantIsolationPolicy.requiresTenantCondition(tableName);
            }
        }));
        return interceptor;
    }
}
