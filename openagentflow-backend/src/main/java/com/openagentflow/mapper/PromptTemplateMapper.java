package com.openagentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openagentflow.entity.PromptTemplateEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提示词模板数据访问 Mapper。
 */
@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplateEntity> {
}
