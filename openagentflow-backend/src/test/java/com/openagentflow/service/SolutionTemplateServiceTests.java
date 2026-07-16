package com.openagentflow.service;

import com.openagentflow.api.PageResult;
import com.openagentflow.domain.template.TemplateDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** 解决方案模板广场真实数据库查询测试。 */
@SpringBootTest(properties = {
        "openagentflow.async-task.consumer-enabled=false",
        "openagentflow.observability.otlp-enabled=false"
})
class SolutionTemplateServiceTests {

    /** 模板服务。 */
    @Autowired
    private SolutionTemplateService templateService;

    /** 公开广场必须返回真实分页数据，并能读取当前版本详情。 */
    @Test
    void shouldLoadPublishedMarketplaceAndDetail() {
        PageResult<TemplateDtos.TemplateSummary> page = templateService.listPublic(
                "all", "企业", "recommended", false, 1, 10);

        assertThat(page.getRecords()).isNotEmpty();
        TemplateDtos.TemplateDetail detail = templateService.detail(page.getRecords().getFirst().id);
        assertThat(detail.currentVersion).isEqualTo("1.0.0");
        assertThat(detail.versions).isNotEmpty();
        assertThat(detail.licenseCode).isEqualTo("Apache-2.0");
    }
}
