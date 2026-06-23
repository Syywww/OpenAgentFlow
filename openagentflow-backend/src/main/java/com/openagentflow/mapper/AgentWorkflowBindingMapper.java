package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.AgentWorkflowBindingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 与工作流绑定 Mapper。
 */
@Mapper
public interface AgentWorkflowBindingMapper extends BaseMapper<AgentWorkflowBindingEntity> {
}
