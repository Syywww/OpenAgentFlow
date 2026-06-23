package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.WorkflowDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流定义 Mapper。
 */
@Mapper
public interface WorkflowDefinitionMapper extends BaseMapper<WorkflowDefinitionEntity> {
}
