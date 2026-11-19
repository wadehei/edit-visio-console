package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * 富文本组件渲染器 — 渲染经 Jsoup 白名单过滤后的 HTML 内容。
 */
@Component("richText")
public class RichTextComponentRenderer implements ComponentRenderer {

    /**
     * HTML 白名单：仅保留常用排版标签与安全属性，其余标签/属性一律剔除。
     *
     * <p>相比正则会话过滤，白名单解析能正确处理大小写混写、属性无引号、HTML 实体编码、
     * {@code <svg/onload=...>} 等绕过手法，避免发布公开页面时的存储型 XSS。</p>
     */
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("video", "source", "figure", "figcaption", "section", "mark")
            .addAttributes(":all", "style", "class")
            .addAttributes("a", "target", "rel")
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes("video", "src", "poster", "controls", "width", "height")
            .addAttributes("source", "src", "type")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("source", "src", "http", "https")
            .preserveRelativeLinks(true);

    private static final Document.OutputSettings OUTPUT_SETTINGS =
            new Document.OutputSettings().prettyPrint(false);

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String content = node.getPropString("content");
        if (content == null || content.isEmpty()) return;

        // 白名单过滤：移除脚本、事件属性、危险协议等
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
     * 使用 Jsoup 白名单清洗富文本 HTML，保留安全排版标签、剔除脚本与危险属性。
     */
    private String sanitize(String html) {
        return Jsoup.clean(html, "", SAFELIST, OUTPUT_SETTINGS);
    }
}
