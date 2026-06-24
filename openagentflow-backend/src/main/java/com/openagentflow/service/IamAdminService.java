package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openagentflow.domain.iam.IamAdminDtos;
import com.openagentflow.entity.IamDepartmentEntity;
import com.openagentflow.entity.IamPermissionEntity;
import com.openagentflow.entity.IamRoleEntity;
import com.openagentflow.entity.IamRolePermissionEntity;
import com.openagentflow.entity.IamUserEntity;
import com.openagentflow.entity.IamUserRoleEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.IamDepartmentMapper;
import com.openagentflow.mapper.IamPermissionMapper;
import com.openagentflow.mapper.IamRoleMapper;
import com.openagentflow.mapper.IamRolePermissionMapper;
import com.openagentflow.mapper.IamUserMapper;
import com.openagentflow.mapper.IamUserRoleMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * IAM 用户、部门、角色与权限管理服务。
 */
@Service
public class IamAdminService {

    /** 用户 Mapper。 */
    private final IamUserMapper userMapper;

    /** 部门 Mapper。 */
    private final IamDepartmentMapper departmentMapper;

    /** 角色 Mapper。 */
    private final IamRoleMapper roleMapper;

    /** 权限 Mapper。 */
    private final IamPermissionMapper permissionMapper;

    /** 用户角色关系 Mapper。 */
    private final IamUserRoleMapper userRoleMapper;

    /** 角色权限关系 Mapper。 */
    private final IamRolePermissionMapper rolePermissionMapper;

    /** JDBC 工具，用于聚合统计和关系查询。 */
    private final JdbcTemplate jdbcTemplate;

    /** 密码编码器。 */
    private final PasswordEncoder passwordEncoder;

