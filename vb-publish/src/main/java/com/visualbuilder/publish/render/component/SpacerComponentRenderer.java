package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

/**
 * 间距组件渲染器 — 渲染固定高度的空白 <div>。
 */
@Component("spacer")
public class SpacerComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        Integer height = node.getPropInt("height");
        if (height == null) height = 16;
        String bgColor = node.getPropString("backgroundColor");

        StringBuilder style = new StringBuilder();
        style.append("height:").append(height).append("px;");
        style.append("width:100%;");
        if (bgColor != null) {
            style.append("background-color:").append(bgColor).append(';');
        }

        // 合并 schema styles
        String schemaStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        if (!schemaStyle.isEmpty()) style.append(schemaStyle);

        out.selfClosingTag("div", HtmlBuilder.attrs("style", style.toString()));
    }
}
