package com.visualbuilder.publish.render;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.publish.config.PublishProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PageRenderer 单元测试 — 覆盖文档骨架、转义、组件数/深度上限与异常路径。
 */
@DisplayName("PageRenderer 页面渲染")
class PageRendererTest {

    private PublishProperties properties;
    private PageRenderer renderer;

    @BeforeEach
    void setUp() {
        properties = new PublishProperties();

        DefaultComponentRenderer defaultRenderer = new DefaultComponentRenderer();
        ComponentRenderer textRenderer = (node, out, ctx) ->
                out.openTag("span").text(node.getPropString("content")).closeTag("span");

        Map<String, ComponentRenderer> renderers = new LinkedHashMap<>();
        renderers.put("default", defaultRenderer);
        renderers.put("text", textRenderer);
        ComponentRendererRegistry registry = new ComponentRendererRegistry(renderers, defaultRenderer);

        renderer = new PageRenderer(registry, new ObjectMapper(), properties);
    }

    @Test
    @DisplayName("渲染完整 HTML 文档：DOCTYPE / head / title / body / 内联样式")
    void rendersFullHtmlDocument() {
        String json = """
                {
                  "meta": {"title": "我的活动页", "description": "测试描述"},
                  "props": {"width": 750, "backgroundColor": "#f0f0f0"},
                  "children": [
                    {"id": "c1", "type": "text", "props": {"content": "你好"}}
                  ]
                }
                """;

        String html = renderer.render(json);

        assertTrue(html.startsWith("<!DOCTYPE html>"), "应以 DOCTYPE 开头");
        assertTrue(html.contains("<html lang=\"zh-CN\">"));
        assertTrue(html.contains("<title>我的活动页</title>"));
        assertTrue(html.contains("name=\"description\" content=\"测试描述\""));
        assertTrue(html.contains("max-width:750px"), "页面宽度应写入基础 CSS");
        assertTrue(html.contains("background-color:#f0f0f0;"), "body 背景色应写入内联样式");
        assertTrue(html.contains("<span>你好</span>"), "子组件应被渲染");
        assertTrue(html.endsWith("</html>"));
    }

    @Test
    @DisplayName("标题与描述中的 HTML 特殊字符被转义")
    void escapesUserContent() {
        String json = """
                {"meta": {"title": "<script>alert(1)</script>", "description": "a\\"b"}}
                """;

        String html = renderer.render(json);

        assertTrue(html.contains("<title>&lt;script&gt;alert(1)&lt;/script&gt;</title>"));
        assertTrue(html.contains("content=\"a&quot;b\""));
    }

    @Test
    @DisplayName("children 缺失时仍能渲染出完整文档")
    void rendersWithoutChildren() {
        String html = renderer.render("{\"meta\": {\"title\": \"空页面\"}}");

        assertTrue(html.contains("<title>空页面</title>"));
        assertTrue(html.contains("class=\"vb-page\""));
        assertTrue(html.endsWith("</html>"));
    }

    @Test
    @DisplayName("组件数超过 maxComponentCount 时停止渲染并输出标记")
    void stopsWhenComponentCountExceeded() {
        properties.setMaxComponentCount(1);
        String json = """
                {"children": [
                   {"type": "text", "props": {"content": "A"}},
                   {"type": "text", "props": {"content": "B"}}
                 ]}
                """;

        String html = renderer.render(json);

        assertTrue(html.contains("<span>A</span>"));
        assertFalse(html.contains("<span>B</span>"), "超限组件不应被渲染");
        assertEquals(1, countOccurrences(html, "<!-- max component count exceeded -->"));
    }

    @Test
    @DisplayName("嵌套深度达到 maxRenderDepth 时停止递归并输出标记")
    void stopsWhenDepthExceeded() {
        properties.setMaxRenderDepth(1);
        String json = """
                {"children": [
                   {"type": "default", "children": [
                     {"type": "default", "children": [
                       {"type": "text", "props": {"content": "deep"}}
                     ]}
                   ]}
                 ]}
                """;

        String html = renderer.render(json);

        assertTrue(html.contains("<!-- max depth exceeded -->"));
        assertFalse(html.contains("<span>deep</span>"), "超深节点不应被渲染");
    }

    @Test
    @DisplayName("上限配置为非正数时回退到默认值，不截断正常页面")
    void fallsBackToDefaultsWhenLimitNotPositive() {
        properties.setMaxComponentCount(0);
        properties.setMaxRenderDepth(0);
        String json = """
                {"children": [
                   {"type": "default", "children": [
                     {"type": "text", "props": {"content": "ok"}}
                   ]}
                 ]}
                """;

        String html = renderer.render(json);

        assertTrue(html.contains("<span>ok</span>"));
        assertFalse(html.contains("exceeded"));
    }

    @Test
    @DisplayName("多次渲染之间组件计数复位，互不影响")
    void resetsComponentCounterBetweenRenders() {
        properties.setMaxComponentCount(1);
        String json = "{\"children\": [{\"type\": \"text\", \"props\": {\"content\": \"A\"}}]}";

        assertFalse(renderer.render(json).contains("exceeded"));
        assertFalse(renderer.render(json).contains("exceeded"),
                "上一次渲染的计数不应泄漏到下一次");
    }

    @Test
    @DisplayName("非法 JSON 抛出渲染失败异常")
    void invalidJsonThrows() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.render("{ not json"));

        assertEquals("页面渲染失败", ex.getMessage());
    }

    private static int countOccurrences(String text, String target) {
        int count = 0;
        int idx = text.indexOf(target);
        while (idx >= 0) {
            count++;
            idx = text.indexOf(target, idx + target.length());
        }
        return count;
    }
}
