package com.visualbuilder.publish.render.component;

import com.visualbuilder.publish.render.ComponentRenderer;
import com.visualbuilder.publish.render.HtmlBuilder;
import com.visualbuilder.publish.render.SchemaNode;
import org.springframework.stereotype.Component;

/**
 * 倒计时组件渲染器 — 渲染静态倒计时展示 + 内联 JS 实现动态计时。
 */
@Component("countdown")
public class CountdownComponentRenderer implements ComponentRenderer {

    @Override
    public void render(SchemaNode node, HtmlBuilder out, RenderContext ctx) {
        String targetDate = node.getPropString("targetDate");
        String title = node.getPropString("title");
        if (targetDate == null || targetDate.isEmpty()) {
            targetDate = "2099-12-31T23:59:59";
        }

        // 生成唯一 ID 用于 JS 绑定
        String compId = node.getId() != null ? node.getId() : "cd_" + System.nanoTime();
        String inlineStyle = HtmlBuilder.stylesToInlineCss(node.getStyles());
        String containerStyle = !inlineStyle.isEmpty() ? inlineStyle
                : "text-align:center;padding:16px;background:#f8f9fa;border-radius:8px;";

        out.openTag("div", HtmlBuilder.attrs("id", compId, "style", containerStyle));

        // 标题
        if (title != null && !title.isEmpty()) {
            out.openTag("div", HtmlBuilder.attrs("style",
                    "font-size:14px;color:#666;margin-bottom:8px;"))
               .text(title).closeTag("div");
        }

        // 倒计时数字区域
        out.openTag("div", HtmlBuilder.attrs("class", "cd-display", "style",
                "display:flex;justify-content:center;gap:8px;font-size:24px;font-weight:bold;color:#333;"));
        out.openTag("span", HtmlBuilder.attrs("class", "cd-days")).text("00").closeTag("span");
        out.append("<span style=\"color:#999;\">天</span>");
        out.openTag("span", HtmlBuilder.attrs("class", "cd-hours")).text("00").closeTag("span");
        out.append("<span style=\"color:#999;\">时</span>");
        out.openTag("span", HtmlBuilder.attrs("class", "cd-minutes")).text("00").closeTag("span");
        out.append("<span style=\"color:#999;\">分</span>");
        out.openTag("span", HtmlBuilder.attrs("class", "cd-seconds")).text("00").closeTag("span");
        out.append("<span style=\"color:#999;\">秒</span>");
        out.closeTag("div"); // cd-display

        out.closeTag("div"); // 容器

        // 内联 JS 倒计时
        out.script("""
                (function(){
                  var target=new Date('%s').getTime();
                  var el=document.getElementById('%s');
                  if(!el)return;
                  function pad(n){return n<10?'0'+n:''+n;}
                  function tick(){
                    var now=Date.now(),diff=target-now;
                    if(diff<=0){el.querySelector('.cd-display').innerHTML='<span style="color:#e74c3c;">已结束</span>';return;}
                    var d=Math.floor(diff/86400000),h=Math.floor(diff%%86400000/3600000),
                        m=Math.floor(diff%%3600000/60000),s=Math.floor(diff%%60000/1000);
                    el.querySelector('.cd-days').textContent=pad(d);
                    el.querySelector('.cd-hours').textContent=pad(h);
                    el.querySelector('.cd-minutes').textContent=pad(m);
                    el.querySelector('.cd-seconds').textContent=pad(s);
                    requestAnimationFrame(function(){setTimeout(tick,1000);});
                  }
                  tick();
                })();
                """.formatted(targetDate.replace("'", "\\'"), compId));
    }
}
