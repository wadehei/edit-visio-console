package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 输入框组件渲染器 — 渲染 <input> 或 <textarea>。
 */
@Component("input")
public class InputComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String inputType = node.getPropString("inputType"); // text, number, password, textarea
        String placeholder = node.getPropString("placeholder");
        String label = node.getPropString("label");

        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        String style = !inlineStyle.isEmpty() ? inlineStyle
                : "width:100%;padding:8px 12px;border:1px solid #d9d9d9;border-radius:4px;font-size:14px;";

        Map<String, String> attrs = new LinkedHashMap<>();
        if (placeholder != null) attrs.put("placeholder", placeholder);
        attrs.put("style", style);

        // 容器
        out.openTag("div", HtmlBuilder.attrs("style", "margin-bottom:12px;"));

        // label
        if (label != null && !label.isEmpty()) {
            out.openTag("label", HtmlBuilder.attrs("style", "display:block;margin-bottom:4px;font-size:14px;color:#333;"))
               .text(label).closeTag("label");
        }

        if ("textarea".equals(inputType)) {
            Integer rows = node.getPropInt("rows");
            if (rows != null) attrs.put("rows", String.valueOf(rows));
            else attrs.put("rows", "4");
            out.openTag("textarea", attrs).closeTag("textarea");
        } else {
            attrs.put("type", inputType != null ? inputType : "text");
            out.selfClosingTag("input", attrs);
        }

        out.closeTag("div");
    }
}