    public IamAdminService(IamUserMapper userMapper,
                           IamDepartmentMapper departmentMapper,
                           IamRoleMapper roleMapper,
                           IamPermissionMapper permissionMapper,
                           IamUserRoleMapper userRoleMapper,
                           IamRolePermissionMapper rolePermissionMapper,
                           JdbcTemplate jdbcTemplate,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询 IAM 管理概览。
     *
     * @return IAM 管理概览
     */
    public IamAdminDtos.IamOverview overview() {
        requireIamManager();
        IamAdminDtos.IamOverview overview = new IamAdminDtos.IamOverview();
        // 用户统计排除软删除账号，避免回收站数据影响运营视图。
        overview.setUserCount(userMapper.selectCount(new LambdaQueryWrapper<IamUserEntity>()
                .isNull(IamUserEntity::getDeletedAt)));
        overview.setDepartmentCount(departmentMapper.selectCount(new LambdaQueryWrapper<>()));
        overview.setRoleCount(roleMapper.selectCount(new LambdaQueryWrapper<>()));
        overview.setPermissionCount(permissionMapper.selectCount(new LambdaQueryWrapper<>()));
        return overview;
    }

    /**
     * 查询用户列表。
     *
     * @return 用户摘要列表
     */
    public List<IamAdminDtos.UserSummary> listUsers() {
        requireIamManager();
        List<IamDepartmentEntity> departments = departmentMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, String> departmentNameMap = departments.stream()
                .collect(Collectors.toMap(IamDepartmentEntity::getId, IamDepartmentEntity::getDeptName, (a, b) -> a));
        return userMapper.selectList(new LambdaQueryWrapper<IamUserEntity>()
                        .isNull(IamUserEntity::getDeletedAt)
                        .orderByDesc(IamUserEntity::getCreatedAt))
                .stream()
                .map(user -> toUserSummary(user, departmentNameMap))
                .toList();
    }

    /**
     * 创建用户。
     *
     * @param request 用户保存请求
     * @return 用户摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public IamAdminDtos.UserSummary createUser(IamAdminDtos.UserRequest request) {
        requireIamManager();
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("IAM_PASSWORD_REQUIRED", "创建用户时密码不能为空");
        }
        ensureUsernameUnique(null, request.getUsername());
        ensureDepartmentExists(request.getDepartmentId());

        IamUserEntity user = new IamUserEntity();
        user.setId(newId());
        fillUser(user, request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setSourceType("local");
        userMapper.insert(user);
        replaceUserRoles(user.getId(), request.getRoleIds());
        return listUsers().stream()
                .filter(item -> user.getId().equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("IAM_USER_NOT_FOUND", "用户创建后查询失败"));
    }

    /**
     * 更新用户。
     *
     * @param id 用户 ID
     * @param request 用户保存请求
     * @return 用户摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public IamAdminDtos.UserSummary updateUser(String id, IamAdminDtos.UserRequest request) {
        requireIamManager();
        IamUserEntity user = requireUser(id);
        ensureUsernameUnique(id, request.getUsername());
        ensureDepartmentExists(request.getDepartmentId());

        fillUser(user, request);
        if (StringUtils.hasText(request.getPassword())) {
            // 密码为空时保持原密码，便于只调整部门或角色。
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setPasswordChangedAt(LocalDateTime.now());
        }
        userMapper.updateById(user);
        replaceUserRoles(user.getId(), request.getRoleIds());
        return listUsers().stream()
                .filter(item -> user.getId().equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("IAM_USER_NOT_FOUND", "用户更新后查询失败"));
    }

    /**
     * 删除用户。
     *
     * @param id 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(String id) {
        requireIamManager();
        IamUserEntity user = requireUser(id);
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new BusinessException("IAM_ADMIN_PROTECTED", "内置 admin 用户不允许删除");
        }
        // 使用软删除保留审计关联，登录查询会排除 deleted_at 非空的账号。
        user.setStatus("disabled");
        user.setDeletedAt(LocalDateTime.now());
        userMapper.updateById(user);
        userRoleMapper.delete(new LambdaQueryWrapper<IamUserRoleEntity>()
                .eq(IamUserRoleEntity::getUserId, id));
    }

    /**
     * 查询部门树。
     *
     * @return 部门树节点列表
     */
    public List<IamAdminDtos.DepartmentNode> listDepartments() {
        requireIamManager();
        List<IamDepartmentEntity> departments = departmentMapper.selectList(new LambdaQueryWrapper<IamDepartmentEntity>()
                .orderByAsc(IamDepartmentEntity::getSortOrder)
                .orderByAsc(IamDepartmentEntity::getDeptCode));
        Map<String, IamAdminDtos.DepartmentNode> nodeMap = new LinkedHashMap<>();
        departments.forEach(department -> nodeMap.put(department.getId(), toDepartmentNode(department)));

        List<IamAdminDtos.DepartmentNode> roots = new ArrayList<>();
        nodeMap.values().forEach(node -> {
            IamAdminDtos.DepartmentNode parent = nodeMap.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        });
        return roots;
    }

    /**
     * 创建部门。
     *
     * @param request 部门保存请求
     * @return 部门树
     */
    @Transactional(rollbackFor = Exception.class)
    public List<IamAdminDtos.DepartmentNode> createDepartment(IamAdminDtos.DepartmentRequest request) {
        requireIamManager();
        ensureDepartmentCodeUnique(null, request.getDeptCode());
        ensureParentDepartmentExists(request.getParentId());
        IamDepartmentEntity entity = new IamDepartmentEntity();
        entity.setId(newId());
        fillDepartment(entity, request);
        departmentMapper.insert(entity);
        return listDepartments();
    }

    /**
     * 更新部门。
     *
     * @param id 部门 ID
     * @param request 部门保存请求
     * @return 部门树
     */
    @Transactional(rollbackFor = Exception.class)
    public List<IamAdminDtos.DepartmentNode> updateDepartment(String id, IamAdminDtos.DepartmentRequest request) {
        requireIamManager();
        IamDepartmentEntity entity = requireDepartment(id);
        if (id.equals(request.getParentId())) {
            throw new BusinessException("IAM_DEPT_PARENT_INVALID", "父部门不能选择自己");
        }
        ensureDepartmentCodeUnique(id, request.getDeptCode());
        ensureParentDepartmentExists(request.getParentId());
        fillDepartment(entity, request);
        departmentMapper.updateById(entity);
        return listDepartments();
    }

    /**
     * 删除部门。
     *
     * @param id 部门 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(String id) {
        requireIamManager();
        requireDepartment(id);
        Long childCount = departmentMapper.selectCount(new LambdaQueryWrapper<IamDepartmentEntity>()
                .eq(IamDepartmentEntity::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("IAM_DEPT_HAS_CHILDREN", "部门下存在子部门，不能删除");
        }
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<IamUserEntity>()
                .eq(IamUserEntity::getDepartmentId, id)
                .isNull(IamUserEntity::getDeletedAt));
        if (userCount > 0) {
            throw new BusinessException("IAM_DEPT_HAS_USERS", "部门下存在用户，不能删除");
        }
        departmentMapper.deleteById(id);
    }

    /**
     * 查询角色列表。
     *
     * @return 角色摘要列表
     */
    public List<IamAdminDtos.RoleSummary> listRoles() {
        requireIamManager();
        return roleMapper.selectList(new LambdaQueryWrapper<IamRoleEntity>()
                        .orderByDesc(IamRoleEntity::getBuiltIn)
                        .orderByAsc(IamRoleEntity::getRoleCode))
                .stream()
                .map(this::toRoleSummary)
                .toList();
    }

    /**
     * 创建角色。
     *
     * @param request 角色保存请求
     * @return 角色摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public IamAdminDtos.RoleSummary createRole(IamAdminDtos.RoleRequest request) {
        requireIamManager();
        ensureRoleCodeUnique(null, request.getRoleCode());
        IamRoleEntity entity = new IamRoleEntity();
        entity.setId(newId());
        fillRole(entity, request);
        entity.setBuiltIn(false);
        roleMapper.insert(entity);
        replaceRolePermissions(entity.getId(), request.getPermissionIds());
        return toRoleSummary(requireRole(entity.getId()));
    }

    /**
     * 更新角色。
     *
     * @param id 角色 ID
     * @param request 角色保存请求
     * @return 角色摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public IamAdminDtos.RoleSummary updateRole(String id, IamAdminDtos.RoleRequest request) {
        requireIamManager();
        IamRoleEntity entity = requireRole(id);
        ensureRoleCodeUnique(id, request.getRoleCode());
        fillRole(entity, request);
        roleMapper.updateById(entity);
        replaceRolePermissions(id, request.getPermissionIds());
        return toRoleSummary(requireRole(id));
    }

    /**
     * 删除角色。
     *
     * @param id 角色 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(String id) {
        requireIamManager();
        IamRoleEntity entity = requireRole(id);
        if (Boolean.TRUE.equals(entity.getBuiltIn())) {
            throw new BusinessException("IAM_ROLE_BUILT_IN", "内置角色不允许删除");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<IamUserRoleEntity>()
                .eq(IamUserRoleEntity::getRoleId, id));
        rolePermissionMapper.delete(new LambdaQueryWrapper<IamRolePermissionEntity>()
                .eq(IamRolePermissionEntity::getRoleId, id));
        roleMapper.deleteById(id);
    }

    /**
     * 更新角色权限。
     *
     * @param id 角色 ID
     * @param permissionIds 权限 ID 列表
     * @return 角色摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public IamAdminDtos.RoleSummary updateRolePermissions(String id, List<String> permissionIds) {
        requireIamManager();
        requireRole(id);
        replaceRolePermissions(id, permissionIds);
        return toRoleSummary(requireRole(id));
    }

    /**
     * 查询权限树。
     *
     * @return 权限树
     */
    public List<IamAdminDtos.PermissionNode> listPermissions() {
        requireIamManager();
        List<IamPermissionEntity> permissions = permissionMapper.selectList(new LambdaQueryWrapper<IamPermissionEntity>()
                .orderByAsc(IamPermissionEntity::getSortOrder)
                .orderByAsc(IamPermissionEntity::getPermissionCode));
        Map<String, IamAdminDtos.PermissionNode> nodeMap = new LinkedHashMap<>();
        permissions.forEach(permission -> nodeMap.put(permission.getId(), toPermissionNode(permission)));

        List<IamAdminDtos.PermissionNode> roots = new ArrayList<>();
        nodeMap.values().forEach(node -> {
            IamAdminDtos.PermissionNode parent = nodeMap.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        });
        return roots;
    }

    /**
     * 填充用户实体。
     *
     * @param user 用户实体
     * @param request 保存请求
     */
    private void fillUser(IamUserEntity user, IamAdminDtos.UserRequest request) {
        user.setDepartmentId(blankToNull(request.getDepartmentId()));
        user.setUsername(request.getUsername().trim());
        user.setEmail(blankToNull(request.getEmail()));
        user.setPhone(blankToNull(request.getPhone()));
        user.setDisplayName(request.getDisplayName().trim());
        user.setStatus(normalizeStatus(request.getStatus()));
    }

    /**
     * 替换用户角色关系。
     *
     * @param userId 用户 ID
     * @param roleIds 角色 ID 列表
     */
    private void replaceUserRoles(String userId, List<String> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<IamUserRoleEntity>()
                .eq(IamUserRoleEntity::getUserId, userId));
        safeIds(roleIds).forEach(roleId -> {
            requireRole(roleId);
            IamUserRoleEntity relation = new IamUserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        });
    }

