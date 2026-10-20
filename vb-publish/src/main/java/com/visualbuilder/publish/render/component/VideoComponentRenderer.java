package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 视频组件渲染器 — 渲染 <video> 标签。
 */
@Component("video")
public class VideoComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String src = node.getPropString("src");
        if (src == null || src.isEmpty()) {
            out.openTag("div", HtmlBuilder.attrs("style",
                    "width:100%;height:200px;background:#000;display:flex;align-items:center;justify-content:center;color:#999;"))
               .text("暂无视频").closeTag("div");
            return;
        }

        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("src", src);
        attrs.put("controls", "controls");
        attrs.put("preload", "metadata");

        Boolean autoplay = node.getPropBool("autoplay");
        if (Boolean.TRUE.equals(autoplay)) {
            attrs.put("autoplay", "autoplay");
            attrs.put("muted", "muted"); // 浏览器要求自动播放必须静音
        }

        Boolean loop = node.getPropBool("loop");
        if (Boolean.TRUE.equals(loop)) {
            attrs.put("loop", "loop");
        }

        String poster = node.getPropString("poster");
        if (poster != null) attrs.put("poster", poster);

        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        String style = !inlineStyle.isEmpty() ? inlineStyle : "width:100%;";
        attrs.put("style", style);

        out.openTag("video", attrs).closeTag("video");
    }
}
