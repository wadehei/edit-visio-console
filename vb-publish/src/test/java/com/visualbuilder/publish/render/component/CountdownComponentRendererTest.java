package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CountdownComponentRenderer 回归测试 — 内联 JS 是发布产物唯一的脚本注入点,
 * 全部插值参数必须经白名单校验(存储型 XSS 防线,见 ADR 与独立审查 P0-2)。
 */
@DisplayName("Countdown 渲染器 JS 注入防线")
class CountdownComponentRendererTest {

    private final CountdownComponentRenderer renderer = new CountdownComponentRenderer();

    private String render(SchemaNode node) {
        HtmlBuilder out = new HtmlBuilder();
        renderer.render(node, out,
                new com.visualbuilder.publish.render.ComponentRenderer.RenderContext(0, 5, null));
        return out.toString();
    }

    private SchemaNode node(String id, String targetDate) {
        SchemaNode node = new SchemaNode();
        node.setId(id);
        node.setType("countdown");
        Map<String, Object> props = new HashMap<>();
        if (targetDate != null) {
            props.put("targetDate", targetDate);
        }
        node.setProps(props);
        return node;
    }

    @Test
    @DisplayName("合法 targetDate 与 id 正常渲染内联脚本")
    void rendersValidInput() {
        String html = render(node("cd_1", "2026-12-31T23:59:59"));
        assertTrue(html.contains("new Date('2026-12-31T23:59:59')"));
        assertTrue(html.contains("getElementById('cd_1')"));
    }

    @Test
    @DisplayName("targetDate 反斜杠逃逸注入被白名单拦截,回退默认值")
    void rejectsBackslashEscapeInTargetDate() {
        // 旧实现仅替换单引号:反斜杠会先闭合 JS 字符串字面量,后续 alert 得以执行
        String payload = "\\';alert(document.cookie)//";
        String html = render(node("cd_1", payload));
        assertFalse(html.contains("alert(document.cookie)"), "注入代码不得出现在产物中");
        assertTrue(html.contains("new Date('2099-12-31T23:59:59')"), "非法值回退默认时间");
    }

    @Test
    @DisplayName("compId 携带 HTML/引号字符时替换为安全 id")
    void sanitizesUnsafeCompId() {
        SchemaNode node = node("x'); alert(1); ('", "2026-12-31T23:59:59");
        String html = render(node);
        assertFalse(html.contains("alert(1)"), "注入代码不得出现在产物中");
        // 兜底 id 形如 cd_<nanoTime>,全部安全字符
        assertTrue(html.matches("(?s).*getElementById\\('cd_\\d+'\\).*"));
    }

    @Test
    @DisplayName("缺失 targetDate 使用默认值")
    void defaultsWhenMissing() {
        String html = render(node("cd_1", null));
        assertTrue(html.contains("new Date('2099-12-31T23:59:59')"));
    }
}
