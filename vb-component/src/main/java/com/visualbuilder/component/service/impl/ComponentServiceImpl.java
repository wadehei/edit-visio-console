package com.visualbuilder.component.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.component.dto.ComponentVO;
import com.visualbuilder.component.entity.Component;
import com.visualbuilder.component.mapper.ComponentMapper;
import com.visualbuilder.component.service.ComponentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentServiceImpl extends BaseServiceImpl<ComponentMapper, Component>
        implements ComponentService {

    private final ObjectMapper objectMapper;

    @Override
    public List<ComponentVO> listComponents(Long categoryId, String keyword) {
        LambdaQueryWrapper<Component> wrapper = new LambdaQueryWrapper<Component>()
                .eq(categoryId != null, Component::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Component::getName, keyword)
                .eq(Component::getStatus, 1)
                .orderByAsc(Component::getSortOrder);
        List<Component> list = this.list(wrapper);
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public ComponentVO getComponentDetail(Long componentId) {
        Component component = this.getById(componentId);
        if (component == null) throw new BusinessException(40005, "组件不存在");
        return toVO(component);
    }

    private ComponentVO toVO(Component c) {
        return ComponentVO.builder()
                .componentId(c.getId())
                .name(c.getName())
                .type(c.getType())
                .description(c.getDescription())
                .thumbnail(c.getThumbnail())
                .propsSchema(parseJson(c.getSchemaDefine()))
                .defaultProps(parseJson(c.getDefaultProps()))
                .defaultStyles(parseJson(c.getDefaultStyles()))
                .isSystem(c.getIsSystem() == 1)
                .sortOrder(c.getSortOrder())
                .build();
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
