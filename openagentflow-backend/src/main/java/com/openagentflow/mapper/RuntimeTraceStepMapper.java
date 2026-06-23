package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.RuntimeTraceStepEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运行链路步骤 Mapper。
 */
@Mapper
public interface RuntimeTraceStepMapper extends BaseMapper<RuntimeTraceStepEntity> {
}
