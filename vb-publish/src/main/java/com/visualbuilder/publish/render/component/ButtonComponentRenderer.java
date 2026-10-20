package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

/**
 * 按钮组件渲染器 — 渲染 <button> 或 <a>（有链接时）。
 */
@Component("button")
public class ButtonComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String text = node.getPropString("text");
        if (text == null) text = node.getPropString("content");
        if (text == null) text = "按钮";

        String link = node.getPropString("link");
        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        String type = node.getPropString("buttonType"); // primary, default, dashed, text

        // 按钮基础样式
        StringBuilder style = new StringBuilder(getBaseButtonStyle(type));
        if (!inlineStyle.isEmpty()) style.append(inlineStyle);

        if (link != null && !link.isEmpty()) {
            out.openTag("a", HtmlBuilder.attrs("href", link, "style", style.toString()))
               .text(text).closeTag("a");
        } else {
            out.openTag("button", HtmlBuilder.attrs("type", "button", "style", style.toString()))
               .text(text).closeTag("button");
        }
    }

    private String getBaseButtonStyle(String type) {
        String base = "display:inline-block;padding:8px 20px;border-radius:4px;cursor:pointer;text-align:center;";
        return switch (type != null ? type : "default") {
            case "primary" -> base + "background:#1677ff;color:#fff;border:1px solid #1677ff;";
            case "dashed" -> base + "background:transparent;color:#333;border:1px dashed #d9d9d9;";
            case "text" -> base + "background:transparent;color:#1677ff;border:none;";
            default -> base + "background:#fff;color:#333;border:1px solid #d9d9d9;";
        };
    }
}
