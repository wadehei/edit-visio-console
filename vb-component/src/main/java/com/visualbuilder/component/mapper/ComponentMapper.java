package com.visualbuilder.component.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visualbuilder.component.entity.Component;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ComponentMapper extends BaseMapper<Component> {
}
