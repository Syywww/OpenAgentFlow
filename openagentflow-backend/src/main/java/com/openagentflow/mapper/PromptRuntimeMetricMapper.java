package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.PromptRuntimeMetricEntity;
import org.apache.ibatis.annotations.Mapper;

/** Prompt运行指标Mapper。 */
@Mapper
public interface PromptRuntimeMetricMapper extends BaseMapper<PromptRuntimeMetricEntity> {
}
