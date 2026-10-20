package com.visualbuilder.publish.render;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量 HTML 构建器 — 封装 StringBuilder，提供链式 API。
 * 零外部依赖，专注 HTML 片段拼接。
 */
public final class HtmlBuilder {

    private final StringBuilder sb;

    public HtmlBuilder() {
        this.sb = new StringBuilder(4096);
    }

    public HtmlBuilder(int capacity) {
        this.sb = new StringBuilder(capacity);
    }

    /** 开始标签，如 {@code <div class="x">} */
    public HtmlBuilder openTag(String tag) {
        sb.append('<').append(tag).append('>');
        return this;
    }

    /** 开始标签 + 属性，如 {@code <img src="..." alt="...">} */
    public HtmlBuilder openTag(String tag, Map<String, String> attrs) {
        sb.append('<').append(tag);
        appendAttrs(attrs);
        sb.append('>');
        return this;
    }

    /** 自闭合标签，如 {@code <img ... />} */
    public HtmlBuilder selfClosingTag(String tag, Map<String, String> attrs) {
        sb.append('<').append(tag);
        appendAttrs(attrs);
        sb.append(" />");
        return this;
    }

    /** 结束标签，如 {@code </div>} */
    public HtmlBuilder closeTag(String tag) {
        sb.append("</").append(tag).append('>');
        return this;
    }

    /** 带属性的开始标签（手动传入后关闭） */
    public HtmlBuilder openTagStart(String tag) {
        sb.append('<').append(tag);
        return this;
    }

    /** 添加单个属性 */
    public HtmlBuilder attr(String name, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(' ').append(name).append("=\"").append(escapeAttr(value)).append('"');
        }
        return this;
    }

    /** 关闭开始标签 */
    public HtmlBuilder openTagEnd() {
        sb.append('>');
        return this;
    }

    /** 追加转义后的文本内容 */
    public HtmlBuilder text(String content) {
        if (content != null) {
            sb.append(escapeHtml(content));
        }
        return this;
    }

    /** 追加原始 HTML（不转义，用于已渲染的子节点） */
    public HtmlBuilder rawHtml(String html) {
        if (html != null) {
            sb.append(html);
        }
        return this;
    }

    /** 追加 CSS 样式块 */
    public HtmlBuilder style(String css) {
        sb.append("<style>").append(css).append("</style>");
        return this;
    }

    /** 追加 JS 脚本块 */
    public HtmlBuilder script(String js) {
        sb.append("<script>").append(js).append("</script>");
        return this;
    }

    /** 追加换行 */
    public HtmlBuilder newLine() {
        sb.append('\n');
        return this;
    }

    /** 追加原始字符串 */
    public HtmlBuilder append(String str) {
        if (str != null) {
            sb.append(str);
        }
        return this;
    }

    public int length() {
        return sb.length();
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    // ─── 工具方法 ───────────────────────────────────────────────

    private void appendAttrs(Map<String, String> attrs) {
        if (attrs == null) return;
        for (Map.Entry<String, String> e : attrs.entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                sb.append(' ').append(e.getKey()).append("=\"")
                  .append(escapeAttr(e.getValue())).append('"');
            }
        }
    }

    /** HTML 文本转义（防 XSS） */
    public static String escapeHtml(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&'  -> out.append("&amp;");
                case '<'  -> out.append("&lt;");
                case '>'  -> out.append("&gt;");
                case '"'  -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default   -> out.append(c);
            }
        }
        return out.toString();
    }

    /** HTML 属性值转义 */
    public static String escapeAttr(String value) {
        return escapeHtml(value);
    }

    /**
     * 将 camelCase 样式键转为 kebab-case。
     * 例：backgroundColor → background-color
     */
    public static String camelToKebab(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return camelCase;
        StringBuilder kebab = new StringBuilder(camelCase.length() + 4);
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) kebab.append('-');
                kebab.append(Character.toLowerCase(c));
            } else {
                kebab.append(c);
            }
        }
        return kebab.toString();
    }

    /**
     * 将 styles Map 转为 CSS 内联字符串。
     * 例：{"backgroundColor": "#fff", "fontSize": 16} → "background-color:#fff;font-size:16px;"
     */
    public static String stylesToInlineCss(Map<String, Object> styles) {
        if (styles == null || styles.isEmpty()) return "";
        StringBuilder css = new StringBuilder(styles.size() * 20);
        for (Map.Entry<String, Object> e : styles.entrySet()) {
            String prop = camelToKebab(e.getKey());
            Object val = e.getValue();
            if (val != null) {
                String value = val.toString();
                // 数字类型自动追加 px（排除无单位属性）
                if (val instanceof Number && !isUnitless(prop)) {
                    value = value + "px";
                }
                css.append(prop).append(':').append(value).append(';');
            }
        }
        return css.toString();
    }

    /** 无需 px 单位的 CSS 属性 */
    private static boolean isUnitless(String prop) {
        return prop.equals("opacity") || prop.equals("z-index") || prop.equals("z-index")
                || prop.equals("line-height") || prop.equals("flex") || prop.equals("flex-grow")
                || prop.equals("flex-shrink") || prop.equals("order") || prop.equals("font-weight");
    }

    /** 创建不可变属性 Map 的便捷方法 */
    public static Map<String, String> attrs(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must be even length");
        }
        Map<String, String> map = new LinkedHashMap<>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]),
                    keyValues[i + 1] != null ? String.valueOf(keyValues[i + 1]) : null);
        }
        return map;
    }
}
