package com.visualbuilder.publish.render;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 组件渲染器注册表 — 通过 Spring 自动注入所有 ComponentRenderer bean。
 *
 * <p>按组件 type 查找对应渲染器，找不到则使用 {@link DefaultComponentRenderer}。</p>
 */
@Component
public class ComponentRendererRegistry {

    private final Map<String, ComponentRenderer> rendererMap;
    private final DefaultComponentRenderer defaultRenderer;

    public ComponentRendererRegistry(
            Map<String, ComponentRenderer> rendererMap,
            DefaultComponentRenderer defaultRenderer) {
        this.rendererMap = rendererMap;
        this.defaultRenderer = defaultRenderer;
    }

    /**
     * 根据组件类型获取渲染器
     *
     * @param type 组件类型（如 "text", "image"）
     * @return 对应渲染器，未找到返回默认渲染器
     */
    public ComponentRenderer getRenderer(String type) {
        if (type == null || type.isEmpty()) {
            return defaultRenderer;
        }
        return rendererMap.getOrDefault(type, defaultRenderer);
    }
}