    /**
     * 填充部门实体。
     *
     * @param entity 部门实体
     * @param request 保存请求
     */
    private void fillDepartment(IamDepartmentEntity entity, IamAdminDtos.DepartmentRequest request) {
        entity.setParentId(blankToNull(request.getParentId()));
        entity.setDeptCode(request.getDeptCode().trim());
        entity.setDeptName(request.getDeptName().trim());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setStatus(normalizeStatus(request.getStatus()));
    }

    /**
     * 填充角色实体。
     *
     * @param entity 角色实体
     * @param request 保存请求
     */
    private void fillRole(IamRoleEntity entity, IamAdminDtos.RoleRequest request) {
        entity.setRoleCode(request.getRoleCode().trim());
        entity.setRoleName(request.getRoleName().trim());
        entity.setDescription(blankToNull(request.getDescription()));
        entity.setStatus(normalizeStatus(request.getStatus()));
    }

    /**
     * 替换角色权限关系。
     *
     * @param roleId 角色 ID
     * @param permissionIds 权限 ID 列表
     */
    private void replaceRolePermissions(String roleId, List<String> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<IamRolePermissionEntity>()
                .eq(IamRolePermissionEntity::getRoleId, roleId));
        safeIds(permissionIds).forEach(permissionId -> {
            requirePermission(permissionId);
            IamRolePermissionEntity relation = new IamRolePermissionEntity();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        });
    }

    /**
     * 转换用户摘要。
     *
     * @param user 用户实体
     * @param departmentNameMap 部门名称映射
     * @return 用户摘要
     */
    private IamAdminDtos.UserSummary toUserSummary(IamUserEntity user, Map<String, String> departmentNameMap) {
        IamAdminDtos.UserSummary summary = new IamAdminDtos.UserSummary();
        summary.setId(user.getId());
        summary.setDepartmentId(user.getDepartmentId());
        summary.setDepartmentName(departmentNameMap.getOrDefault(user.getDepartmentId(), ""));
        summary.setUsername(user.getUsername());
        summary.setEmail(user.getEmail());
        summary.setPhone(user.getPhone());
        summary.setDisplayName(user.getDisplayName());
        summary.setStatus(user.getStatus());
        summary.setSourceType(user.getSourceType());
        summary.setLastLoginAt(user.getLastLoginAt());
        summary.setCreatedAt(user.getCreatedAt());
        summary.setRoleIds(queryStrings("SELECT role_id FROM iam_user_role WHERE user_id = ? ORDER BY created_at", user.getId()));
        summary.setRoleCodes(queryStrings("""
                SELECT r.role_code
                FROM iam_role r
                JOIN iam_user_role ur ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.role_code
                """, user.getId()));
        summary.setRoleNames(queryStrings("""
                SELECT r.role_name
                FROM iam_role r
                JOIN iam_user_role ur ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.role_code
                """, user.getId()));
        return summary;
    }

    /**
     * 转换部门节点。
     *
     * @param entity 部门实体
     * @return 部门节点
     */
    private IamAdminDtos.DepartmentNode toDepartmentNode(IamDepartmentEntity entity) {
        IamAdminDtos.DepartmentNode node = new IamAdminDtos.DepartmentNode();
        node.setId(entity.getId());
        node.setParentId(entity.getParentId());
        node.setDeptCode(entity.getDeptCode());
        node.setDeptName(entity.getDeptName());
        node.setSortOrder(entity.getSortOrder());
        node.setStatus(entity.getStatus());
        node.setUserCount(count("SELECT COUNT(1) FROM iam_user WHERE department_id = ? AND deleted_at IS NULL", entity.getId()));
        return node;
    }

    /**
     * 转换角色摘要。
     *
     * @param entity 角色实体
     * @return 角色摘要
     */
    private IamAdminDtos.RoleSummary toRoleSummary(IamRoleEntity entity) {
        IamAdminDtos.RoleSummary summary = new IamAdminDtos.RoleSummary();
        summary.setId(entity.getId());
        summary.setRoleCode(entity.getRoleCode());
        summary.setRoleName(entity.getRoleName());
        summary.setDescription(entity.getDescription());
        summary.setBuiltIn(entity.getBuiltIn());
        summary.setStatus(entity.getStatus());
        summary.setPermissionIds(queryStrings("SELECT permission_id FROM iam_role_permission WHERE role_id = ? ORDER BY created_at", entity.getId()));
        summary.setPermissionCodes(queryStrings("""
                SELECT p.permission_code
                FROM iam_permission p
                JOIN iam_role_permission rp ON rp.permission_id = p.id
                WHERE rp.role_id = ?
                ORDER BY p.permission_code
                """, entity.getId()));
        summary.setUserCount(count("SELECT COUNT(1) FROM iam_user_role WHERE role_id = ?", entity.getId()));
        return summary;
    }

    /**
     * 转换权限节点。
     *
     * @param entity 权限实体
     * @return 权限节点
     */
    private IamAdminDtos.PermissionNode toPermissionNode(IamPermissionEntity entity) {
        IamAdminDtos.PermissionNode node = new IamAdminDtos.PermissionNode();
        node.setId(entity.getId());
        node.setParentId(entity.getParentId());
        node.setPermissionCode(entity.getPermissionCode());
        node.setPermissionName(entity.getPermissionName());
        node.setPermissionType(entity.getPermissionType());
        node.setRoutePath(entity.getRoutePath());
        node.setApiMethod(entity.getApiMethod());
        node.setApiPath(entity.getApiPath());
        node.setSortOrder(entity.getSortOrder());
        node.setVisible(entity.getVisible());
        node.setStatus(entity.getStatus());
        return node;
    }

    /**
     * 校验当前用户是否具备 IAM 管理权限。
     */
    private void requireIamManager() {
        if (!hasAuthority("ROLE_super_admin") && !hasAuthority("ROLE_admin") && !hasAuthority("iam:manage")) {
            throw new BusinessException("IAM_FORBIDDEN", "没有用户与权限管理权限");
        }
    }

    /**
     * 判断当前认证上下文是否拥有指定权限。
     *
     * @param authority 权限编码
     * @return 是否拥有权限
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(item -> authority.equals(item.getAuthority()));
    }

    /**
     * 查询并校验用户存在。
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    private IamUserEntity requireUser(String id) {
        IamUserEntity user = userMapper.selectById(id);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException("IAM_USER_NOT_FOUND", "用户不存在");
        }
        return user;
    }

    /**
     * 查询并校验部门存在。
     *
     * @param id 部门 ID
     * @return 部门实体
     */
    private IamDepartmentEntity requireDepartment(String id) {
        IamDepartmentEntity entity = departmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("IAM_DEPT_NOT_FOUND", "部门不存在");
        }
        return entity;
    }

    /**
     * 查询并校验角色存在。
     *
     * @param id 角色 ID
     * @return 角色实体
     */
    private IamRoleEntity requireRole(String id) {
        IamRoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("IAM_ROLE_NOT_FOUND", "角色不存在");
        }
        return role;
    }

    /**
     * 查询并校验权限存在。
     *
     * @param id 权限 ID
     */
    private void requirePermission(String id) {
        if (permissionMapper.selectById(id) == null) {
            throw new BusinessException("IAM_PERMISSION_NOT_FOUND", "权限不存在");
        }
    }

    /**
     * 校验部门存在，空部门允许。
     *
     * @param departmentId 部门 ID
     */
    private void ensureDepartmentExists(String departmentId) {
        if (StringUtils.hasText(departmentId)) {
            requireDepartment(departmentId);
        }
    }

    /**
     * 校验父部门存在，空父部门表示根部门。
     *
     * @param parentId 父部门 ID
     */
    private void ensureParentDepartmentExists(String parentId) {
        if (StringUtils.hasText(parentId)) {
            requireDepartment(parentId);
        }
    }

    /**
     * 校验用户名唯一。
     *
     * @param currentId 当前用户 ID
     * @param username 用户名
     */
    private void ensureUsernameUnique(String currentId, String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<IamUserEntity>()
                .eq(IamUserEntity::getUsername, username)
                .ne(StringUtils.hasText(currentId), IamUserEntity::getId, currentId));
        if (count > 0) {
            throw new BusinessException("IAM_USERNAME_EXISTS", "用户名已存在");
        }
    }

    /**
     * 校验部门编码唯一。
     *
     * @param currentId 当前部门 ID
     * @param deptCode 部门编码
     */
    private void ensureDepartmentCodeUnique(String currentId, String deptCode) {
        Long count = departmentMapper.selectCount(new LambdaQueryWrapper<IamDepartmentEntity>()
                .eq(IamDepartmentEntity::getDeptCode, deptCode)
                .ne(StringUtils.hasText(currentId), IamDepartmentEntity::getId, currentId));
        if (count > 0) {
            throw new BusinessException("IAM_DEPT_CODE_EXISTS", "部门编码已存在");
        }
    }

    /**
     * 校验角色编码唯一。
     *
     * @param currentId 当前角色 ID
     * @param roleCode 角色编码
     */
    private void ensureRoleCodeUnique(String currentId, String roleCode) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<IamRoleEntity>()
                .eq(IamRoleEntity::getRoleCode, roleCode)
                .ne(StringUtils.hasText(currentId), IamRoleEntity::getId, currentId));
        if (count > 0) {
            throw new BusinessException("IAM_ROLE_CODE_EXISTS", "角色编码已存在");
        }
    }

    /**
     * 查询字符串列表。
     *
     * @param sql SQL 语句
     * @param args SQL 参数
     * @return 字符串列表
     */
    private List<String> queryStrings(String sql, Object... args) {
        return jdbcTemplate.queryForList(sql, String.class, args);
    }

    /**
     * 查询数量。
     *
     * @param sql SQL 语句
     * @param args SQL 参数
     * @return 数量
     */
    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    /**
     * 清理 ID 列表，去重并过滤空值。
     *
     * @param ids 原始 ID 列表
     * @return 可保存 ID 列表
     */
    private List<String> safeIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /**
     * 规范化启停状态。
     *
     * @param status 原始状态
     * @return enabled 或 disabled
     */
    private String normalizeStatus(String status) {
        String normalized = StringUtils.hasText(status) ? status.toLowerCase(Locale.ROOT) : "enabled";
        return "disabled".equals(normalized) ? "disabled" : "enabled";
    }

    /**
     * 空字符串转 null。
     *
     * @param value 原始字符串
     * @return null 或去空格字符串
     */
    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 生成 UUID 主键。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }
}
