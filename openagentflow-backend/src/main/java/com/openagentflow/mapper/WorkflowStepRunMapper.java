package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.WorkflowStepRunEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流节点运行记录 Mapper。
 */
@Mapper
public interface WorkflowStepRunMapper extends BaseMapper<WorkflowStepRunEntity> {
}
