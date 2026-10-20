package com.visualbuilder.publish.render;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 页面渲染器 — 将 PageSchema JSON 渲染为完整的 HTML 页面。
 *
 * <p>负责：
 * <ul>
 *   <li>解析 schemaContent JSON → SchemaNode 树</li>
 *   <li>生成 HTML 文档结构（head/body）</li>
 *   <li>注入基础 CSS 重置样式</li>
 *   <li>遍历组件树，分发到对应 ComponentRenderer</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PageRenderer {

    private static final int DEFAULT_MAX_DEPTH = 50;
    private static final int DEFAULT_MAX_COMPONENTS = 1000;

    private final ComponentRendererRegistry registry;
    private final ObjectMapper objectMapper;

    /** 组件计数器（线程不安全，仅用于单次渲染过程） */
    private int componentCount;

    /**
     * 将 PageSchema JSON 字符串渲染为完整 HTML
     *
     * @param schemaJson 页面 schema JSON
     * @return 完整 HTML 字符串
     */
    public String render(String schemaJson) {
        componentCount = 0;
        try {
            // 解析顶层 schema
            Map<String, Object> schema = objectMapper.readValue(
                    schemaJson, new TypeReference<Map<String, Object>>() {});

            // 提取 meta / props / children
            Map<String, Object> meta = getMap(schema, "meta");
            Map<String, Object> props = getMap(schema, "props");
            List<SchemaNode> children = parseChildren(schema.get("children"));

            HtmlBuilder html = new HtmlBuilder(schemaJson.length() * 2);

            // ─── DOCTYPE + <html> ───
            html.append("<!DOCTYPE html>").newLine();
            html.append("<html lang=\"zh-CN\">").newLine();

            // ─── <head> ───
            renderHead(html, meta, props);

            // ─── <body> ───
            renderBody(html, props, children);

            html.append("</html>");
            return html.toString();

        } catch (Exception e) {
            log.error("页面渲染失败: " + e.getMessage(), e);
            throw new RuntimeException("页面渲染失败", e);
        }
    }

    /**
     * 渲染单个节点（由 ComponentRenderer.RenderContext 调用）
     */
    public void renderNode(SchemaNode node, HtmlBuilder out, ComponentRenderer.RenderContext ctx) {
        componentCount++;
        if (componentCount > DEFAULT_MAX_COMPONENTS) {
            out.append("<!-- max component count exceeded -->");
            return;
        }
        ComponentRenderer renderer = registry.getRenderer(node.getType());
        renderer.render(node, out, ctx);
    }

    // ─── 私有方法 ───────────────────────────────────────────────

    private void renderHead(HtmlBuilder html, Map<String, Object> meta, Map<String, Object> props) {
        String title = getString(meta, "title", "可视化页面");
        String description = getString(meta, "description", "");
        int pageWidth = getInt(props, "width", 375);

        html.openTag("head").newLine();
        html.append("<meta charset=\"UTF-8\">").newLine();
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">").newLine();
        html.openTag("title").text(title).closeTag("title").newLine();
        if (!description.isEmpty()) {
            html.openTag("meta", HtmlBuilder.attrs("name", "description", "content", description)).newLine();
        }
        // 基础 CSS 重置 + 移动端适配
        html.style(getBaseCss(pageWidth));
        html.closeTag("head").newLine();
    }

    private void renderBody(HtmlBuilder html, Map<String, Object> props, List<SchemaNode> children) {
        String bgColor = getString(props, "backgroundColor", "#ffffff");
        String bgImage = getString(props, "backgroundImage", "");

        // body 样式
        StringBuilder bodyStyle = new StringBuilder();
        bodyStyle.append("background-color:").append(bgColor).append(';');
        if (!bgImage.isEmpty()) {
            bodyStyle.append("background-image:url('").append(bgImage).append("');");
            bodyStyle.append("background-size:cover;background-position:center;");
        }
        String padding = getString(props, "padding", "");
        if (!padding.isEmpty()) {
            bodyStyle.append("padding:").append(padding).append(';');
        }

        html.openTag("body", HtmlBuilder.attrs("style", bodyStyle.toString()));

        // 页面内容容器
        html.openTag("div", HtmlBuilder.attrs("class", "vb-page", "style", "min-height:100vh;"));

        // 渲染所有子组件
        ComponentRenderer.RenderContext ctx = new ComponentRenderer.RenderContext(
                0, DEFAULT_MAX_DEPTH, this);
        for (SchemaNode child : children) {
            renderNode(child, html, ctx);
        }

        html.closeTag("div"); // .vb-page
        html.closeTag("body").newLine();
    }

    @SuppressWarnings("unchecked")
    private List<SchemaNode> parseChildren(Object childrenObj) {
        if (childrenObj == null) return List.of();
        if (childrenObj instanceof List<?> list) {
            try {
                String json = objectMapper.writeValueAsString(list);
                return objectMapper.readValue(json, new TypeReference<List<SchemaNode>>() {});
            } catch (Exception e) {
                log.error("解析 children 失败: " + e.getMessage(), e);
                return List.of();
            }
        }
        return List.of();
    }

    private String getBaseCss(int pageWidth) {
        return """
                *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
                html{-webkit-text-size-adjust:100%;-webkit-font-smoothing:antialiased}
                body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;
                     font-size:14px;line-height:1.5;color:#333;max-width:%dpx;margin:0 auto;overflow-x:hidden}
                img,video{max-width:100%%;height:auto;display:block}
                a{color:inherit;text-decoration:none}
                button,input,textarea{font:inherit;border:none;outline:none;background:none}
                .vb-container{display:flex;width:100%%}
                .vb-card{background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,.1)}
                .vb-badge{display:inline-block;padding:2px 8px;border-radius:10px;font-size:12px}
                .vb-progress-bar{width:100%%;background:#e9ecef;border-radius:4px;overflow:hidden}
                .vb-progress-fill{height:100%%;border-radius:4px;transition:width .3s}
                """.formatted(pageWidth);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> source, String key) {
        Object val = source.get(key);
        return val instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private static int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }
}
