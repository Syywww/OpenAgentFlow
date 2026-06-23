package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.WorkflowRunEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流运行记录 Mapper。
 */
@Mapper
public interface WorkflowRunMapper extends BaseMapper<WorkflowRunEntity> {
}
