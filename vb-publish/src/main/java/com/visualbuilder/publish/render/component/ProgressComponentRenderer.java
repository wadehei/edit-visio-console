package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

/**
 * 进度条组件渲染器 — 纯 CSS 实现。
 */
@Component("progress")
public class ProgressComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        Integer percent = node.getPropInt("percent");
        if (percent == null) percent = 0;
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;

        String color = node.getPropString("color");
        if (color == null) color = "#1677ff";
        Boolean showLabel = node.getPropBool("showLabel");
        String label = node.getPropString("label");

        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        String containerStyle = !inlineStyle.isEmpty() ? inlineStyle : "width:100%;padding:8px 0;";

        out.openTag("div", HtmlBuilder.attrs("style", containerStyle));

        // 进度条轨道
        out.openTag("div", HtmlBuilder.attrs("class", "vb-progress-bar", "style",
                "width:100%;background:#e9ecef;border-radius:4px;overflow:hidden;height:8px;"));
        // 进度条填充
        out.openTag("div", HtmlBuilder.attrs("class", "vb-progress-fill", "style",
                "width:" + percent + "%;background:" + color + ";height:100%;border-radius:4px;transition:width .3s;"));
        out.closeTag("div"); // fill
        out.closeTag("div"); // bar

        // 标签
        if (Boolean.TRUE.equals(showLabel) || label != null) {
            String text = label != null ? label : percent + "%";
            out.openTag("div", HtmlBuilder.attrs("style",
                    "font-size:12px;color:#666;margin-top:4px;text-align:right;"))
               .text(text).closeTag("div");
        }

        out.closeTag("div"); // 容器
    }
}
