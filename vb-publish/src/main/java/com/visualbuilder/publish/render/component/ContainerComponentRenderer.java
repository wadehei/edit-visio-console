package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

/**
 * 容器组件渲染器 — 渲染 <div> + flex 布局，递归渲染子节点。
 */
@Component("container")
public class ContainerComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String direction = node.getPropString("direction"); // row / column
        String gap = node.getPropString("gap");
        String wrap = node.getPropString("wrap");

        // 构建容器样式
        StringBuilder style = new StringBuilder("display:flex;");
        style.append("flex-direction:").append(direction != null ? direction : "column").append(';');
        if (gap != null) {
            // gap 可能是 "16px" 或数字 16
            if (gap.matches("\\d+")) gap = gap + "px";
            style.append("gap:").append(gap).append(';');
        }
        if ("true".equals(wrap) || "wrap".equals(wrap)) {
            style.append("flex-wrap:wrap;");
        }
        style.append("width:100%;");

        // 合并 schema styles
        String schemaStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        if (!schemaStyle.isEmpty()) style.append(schemaStyle);

        out.openTag("div", HtmlBuilder.attrs("style", style.toString()));

        // 递归渲染子节点
        if (node.hasChildren()) {
            ctx.renderChildren(node.getChildren(), out);
        }

        out.closeTag("div");
    }
}
