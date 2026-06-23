package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.WorkflowNodeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流节点 Mapper。
 */
@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNodeEntity> {
}
