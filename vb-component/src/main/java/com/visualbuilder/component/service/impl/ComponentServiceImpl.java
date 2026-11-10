package com.visualbuilder.component.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.LimitsConst;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.util.TransactionUtils;
import com.visualbuilder.component.dto.ComponentCreateDTO;
import com.visualbuilder.component.dto.ComponentUpdateDTO;
import com.visualbuilder.component.dto.ComponentVO;
import com.visualbuilder.component.entity.Component;
import com.visualbuilder.component.mapper.ComponentMapper;
import com.visualbuilder.component.service.ComponentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentServiceImpl extends BaseServiceImpl<ComponentMapper, Component>
        implements ComponentService {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_KEY_ALL = "component:list:all";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /** 列表不需要完整定义，查询时排除三个大字段 */
    private static final Set<String> SCHEMA_COLUMNS =
            Set.of("schema_define", "default_props", "default_styles");

    @Override
    public List<ComponentVO> listComponents(Long categoryId, String keyword) {
        // 无过滤条件时使用缓存
        if (categoryId == null && !StringUtils.hasText(keyword)) {
            try {
                String cached = redisTemplate.opsForValue().get(CACHE_KEY_ALL);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<List<ComponentVO>>() {});
                }
            } catch (Exception e) {
                log.warn("读取组件缓存失败: {}", e.getMessage());
            }
        }

        LambdaQueryWrapper<Component> wrapper = new LambdaQueryWrapper<>();
        // 列表不需要完整定义，排除大字段避免无谓的 IO、反序列化与缓存膨胀
        wrapper.select(Component.class, info -> !SCHEMA_COLUMNS.contains(info.getColumn()));
        wrapper.eq(categoryId != null, Component::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Component::getName, keyword)
                .eq(Component::getStatus, StatusConst.Enable.ENABLED)
                .orderByAsc(Component::getSortOrder)
                .last("LIMIT " + LimitsConst.MAX_LIST_SIZE);
        List<Component> list = this.list(wrapper);
        List<ComponentVO> result = list.stream().map(this::toVO).toList();

        // 写入缓存
        if (categoryId == null && !StringUtils.hasText(keyword)) {
            try {
                redisTemplate.opsForValue().set(CACHE_KEY_ALL,
                        objectMapper.writeValueAsString(result), CACHE_TTL);
            } catch (Exception e) {
                log.warn("写入组件缓存失败: {}", e.getMessage());
            }
        }

        return result;
    }

    @Override
    public ComponentVO getComponentDetail(Long componentId) {
        Component component = this.getById(componentId);
        if (component == null) throw new BusinessException(ErrorCode.COMPONENT_NOT_FOUND, "组件不存在");
        return toVO(component);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void createComponent(ComponentCreateDTO dto) {
        Component component = new Component();
        component.setName(dto.getName());
        component.setType(dto.getType());
        component.setCategoryId(dto.getCategoryId());
        component.setDescription(dto.getDescription());
        component.setThumbnail(dto.getThumbnail());
        component.setSchemaDefine(dto.getSchemaDefine());
        component.setDefaultProps(dto.getDefaultProps());
        component.setDefaultStyles(dto.getDefaultStyles());
        component.setSortOrder(dto.getSortOrder());
        component.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusConst.Enable.ENABLED);
        this.save(component);
        evictCache();
        log.info("创建组件: name={}", component.getName());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void updateComponent(Long componentId, ComponentUpdateDTO dto) {
        Component component = this.getById(componentId);
        if (component == null) throw new BusinessException(ErrorCode.COMPONENT_NOT_FOUND, "组件不存在");
        if (dto.getName() != null) component.setName(dto.getName());
        if (dto.getDescription() != null) component.setDescription(dto.getDescription());
        if (dto.getThumbnail() != null) component.setThumbnail(dto.getThumbnail());
        if (dto.getSchemaDefine() != null) component.setSchemaDefine(dto.getSchemaDefine());
        if (dto.getDefaultProps() != null) component.setDefaultProps(dto.getDefaultProps());
        if (dto.getDefaultStyles() != null) component.setDefaultStyles(dto.getDefaultStyles());
        if (dto.getCategoryId() != null) component.setCategoryId(dto.getCategoryId());
        if (dto.getSortOrder() != null) component.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) component.setStatus(dto.getStatus());
        this.updateById(component);
        evictCache();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void deleteComponent(Long componentId) {
        Component component = this.getById(componentId);
        if (component == null) throw new BusinessException(ErrorCode.COMPONENT_NOT_FOUND, "组件不存在");
        this.removeById(componentId);
        evictCache();
    }

    /**
     * 清除组件列表缓存（供管理操作调用）
     */
    public void evictCache() {
        // 事务提交后再失效缓存，避免「缓存已清、事务回滚」造成的不一致
        TransactionUtils.executeAfterCommit(() -> redisTemplate.delete(CACHE_KEY_ALL));
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
                .isSystem(c.getIsSystem() != null && c.getIsSystem() == StatusConst.YesNo.YES)
                .sortOrder(c.getSortOrder())
                .build();
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("JSON 解析失败, 内容长度={}: {}", json.length(), e.getMessage());
            return Collections.emptyMap();
        }
    }
}
