package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

/**
 * 徽标组件渲染器 — 渲染 <span class="vb-badge">。
 */
@Component("badge")
public class BadgeComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String text = node.getPropString("text");
        if (text == null) text = "";

        String color = node.getPropString("color");
        String bgColor = node.getPropString("backgroundColor");

        StringBuilder style = new StringBuilder();
        style.append("display:inline-block;padding:2px 8px;border-radius:10px;font-size:12px;line-height:1.5;");
        if (bgColor != null) {
            style.append("background-color:").append(bgColor).append(';');
        } else {
            style.append("background-color:#1677ff;");
        }
        if (color != null) {
            style.append("color:").append(color).append(';');
        } else {
            style.append("color:#fff;");
        }

        // 合并 schema styles
        String schemaStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        if (!schemaStyle.isEmpty()) style.append(schemaStyle);

        out.openTag("span", HtmlBuilder.attrs("class", "vb-badge", "style", style.toString()))
           .text(text).closeTag("span");
    }
}
