package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.RuntimeLlmCallEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * LLM 调用日志 Mapper。
 */
@Mapper
public interface RuntimeLlmCallMapper extends BaseMapper<RuntimeLlmCallEntity> {
}
