package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.PromptBindingEntity;
import org.apache.ibatis.annotations.Mapper;

/** Prompt资源版本绑定Mapper。 */
@Mapper
public interface PromptBindingMapper extends BaseMapper<PromptBindingEntity> {
}
