package com.openagentflow.domain.iam;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 权限治理接口对象集合。
 */
public final class PermissionGovernanceDtos {

    private PermissionGovernanceDtos() {
    }

    /**
     * 权限治理概览。
     *
     * @param workspaceRoleCount 空间角色数量
     * @param memberRoleBindingCount 成员角色绑定数量
     * @param activeAclCount 生效资源授权数量
     * @param authorizationAuditCount 授权审计数量
     */
    public record GovernanceOverview(long workspaceRoleCount,
                                     long memberRoleBindingCount,
                                     long activeAclCount,
                                     long authorizationAuditCount) {
    }

    /**
     * 工作空间角色摘要。
     *
     * @param id 角色ID
     * @param workspaceId 工作空间ID
     * @param roleCode 角色编码
     * @param roleName 角色名称
     * @param description 角色说明
     * @param dataScope 数据范围
     * @param builtIn 是否内置
     * @param status 状态
     * @param permissionIds 权限ID集合
     * @param permissionCodes 权限编码集合
     * @param departmentIds 自定义部门ID集合
     * @param memberCount 绑定成员数量
     */
    public record WorkspaceRoleSummary(String id,
                                       String workspaceId,
                                       String roleCode,
                                       String roleName,
                                       String description,
                                       String dataScope,
                                       boolean builtIn,
                                       String status,
                                       List<String> permissionIds,
                                       List<String> permissionCodes,
                                       List<String> departmentIds,
                                       long memberCount) {
    }

    /**
     * 工作空间角色保存请求。
     *
     * @param workspaceId 工作空间ID
     * @param roleCode 角色编码
     * @param roleName 角色名称
     * @param description 角色说明
     * @param dataScope 数据范围
     * @param status 状态
     * @param permissionIds 权限ID集合
     * @param departmentIds 自定义部门ID集合
     */
    public record WorkspaceRoleRequest(@NotBlank String workspaceId,
                                       @NotBlank String roleCode,
                                       @NotBlank String roleName,
                                       String description,
                                       @NotBlank String dataScope,
                                       @NotBlank String status,
                                       List<String> permissionIds,
                                       List<String> departmentIds) {
    }

    /**
     * 空间成员角色分配请求。
     *
     * @param roleIds 空间角色ID集合
     * @param reason 分配原因
     */
    public record MemberRoleRequest(List<String> roleIds, String reason) {
    }

    /**
     * 数据范围结果。
     *
     * @param scopeType 数据范围类型
     * @param departmentIds 可访问部门ID集合
     * @param ownerOnly 是否仅允许本人数据
     */
    public record DataScopeResult(String scopeType, List<String> departmentIds, boolean ownerOnly) {
    }

    /**
     * 资源授权摘要。
     *
     * @param id 授权ID
     * @param workspaceId 工作空间ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param subjectType 主体类型
     * @param subjectId 主体ID
     * @param permissionLevel 权限级别
     * @param status 授权状态
     * @param expiresAt 到期时间
     * @param grantReason 授权原因
     * @param grantedBy 授权人ID
     * @param createdAt 创建时间
     */
    public record ResourceAclSummary(String id,
                                     String workspaceId,
                                     String resourceType,
                                     String resourceId,
                                     String subjectType,
                                     String subjectId,
                                     String permissionLevel,
                                     String status,
                                     LocalDateTime expiresAt,
                                     String grantReason,
                                     String grantedBy,
                                     LocalDateTime createdAt) {
    }

    /**
     * 资源授权请求。
     *
     * @param workspaceId 工作空间ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param subjectType 主体类型：user、role、department
     * @param subjectId 主体ID
     * @param permissionLevel 权限级别：read、run、write、owner
     * @param expiresAt 到期时间
     * @param reason 授权原因
     */
    public record ResourceAclRequest(@NotBlank String workspaceId,
                                     @NotBlank String resourceType,
                                     @NotBlank String resourceId,
                                     @NotBlank String subjectType,
                                     @NotBlank String subjectId,
                                     @NotBlank String permissionLevel,
                                     LocalDateTime expiresAt,
                                     String reason) {
    }

    /**
     * 强制下线请求。
     *
     * @param reason 强制下线原因
     */
    public record RevokeSessionRequest(String reason) {
    }

    /**
     * 授权审计摘要。
     *
     * @param id 审计ID
     * @param workspaceId 工作空间ID
     * @param operatorUserId 操作人ID
     * @param actionType 动作类型
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param subjectType 主体类型
     * @param subjectId 主体ID
     * @param reason 原因
     * @param beforeData 变更前数据
     * @param afterData 变更后数据
     * @param createdAt 创建时间
     */
    public record AuthorizationAuditSummary(String id,
                                            String workspaceId,
                                            String operatorUserId,
                                            String actionType,
                                            String targetType,
                                            String targetId,
                                            String subjectType,
                                            String subjectId,
                                            String reason,
                                            Map<String, Object> beforeData,
                                            Map<String, Object> afterData,
                                            LocalDateTime createdAt) {
    }
}
