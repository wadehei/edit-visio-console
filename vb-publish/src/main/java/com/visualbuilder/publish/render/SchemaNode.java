package com.visualbuilder.publish.render;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Schema 树节点 POJO — 递归反序列化 PageSchema 的 children 结构。
 *
 * <p>每个节点对应页面中的一个组件实例，包含类型、属性、样式和可选的子节点。</p>
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaNode {

    /** 组件实例 ID，如 "comp_001" */
    private String id;

    /** 组件类型，如 "text", "image", "container" */
    private String type;

    /** 组件属性（内容相关），如 content、src、direction */
    private Map<String, Object> props;

    /** CSS 样式（展示相关），如 backgroundColor、fontSize */
    private Map<String, Object> styles;

    /** 事件绑定配置（暂不渲染，保留扩展） */
    private List<Map<String, Object>> events;

    /** 子节点列表（容器类组件有，叶子组件无） */
    private List<SchemaNode> children;

    // ─── 便捷方法 ───────────────────────────────────────────────

    public Map<String, Object> getProps() {
        return props != null ? props : Collections.emptyMap();
    }

    public Map<String, Object> getStyles() {
        return styles != null ? styles : Collections.emptyMap();
    }

    public List<SchemaNode> getChildren() {
        return children != null ? children : Collections.emptyList();
    }

    /** 获取 prop 值，不存在返回 null */
    public Object getProp(String key) {
        return getProps().get(key);
    }

    /** 获取 prop 字符串值 */
    public String getPropString(String key) {
        Object val = getProp(key);
        return val != null ? val.toString() : null;
    }

    /** 获取 prop 整数值 */
    public Integer getPropInt(String key) {
        Object val = getProp(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    /** 获取 prop 布尔值 */
    public Boolean getPropBool(String key) {
        Object val = getProp(key);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return null;
    }

    /** 获取 style 值 */
    public Object getStyle(String key) {
        return getStyles().get(key);
    }

    /** 是否有子节点 */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
