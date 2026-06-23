package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.WorkflowVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流发布版本 Mapper。
 */
@Mapper
public interface WorkflowVersionMapper extends BaseMapper<WorkflowVersionEntity> {
}
