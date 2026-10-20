package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 图片组件渲染器 — 渲染 <img> 标签。
 */
@Component("image")
public class ImageComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String src = node.getPropString("src");
        if (src == null || src.isEmpty()) {
            // 无图片源时渲染占位符
            out.openTag("div", HtmlBuilder.attrs("style",
                    "width:100%;height:120px;background:#f5f5f5;display:flex;align-items:center;justify-content:center;color:#999;"))
               .text("暂无图片").closeTag("div");
            return;
        }

        String alt = node.getPropString("alt");
        if (alt == null) alt = "";

        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("src", src);
        attrs.put("alt", alt);

        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        if (!inlineStyle.isEmpty()) {
            attrs.put("style", inlineStyle);
        }

        // 处理 lazy loading
        Boolean lazy = node.getPropBool("lazy");
        if (Boolean.TRUE.equals(lazy)) {
            attrs.put("loading", "lazy");
        }

        // 处理 objectFit
        String objectFit = node.getPropString("objectFit");
        if (objectFit != null) {
            String existing = attrs.getOrDefault("style", "");
            attrs.put("style", existing + "object-fit:" + objectFit + ";");
        }

        out.selfClosingTag("img", attrs);
    }
}
