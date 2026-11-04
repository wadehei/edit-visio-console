package com.visualbuilder.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visualbuilder.template.entity.Template;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TemplateMapper extends BaseMapper<Template> {

    @Update("UPDATE biz_template SET use_count = use_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementUseCount(@Param("id") Long id);
}
