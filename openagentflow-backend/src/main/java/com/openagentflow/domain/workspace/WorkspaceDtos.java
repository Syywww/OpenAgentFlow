package com.openagentflow.domain.workspace;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织与工作空间 DTO 集合。
 */
public class WorkspaceDtos {

    /**
     * 组织保存请求。
     */
    public static class OrganizationRequest {
        /** 组织编码，不传时后端根据名称生成。 */
        private String orgCode;
        /** 组织名称。 */
        @NotBlank(message = "组织名称不能为空")
        private String orgName;
        /** 组织描述。 */
        private String description;

        public String getOrgCode() {
            return orgCode;
        }

        public void setOrgCode(String orgCode) {
            this.orgCode = orgCode;
        }

        public String getOrgName() {
            return orgName;
        }

        public void setOrgName(String orgName) {
            this.orgName = orgName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 工作空间保存请求。
     */
    public static class WorkspaceRequest {
        /** 所属组织 ID。 */
        private String organizationId;
        /** 工作空间编码，不传时后端根据名称生成。 */
        private String workspaceCode;
        /** 工作空间名称。 */
        @NotBlank(message = "工作空间名称不能为空")
        private String workspaceName;
        /** 工作空间描述。 */
        private String description;
        /** 工作空间类型。 */
        private String workspaceType;
        /** 是否默认工作空间。 */
        private Boolean defaultFlag;

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getWorkspaceCode() {
            return workspaceCode;
        }

        public void setWorkspaceCode(String workspaceCode) {
            this.workspaceCode = workspaceCode;
        }

        public String getWorkspaceName() {
            return workspaceName;
        }

        public void setWorkspaceName(String workspaceName) {
            this.workspaceName = workspaceName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getWorkspaceType() {
            return workspaceType;
        }

        public void setWorkspaceType(String workspaceType) {
            this.workspaceType = workspaceType;
        }

        public Boolean getDefaultFlag() {
            return defaultFlag;
        }

        public void setDefaultFlag(Boolean defaultFlag) {
            this.defaultFlag = defaultFlag;
        }
    }

    /**
     * 成员保存请求。
     */
    public static class MemberRequest {
        /** 用户 ID。 */
        @NotBlank(message = "用户 ID 不能为空")
        private String userId;
        /** 成员角色，owner/admin/member/viewer。 */
        private String memberRole;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getMemberRole() {
            return memberRole;
        }

        public void setMemberRole(String memberRole) {
            this.memberRole = memberRole;
        }
    }

    /**
     * 组织摘要。
     */
    public static class OrganizationSummary {
        /** 组织 ID。 */
        private String id;
        /** 组织编码。 */
        private String orgCode;
        /** 组织名称。 */
        private String orgName;
        /** 组织描述。 */
        private String description;
        /** 组织状态。 */
        private String status;
        /** 成员数量。 */
        private Integer memberCount;
        /** 工作空间数量。 */
        private Integer workspaceCount;
        /** 当前用户是否可管理。 */
        private Boolean canManage;
        /** 创建时间。 */
        private LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getOrgCode() {
            return orgCode;
        }

        public void setOrgCode(String orgCode) {
            this.orgCode = orgCode;
        }

        public String getOrgName() {
            return orgName;
        }

        public void setOrgName(String orgName) {
            this.orgName = orgName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getMemberCount() {
            return memberCount;
        }

        public void setMemberCount(Integer memberCount) {
            this.memberCount = memberCount;
        }

        public Integer getWorkspaceCount() {
            return workspaceCount;
        }

        public void setWorkspaceCount(Integer workspaceCount) {
            this.workspaceCount = workspaceCount;
        }

        public Boolean getCanManage() {
            return canManage;
        }

        public void setCanManage(Boolean canManage) {
            this.canManage = canManage;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 工作空间摘要。
     */
    public static class WorkspaceSummary {
        /** 工作空间 ID。 */
        private String id;
        /** 所属组织 ID。 */
        private String organizationId;
        /** 所属组织名称。 */
        private String organizationName;
        /** 工作空间编码。 */
        private String workspaceCode;
        /** 工作空间名称。 */
        private String workspaceName;
        /** 工作空间描述。 */
        private String description;
        /** 工作空间类型。 */
        private String workspaceType;
        /** 成员数量。 */
        private Integer memberCount;
        /** Agent 数量。 */
        private Integer agentCount;
        /** 知识库数量。 */
        private Integer knowledgeBaseCount;
        /** 工具数量。 */
        private Integer toolCount;
        /** 工作流数量。 */
        private Integer workflowCount;
        /** 是否默认工作空间。 */
        private Boolean defaultFlag;
        /** 当前用户角色。 */
        private String currentUserRole;
        /** 当前用户是否可管理。 */
        private Boolean canManage;
        /** 创建时间。 */
        private LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getOrganizationName() {
            return organizationName;
        }

        public void setOrganizationName(String organizationName) {
            this.organizationName = organizationName;
        }

        public String getWorkspaceCode() {
            return workspaceCode;
        }

        public void setWorkspaceCode(String workspaceCode) {
            this.workspaceCode = workspaceCode;
        }

        public String getWorkspaceName() {
            return workspaceName;
        }

        public void setWorkspaceName(String workspaceName) {
            this.workspaceName = workspaceName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getWorkspaceType() {
            return workspaceType;
        }

        public void setWorkspaceType(String workspaceType) {
            this.workspaceType = workspaceType;
        }

        public Integer getMemberCount() {
            return memberCount;
        }

        public void setMemberCount(Integer memberCount) {
            this.memberCount = memberCount;
        }

        public Integer getAgentCount() {
            return agentCount;
        }

        public void setAgentCount(Integer agentCount) {
            this.agentCount = agentCount;
        }

        public Integer getKnowledgeBaseCount() {
            return knowledgeBaseCount;
        }

        public void setKnowledgeBaseCount(Integer knowledgeBaseCount) {
            this.knowledgeBaseCount = knowledgeBaseCount;
        }

        public Integer getToolCount() {
            return toolCount;
        }

        public void setToolCount(Integer toolCount) {
            this.toolCount = toolCount;
        }

        public Integer getWorkflowCount() {
            return workflowCount;
        }

        public void setWorkflowCount(Integer workflowCount) {
            this.workflowCount = workflowCount;
        }

        public Boolean getDefaultFlag() {
            return defaultFlag;
        }

        public void setDefaultFlag(Boolean defaultFlag) {
            this.defaultFlag = defaultFlag;
        }

        public String getCurrentUserRole() {
            return currentUserRole;
        }

        public void setCurrentUserRole(String currentUserRole) {
            this.currentUserRole = currentUserRole;
        }

        public Boolean getCanManage() {
            return canManage;
        }

        public void setCanManage(Boolean canManage) {
            this.canManage = canManage;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 成员摘要。
     */
    public static class MemberSummary {
        /** 成员记录 ID。 */
        private String id;
        /** 用户 ID。 */
        private String userId;
        /** 用户名。 */
        private String username;
        /** 展示名称。 */
        private String displayName;
        /** 成员角色。 */
        private String memberRole;
        /** 成员状态。 */
        private String status;
        /** 加入时间。 */
        private LocalDateTime joinedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getMemberRole() {
            return memberRole;
        }

        public void setMemberRole(String memberRole) {
            this.memberRole = memberRole;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getJoinedAt() {
            return joinedAt;
        }

        public void setJoinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
        }
    }

    /**
     * 工作空间详情。
     */
    public static class WorkspaceDetail extends WorkspaceSummary {
        /** 成员列表。 */
        private List<MemberSummary> members;

        public List<MemberSummary> getMembers() {
            return members;
        }

        public void setMembers(List<MemberSummary> members) {
            this.members = members;
        }
    }
}
