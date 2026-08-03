package com.openagentflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * OpenAgentFlow 应用启动测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class OpenAgentFlowApplicationTests {

    /**
     * 验证 Spring 容器可以正常加载。
     */
    @Test
    void contextLoads() {
        // 基础框架阶段只验证上下文加载，具体业务接口后续按模块补充测试。
    }
}
