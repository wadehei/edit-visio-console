package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

/**
 * 富文本组件渲染器 — 渲染原始 HTML 内容（轻量白名单过滤）。
 */
@Component("richText")
public class RichTextComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String content = node.getPropString("content");
        if (content == null || content.isEmpty()) return;

        // 轻量 XSS 过滤：移除 <script>、on* 事件、javascript: URL
        String safeContent = sanitize(content);

        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        if (!inlineStyle.isEmpty()) {
            out.openTag("div", HtmlBuilder.attrs("style", inlineStyle))
               .rawHtml(safeContent).closeTag("div");
        } else {
            out.rawHtml(safeContent);
        }
    }

    /**
     * 轻量 HTML 白名单过滤 — 移除危险标签和属性。
     * 保留常用排版标签，移除脚本和事件。
     */
    private String sanitize(String html) {
        // 移除 <script>...</script>
        String result = html.replaceAll("(?i)<script[^>]*>[\\s\\S]*?</script>", "");
        // 移除 <iframe>...</iframe>
        result = result.replaceAll("(?i)<iframe[^>]*>[\\s\\S]*?</iframe>", "");
        result = result.replaceAll("(?i)<iframe[^>]*/?>", "");
        // 移除 on* 事件属性
        result = result.replaceAll("(?i)\\s+on\\w+\\s*=\\s*\"[^\"]*\"", "");
        result = result.replaceAll("(?i)\\s+on\\w+\\s*=\\s*'[^']*'", "");
        // 移除 javascript: URL
        result = result.replaceAll("(?i)javascript\\s*:", "");
        // 移除 <object>、<embed>、<form>
        result = result.replaceAll("(?i)<(object|embed|form)[^>]*>[\\s\\S]*?</\\1>", "");
        result = result.replaceAll("(?i)<(object|embed|form)[^>]*/?>", "");
        return result;
    }
}
