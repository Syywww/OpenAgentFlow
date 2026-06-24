package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.PromptTemplateVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提示词模板版本数据访问 Mapper。
 */
@Mapper
public interface PromptTemplateVersionMapper extends BaseMapper<PromptTemplateVersionEntity> {
}
