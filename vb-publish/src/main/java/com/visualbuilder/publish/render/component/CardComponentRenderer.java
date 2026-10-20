package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

/**
 * 卡片组件渲染器 — 渲染带标题/描述/内容的卡片容器。
 */
@Component("card")
public class CardComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String title = node.getPropString("title");
        String description = node.getPropString("description");
        String coverUrl = node.getPropString("coverUrl");

        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        String containerStyle = !inlineStyle.isEmpty() ? inlineStyle
                : "background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,.1);";

        out.openTag("div", HtmlBuilder.attrs("class", "vb-card", "style", containerStyle));

        // 封面图
        if (coverUrl != null && !coverUrl.isEmpty()) {
            out.selfClosingTag("img", HtmlBuilder.attrs("src", coverUrl, "style",
                    "width:100%;height:auto;display:block;"));
        }

        // 卡片内容区
        out.openTag("div", HtmlBuilder.attrs("style", "padding:12px 16px;"));

        // 标题
        if (title != null && !title.isEmpty()) {
            out.openTag("div", HtmlBuilder.attrs("style",
                    "font-size:16px;font-weight:600;color:#333;margin-bottom:4px;"))
               .text(title).closeTag("div");
        }

        // 描述
        if (description != null && !description.isEmpty()) {
            out.openTag("div", HtmlBuilder.attrs("style",
                    "font-size:13px;color:#999;margin-bottom:8px;"))
               .text(description).closeTag("div");
        }

        // 子节点内容
        if (node.hasChildren()) {
            ctx.renderChildren(node.getChildren(), out);
        }

        out.closeTag("div"); // 内容区
        out.closeTag("div"); // 卡片容器
    }
}
