package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.notification.NotificationDtos;
import com.openagentflow.entity.IamUserEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.security.AuthUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 通知发布与偏好参数策略测试。
 */
class NotificationServicePolicyTests {

    /** 被测试的通知服务。 */
    private NotificationService service;

    @BeforeEach
    void setUp() {
        IamUserEntity user = new IamUserEntity();
        user.setId("user-1");
        user.setUsername("tester");
        AuthUserDetails details = new AuthUserDetails(user, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, List.of()));
        service = new NotificationService(mock(JdbcTemplate.class), new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 发布通知必须指定接收用户、角色或全员广播。 */
    @Test
    void shouldRejectPublishWithoutRecipients() {
        NotificationDtos.PublishRequest request = new NotificationDtos.PublishRequest();
        request.setNotificationType("system");
        request.setTitle("系统消息");
        request.setContent("正文");

        assertThatThrownBy(() -> service.publish(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须指定接收用户");
    }

    /** 不支持的通知级别必须在写库前被拒绝。 */
    @Test
    void shouldRejectUnknownSeverity() {
        NotificationDtos.PublishRequest request = new NotificationDtos.PublishRequest();
        request.setNotificationType("system");
        request.setTitle("系统消息");
        request.setContent("正文");
        request.setSeverity("fatal");
        request.setBroadcast(true);

        assertThatThrownBy(() -> service.publish(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("info、warning 或 critical");
    }

    /** 通知摘要频率必须使用平台支持的枚举。 */
    @Test
    void shouldRejectUnknownDigestMode() {
        NotificationDtos.Preference preference = new NotificationDtos.Preference();
        preference.setMinSeverity("info");
        preference.setDigestMode("weekly");
        preference.setStationEnabled(true);

        assertThatThrownBy(() -> service.savePreference(preference))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("realtime、hourly 或 daily");
    }
}
