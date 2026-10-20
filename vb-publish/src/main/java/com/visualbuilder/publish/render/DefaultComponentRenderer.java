package com.visualbuilder.publish.render;

import org.springframework.stereotype.Component;

/**
 * 默认/降级渲染器 — 处理未知组件类型。
 * 仅渲染子节点，跳过当前节点的内容/样式。
 */
@Component("default")
public class DefaultComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        // 未知类型：用 div 包裹，应用样式，递归渲染子节点
        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        if (!inlineStyle.isEmpty()) {
            out.openTag("div", HtmlBuilder.attrs("style", inlineStyle));
        } else {
            out.openTag("div");
        }
        if (node.hasChildren()) {
            ctx.renderChildren(node.getChildren(), out);
        }
        out.closeTag("div");
    }
}
