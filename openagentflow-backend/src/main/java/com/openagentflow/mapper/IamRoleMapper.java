package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.IamRoleEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色数据访问 Mapper。
 */
public interface IamRoleMapper extends BaseMapper<IamRoleEntity> {

    /**
     * 按用户 ID 查询角色编码。
     *
     * @param userId 用户 ID
     * @return 角色编码列表
     */
    @Select("select r.role_code "
            + "from iam_role r "
            + "inner join iam_user_role ur on ur.role_id = r.id "
            + "where ur.user_id = #{userId} "
            + "and r.status = 'enabled' "
            + "order by r.role_code")
    List<String> selectRoleCodesByUserId(@Param("userId") String userId);
}
