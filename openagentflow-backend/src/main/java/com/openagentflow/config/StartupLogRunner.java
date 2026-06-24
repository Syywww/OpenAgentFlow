package com.openagentflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 后端启动完成后的控制台提示。
 *
 * <p>用于 IDEA 直接启动时在控制台打印关键访问地址和基础依赖配置，方便开发者确认服务已经启动成功。</p>
 */
@Component
public class StartupLogRunner implements ApplicationRunner {

    /** 启动日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(StartupLogRunner.class);

    /** Spring 环境配置读取器。 */
    private final Environment environment;

    public StartupLogRunner(Environment environment) {
        this.environment = environment;
    }

    /**
     * Spring Boot 容器启动完成后执行。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        // 读取端口、上下文路径和依赖配置，用于拼接 IDEA 控制台里的开发访问地址。
        String port = property("server.port", "8080");
        String contextPath = normalizeContextPath(property("server.servlet.context-path", ""));
        String localHost = "localhost";
        String lanHost = resolveLanHost();
        String profiles = activeProfiles();
        String mysqlUrl = property("spring.datasource.url", "-");
        String redisHost = property("spring.data.redis.host", "localhost");
        String redisPort = property("spring.data.redis.port", "6379");
        String milvusEnabled = property("openagentflow.milvus.enabled", "true");
        String milvusHost = property("openagentflow.milvus.host", "localhost");
        String milvusPort = property("openagentflow.milvus.port", "19530");
        boolean debugProfileEnabled = Arrays.asList(environment.getActiveProfiles()).contains("debug");

        // 使用多行日志块，让 IDEA 控制台中启动成功信息更醒目。
        log.info("""

                ----------------------------------------------------------
                OpenAgentFlow-Java 后端已启动
                当前环境: {}
                Debug日志: {}
                本机访问: http://{}:{}{}
                局域网访问: http://{}:{}{}
                Swagger: http://{}:{}{}/swagger-ui.html
                MySQL: {}
                Redis: {}:{}
                Milvus: enabled={}, {}:{}
                ----------------------------------------------------------
                """, profiles, debugProfileEnabled ? "已启用" : "未启用", localHost, port, contextPath, lanHost, port, contextPath,
                localHost, port, contextPath, mysqlUrl, redisHost, redisPort,
                milvusEnabled, milvusHost, milvusPort);
    }

    /**
     * 读取配置项，空值时返回默认值。
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    private String property(String key, String defaultValue) {
        String value = environment.getProperty(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    /**
     * 规范化上下文路径。
     *
     * @param contextPath 原始上下文路径
     * @return 可直接拼接 URL 的上下文路径
     */
    private String normalizeContextPath(String contextPath) {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }

    /**
     * 获取当前激活的 Spring Profile。
     *
     * @return Profile 展示文本
     */
    private String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return "default";
        }
        return Arrays.stream(profiles).collect(Collectors.joining(","));
    }

    /**
     * 获取局域网 IP，失败时回退到 localhost。
     *
     * @return 局域网主机地址
     */
    private String resolveLanHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            // 主机名解析失败不应影响应用启动，只回退展示 localhost。
            return "localhost";
        }
    }
}
