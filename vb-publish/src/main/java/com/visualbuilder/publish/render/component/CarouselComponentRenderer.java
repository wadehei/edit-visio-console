package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 轮播图组件渲染器 — CSS scroll-snap 实现，无需 JS 依赖。
 */
@Component("carousel")
public class CarouselComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        Integer height = node.getPropInt("height");
        if (height == null) height = 200;
        Boolean autoplay = node.getPropBool("autoplay");
        Integer interval = node.getPropInt("interval");
        if (interval == null) interval = 3000;

        // 获取轮播项（从 props.images 或 props.items）
        Object imagesObj = node.getProp("images");
        Object itemsObj = node.getProp("items");
        List<?> items = null;
        if (imagesObj instanceof List<?> list) items = list;
        else if (itemsObj instanceof List<?> list) items = list;

        if (items == null || items.isEmpty()) {
            out.openTag("div", HtmlBuilder.attrs("style",
                    "width:100%;height:" + height + "px;background:#f5f5f5;display:flex;align-items:center;justify-content:center;color:#999;"))
               .text("暂无轮播内容").closeTag("div");
            return;
        }

        // 容器：横向滚动 + scroll-snap
        out.openTag("div", HtmlBuilder.attrs("style",
                "display:flex;overflow-x:auto;scroll-snap-type:x mandatory;-webkit-overflow-scrolling:touch;"
                + "scrollbar-width:none;height:" + height + "px;"))
           .append("<style>div::-webkit-scrollbar{display:none}</style>");

        for (Object item : items) {
            String imgSrc = "";
            String link = "";
            if (item instanceof Map<?, ?> map) {
                Object src = map.get("src");
                Object img = map.get("image");
                Object url = map.get("url");
                Object linkVal = map.get("link");
                if (src != null) imgSrc = src.toString();
                else if (img != null) imgSrc = img.toString();
                if (url != null) link = url.toString();
                else if (linkVal != null) link = linkVal.toString();
            } else if (item instanceof String s) {
                imgSrc = s;
            }

            out.openTag("div", HtmlBuilder.attrs("style",
                    "flex:0 0 100%;width:100%;height:100%;scroll-snap-align:start;position:relative;"));

            if (!imgSrc.isEmpty()) {
                if (!link.isEmpty()) {
                    out.openTag("a", HtmlBuilder.attrs("href", link, "style", "display:block;width:100%;height:100%;"));
                }
                out.selfClosingTag("img", HtmlBuilder.attrs("src", imgSrc, "style",
                        "width:100%;height:100%;object-fit:cover;"));
                if (!link.isEmpty()) {
                    out.closeTag("a");
                }
            }

            out.closeTag("div"); // slide
        }

        out.closeTag("div"); // carousel container
    }
}
