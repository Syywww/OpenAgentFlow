package com.openagentflow.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 多角色数据范围合并策略。
 */
public final class DataScopePolicy {

    /** 数据范围从小到大的优先级。 */
    private static final List<String> SCOPE_ORDER = List.of("self", "custom", "dept", "dept_tree", "all");

    private DataScopePolicy() {
    }

    /**
     * 合并多个角色的数据范围，返回其中权限最大的范围。
     *
     * @param scopes 数据范围集合
     * @return 合并后的数据范围
     */
    public static String merge(Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "self";
        }
        return scopes.stream()
                .filter(scope -> scope != null && SCOPE_ORDER.contains(scope.toLowerCase(Locale.ROOT)))
                .map(scope -> scope.toLowerCase(Locale.ROOT))
                .max((left, right) -> Integer.compare(SCOPE_ORDER.indexOf(left), SCOPE_ORDER.indexOf(right)))
                .orElse("self");
    }
}
