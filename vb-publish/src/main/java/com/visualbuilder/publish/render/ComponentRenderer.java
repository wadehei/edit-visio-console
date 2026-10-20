package com.visualbuilder.publish.render;

/**
 * 组件渲染器接口 — 每个组件类型实现一个。
 *
 * <p>将 SchemaNode 渲染为 HTML 片段，追加到 HtmlBuilder 中。
 * Spring 通过 bean name 自动注册到 {@link ComponentRendererRegistry}。</p>
 */
public interface ComponentRenderer {

    /**
     * 渲染组件节点
     *
     * @param node   当前组件节点
     * @param out    HTML 输出构建器
     * @param ctx    渲染上下文（携带递归深度、子节点渲染能力）
     */
    void render(SchemaNode node, HtmlBuilder out, RenderContext ctx);

    /**
     * 渲染上下文 — 提供子节点递归渲染能力和全局配置。
     */
    record RenderContext(int depth, int maxDepth, PageRenderer pageRenderer) {

        /** 渲染子节点列表 */
        public void renderChildren(java.util.List<SchemaNode> children, HtmlBuilder out) {
            if (depth >= maxDepth) {
                out.append("<!-- max depth exceeded -->");
                return;
            }
            RenderContext childCtx = new RenderContext(depth + 1, maxDepth, pageRenderer);
            for (SchemaNode child : children) {
                pageRenderer.renderNode(child, out, childCtx);
            }
        }
    }
}
