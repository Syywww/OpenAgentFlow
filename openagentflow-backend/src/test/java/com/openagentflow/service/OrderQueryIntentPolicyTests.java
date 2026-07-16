package com.openagentflow.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 订单实时查询意图规则测试。 */
class OrderQueryIntentPolicyTests {

    /** 常见同义表达和“那些/哪些”差异都应识别为订单汇总查询。 */
    @Test
    void shouldRecognizeOrderSummaryVariants() {
        assertThat(OrderQueryIntentPolicy.isSummaryQuery("现在有那些订单")).isTrue();
        assertThat(OrderQueryIntentPolicy.isSummaryQuery("我有哪些订单？")).isTrue();
        assertThat(OrderQueryIntentPolicy.isSummaryQuery("帮我查一下订单列表")).isTrue();
        assertThat(OrderQueryIntentPolicy.isSummaryQuery("当前一共有几笔订单")).isTrue();
        assertThat(OrderQueryIntentPolicy.isSummaryQuery("把我当前的订单列一下")).isTrue();
        assertThat(OrderQueryIntentPolicy.isSummaryQuery("查查现有的订单清单")).isTrue();
    }

    /** 产品知识、问候和订单政策咨询不能误触发实时订单工具。 */
    @Test
    void shouldRejectKnowledgeAndUnrelatedQueries() {
        assertThat(OrderQueryIntentPolicy.isSummaryQuery("订单退款政策是什么")).isFalse();
        assertThat(OrderQueryIntentPolicy.isSummaryQuery("我的订单退款政策是什么")).isFalse();
        assertThat(OrderQueryIntentPolicy.isSummaryQuery("订单有哪些退款规则")).isFalse();
        assertThat(OrderQueryIntentPolicy.isSummaryQuery("订单状态一共有哪几种")).isFalse();
        assertThat(OrderQueryIntentPolicy.shouldRunOrderWorkflow("你好啊")).isFalse();
        assertThat(OrderQueryIntentPolicy.shouldRunOrderWorkflow("有什么优惠券")).isFalse();
    }

    /** 明确订单号加物流动作应进入实时订单链路。 */
    @Test
    void shouldRecognizeSpecificOrderRuntimeQuery() {
        assertThat(OrderQueryIntentPolicy.shouldRunOrderWorkflow("订单 OAF-DEMO-1001 到哪里了")).isTrue();
        assertThat(OrderQueryIntentPolicy.matchesIntent("order_runtime", "把我当前的订单列一下")).isTrue();
        assertThat(OrderQueryIntentPolicy.matchesIntent("unknown", "把我当前的订单列一下")).isFalse();
    }
}
