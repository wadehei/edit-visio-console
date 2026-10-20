package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 地图组件渲染器 — 渲染 <iframe> 嵌入地图。
 */
@Component("map")
public class MapComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String url = node.getPropString("url");
        String lat = node.getPropString("latitude");
        String lng = node.getPropString("longitude");
        Integer zoom = node.getPropInt("zoom");
        if (zoom == null) zoom = 15;

        // 如果没有提供 URL，尝试用高德地图生成
        if (url == null || url.isEmpty()) {
            if (lat != null && lng != null) {
                url = "https://uri.amap.com/marker?position=" + lng + "," + lat + "&name=位置&coordinate=gaode&callnative=0";
            } else {
                out.openTag("div", HtmlBuilder.attrs("style",
                        "width:100%;height:200px;background:#f5f5f5;display:flex;align-items:center;justify-content:center;color:#999;"))
                   .text("暂无地图").closeTag("div");
                return;
            }
        }

        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("src", url);
        attrs.put("style", "width:100%;height:300px;border:none;");
        attrs.put("loading", "lazy");
        attrs.put("allowfullscreen", "true");

        // 合并 schema styles 中的高度
        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        if (!inlineStyle.isEmpty()) {
            attrs.put("style", inlineStyle);
        }

        out.openTag("iframe", attrs).closeTag("iframe");
    }
}
