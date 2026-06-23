package com.openagentflow;

import com.openagentflow.config.OpenAgentFlowProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * OpenAgentFlow 后端启动类。
 *
 * <p>当前后端基础框架负责承接前端原型中的 Agent、RAG、MCP、工具中心、运行观测等模块。</p>
 */
@SpringBootApplication
@MapperScan("com.openagentflow.mapper")
@EnableConfigurationProperties(OpenAgentFlowProperties.class)
public class OpenAgentFlowApplication {

    /**
     * 应用启动入口。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        // 交给 Spring Boot 统一完成容器启动、配置加载和 Bean 扫描。
        SpringApplication.run(OpenAgentFlowApplication.class, args);
    }
}
