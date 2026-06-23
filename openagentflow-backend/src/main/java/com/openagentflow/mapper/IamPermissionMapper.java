package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.IamPermissionEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限数据访问 Mapper。
 */
public interface IamPermissionMapper extends BaseMapper<IamPermissionEntity> {

    /**
     * 按用户 ID 查询权限编码。
     *
     * @param userId 用户 ID
     * @return 权限编码列表
     */
    @Select("select distinct p.permission_code "
            + "from iam_permission p "
            + "inner join iam_role_permission rp on rp.permission_id = p.id "
            + "inner join iam_user_role ur on ur.role_id = rp.role_id "
            + "where ur.user_id = #{userId} "
            + "and p.status = 'enabled' "
            + "order by p.permission_code")
    List<String> selectPermissionCodesByUserId(@Param("userId") String userId);
}
