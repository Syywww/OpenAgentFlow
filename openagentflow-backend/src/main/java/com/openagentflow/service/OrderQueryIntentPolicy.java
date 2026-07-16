package com.openagentflow.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 订单实时查询意图策略。
 *
 * <p>统一维护 Agent 路由和演示订单工具使用的判断规则，避免不同入口对“哪些/那些”等同义表达处理不一致。</p>
 */
final class OrderQueryIntentPolicy {

    /** 演示订单号或常见业务订单号匹配规则。 */
    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile(
            ".*(oaf-demo-[0-9]+|\\b[a-z]{1,8}[-_][0-9]{4,}\\b|\\b[0-9]{8,}\\b).*",
            Pattern.CASE_INSENSITIVE
    );

    /** 订单汇总查询的明确表达，避免把订单政策等知识咨询误判为实时查询。 */
    private static final List<String> SUMMARY_PHRASES = List.of(
            "多少订单", "几个订单", "几笔订单", "订单数量", "订单数", "我的订单", "订单列表", "订单清单",
            "所有订单", "全部订单", "有哪些订单", "有那些订单", "哪些订单", "那些订单", "myorders", "orderlist"
    );

    /** 用户订单范围表达，不要求查询动作和订单词紧邻。 */
    private static final List<String> ORDER_SCOPE_PHRASES = List.of(
            "我的订单", "我当前的订单", "我现在的订单", "我现有的订单", "当前订单", "当前的订单",
            "现在的订单", "现有订单", "现有的订单", "全部订单", "所有订单"
    );

    /** 数量、列举和查看动作，配合订单实体形成结构化汇总意图。 */
    private static final List<String> SUMMARY_MARKERS = List.of(
            "多少", "几个", "几笔", "哪些", "那些", "哪几笔", "啥订单", "什么订单", "一共", "总共",
            "列表", "清单", "明细", "列一下", "列出来", "查一下", "查查", "查看", "看看", "显示"
    );

    /** 订单概念知识表达，这类问题应交给 RAG 而不是实时订单工具。 */
    private static final List<String> ORDER_KNOWLEDGE_PHRASES = List.of(
            "订单政策", "订单规则", "订单流程", "退款政策", "退款规则", "退款流程", "状态类型", "状态种类",
            "状态一共有哪几种", "订单说明"
    );

    /** 工具实时查询常见动作。 */
    private static final List<String> RUNTIME_ACTIONS = List.of(
            "订单", "order", "物流", "快递", "运单", "包裹", "到哪里", "到哪", "状态", "进度",
            "发货", "发出", "签收", "配送", "送达", "退款", "售后"
    );

    /** 与演示订单实时工具无关的知识或闲聊主题。 */
    private static final List<String> KNOWLEDGE_ONLY_TOPICS = List.of(
            "天气", "你好", "您好", "我是谁", "产品", "优惠", "优惠券", "优惠卷", "活动",
            "折扣", "满减", "促销", "会员", "积分", "价格", "套餐"
    );

    /** 工具类不允许实例化。 */
    private OrderQueryIntentPolicy() {
    }

    /**
     * 判断是否为订单数量或订单列表查询。
     *
     * @param input 用户输入
     * @return 是否为订单汇总查询
     */
    static boolean isSummaryQuery(String input) {
        String compactText = compact(input);
        if (compactText.isEmpty()) {
            return false;
        }

        // 先排除明确的概念知识问题，再按“订单实体 + 用户范围/汇总动作”组合判断。
        if (containsAny(compactText, ORDER_KNOWLEDGE_PHRASES)) {
            return false;
        }
        if (containsAny(compactText, SUMMARY_PHRASES)) {
            return true;
        }
        boolean hasOrderEntity = compactText.contains("订单") || compactText.contains("orders");
        boolean hasOrderScope = containsAny(compactText, ORDER_SCOPE_PHRASES);
        boolean hasSummaryMarker = containsAny(compactText, SUMMARY_MARKERS);
        return hasOrderEntity && (hasOrderScope || hasSummaryMarker);
    }

    /**
     * 判断输入是否应进入订单实时工作流。
     *
     * @param input 用户输入
     * @return 是否执行订单工作流
     */
    static boolean shouldRunOrderWorkflow(String input) {
        String normalizedText = normalize(input);
        if (normalizedText.isEmpty()) {
            return false;
        }

        // 汇总查询不要求用户提供订单号；单笔实时查询必须同时具备订单号和查询动作。
        boolean summaryQuery = isSummaryQuery(normalizedText);
        boolean specificRuntimeQuery = ORDER_NUMBER_PATTERN.matcher(normalizedText).matches()
                && containsAny(normalizedText, RUNTIME_ACTIONS);
        boolean knowledgeOnly = containsAny(normalizedText, KNOWLEDGE_ONLY_TOPICS);
        return (summaryQuery || specificRuntimeQuery) && !knowledgeOnly;
    }

    /**
     * 根据工作流条件中的意图编码执行统一判断。
     *
     * @param intentCode 意图编码
     * @param input 用户输入
     * @return 是否命中指定意图
     */
    static boolean matchesIntent(String intentCode, String input) {
        String normalizedCode = normalize(intentCode).replace('-', '_');
        return switch (normalizedCode) {
            case "order_runtime" -> shouldRunOrderWorkflow(input);
            case "order_summary" -> isSummaryQuery(input);
            default -> false;
        };
    }

    /**
     * 统一文本大小写和全角字符，保留订单号中的连接符供正则识别。
     *
     * @param input 原始文本
     * @return 标准化文本
     */
    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 移除空白和标点，兼容自然语言中的停顿符号与输入空格。
     *
     * @param input 原始文本
     * @return 紧凑文本
     */
    private static String compact(String input) {
        return normalize(input).replaceAll("[\\s\\p{P}\\p{S}]+", "");
    }

    /**
     * 判断文本是否包含任一关键词。
     *
     * @param text 待判断文本
     * @param keywords 关键词集合
     * @return 是否命中
     */
    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}
