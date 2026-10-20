package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

/**
 * 文本组件渲染器 — 根据 fontSize 选择 h1-h6/p/span 标签。
 */
@Component("text")
public class TextComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String content = node.getPropString("content");
        if (content == null) content = "";

        Integer fontSize = node.getPropInt("fontSize");
        String tag = determineTag(fontSize);
        String inlineStyle = buildStyle(node, fontSize);

        if (inlineStyle.isEmpty()) {
            out.openTag(tag).text(content).closeTag(tag);
        } else {
            out.openTag(tag, HtmlBuilder.attrs("style", inlineStyle)).text(content).closeTag(tag);
        }
    }

    private String determineTag(Integer fontSize) {
        if (fontSize == null) return "p";
        if (fontSize >= 32) return "h1";
        if (fontSize >= 28) return "h2";
        if (fontSize >= 24) return "h3";
        if (fontSize >= 20) return "h4";
        if (fontSize >= 18) return "h5";
        return "p";
    }

    private String buildStyle(SchemaNode node, Integer fontSize) {
        StringBuilder style = new StringBuilder();
        String color = node.getPropString("color");
        if (color != null) style.append("color:").append(color).append(';');

        // 合并 schema styles
        String schemaStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        if (!schemaStyle.isEmpty()) style.append(schemaStyle);

        return style.toString();
    }
}
