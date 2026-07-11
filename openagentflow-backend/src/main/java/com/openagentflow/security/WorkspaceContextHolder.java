package com.openagentflow.security;

/** 当前请求工作空间上下文。 */
public final class WorkspaceContextHolder {

    /** 当前线程工作空间ID。 */
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private WorkspaceContextHolder() { }

    /** 绑定已通过成员关系校验的工作空间ID。 */
    public static void bind(String workspaceId) { CURRENT.set(workspaceId); }

    /** 获取当前工作空间ID。 */
    public static String current() { return CURRENT.get(); }

    /** 清理线程上下文，防止线程池复用时串租户。 */
    public static void clear() { CURRENT.remove(); }
}
