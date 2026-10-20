package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

/**
 * 分割线组件渲染器 — 渲染 <hr>。
 */
@Component("divider")
public class DividerComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String color = node.getPropString("color");
        String width = node.getPropString("width");

        StringBuilder style = new StringBuilder("border:none;");
        if (color != null) {
            style.append("border-top:1px solid ").append(color).append(';');
        } else {
            style.append("border-top:1px solid #e8e8e8;");
        }
        if (width != null) {
            if (width.matches("\\d+")) width = width + "px";
            style.append("width:").append(width).append(';');
            style.append("margin-left:auto;margin-right:auto;");
        } else {
            style.append("width:100%;");
        }
        style.append("margin:8px 0;");

        // 合并 schema styles
        String schemaStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        if (!schemaStyle.isEmpty()) style.append(schemaStyle);

        out.selfClosingTag("hr", HtmlBuilder.attrs("style", style.toString()));
    }
}
