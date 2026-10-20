package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 音频组件渲染器 — 渲染 <audio> 标签。
 */
@Component("audio")
public class AudioComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String src = node.getPropString("src");
        if (src == null || src.isEmpty()) {
            out.openTag("div", HtmlBuilder.attrs("style",
                    "padding:12px;background:#f5f5f5;border-radius:4px;color:#999;text-align:center;"))
               .text("暂无音频").closeTag("div");
            return;
        }

        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("src", src);
        attrs.put("controls", "controls");
        attrs.put("preload", "metadata");

        Boolean autoplay = node.getPropBool("autoplay");
        if (Boolean.TRUE.equals(autoplay)) {
            attrs.put("autoplay", "autoplay");
        }

        Boolean loop = node.getPropBool("loop");
        if (Boolean.TRUE.equals(loop)) {
            attrs.put("loop", "loop");
        }

        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        String style = !inlineStyle.isEmpty() ? inlineStyle : "width:100%;";
        attrs.put("style", style);

        out.openTag("audio", attrs).closeTag("audio");
    }
}
