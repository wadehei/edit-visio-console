-- ============================================================
-- 索引优化补丁 — 补充高频查询列的索引
-- 在 init.sql 基础上新增，不影响已有数据
-- ============================================================

USE visual_builder;

-- ─── 用户域 ──────────────────────────────────────────────────
CREATE INDEX idx_user_tenant    ON sys_user (tenant_id);
CREATE INDEX idx_user_dept      ON sys_user (dept_id);
CREATE INDEX idx_user_status    ON sys_user (status);

-- ─── 角色域 ──────────────────────────────────────────────────
CREATE INDEX idx_role_tenant    ON sys_role (tenant_id);
CREATE INDEX idx_role_status    ON sys_role (status);

-- ─── 权限域 ──────────────────────────────────────────────────
CREATE UNIQUE INDEX uk_permission_key ON sys_permission (permission_key, deleted);

-- ─── 部门域 ──────────────────────────────────────────────────
CREATE INDEX idx_dept_tenant_parent ON sys_dept (tenant_id, parent_id);

-- ─── 项目域 ──────────────────────────────────────────────────
CREATE INDEX idx_project_status      ON biz_project (status);
CREATE INDEX idx_project_created_time ON biz_project (created_time);

-- ─── 页面域 ──────────────────────────────────────────────────
CREATE INDEX idx_page_path   ON biz_page (page_path);
CREATE INDEX idx_page_status ON biz_page (status);

-- ─── 组件域 ──────────────────────────────────────────────────
CREATE INDEX idx_component_type   ON biz_component (type);
CREATE INDEX idx_component_status ON biz_component (status);

-- ─── 模板域 ──────────────────────────────────────────────────
CREATE INDEX idx_template_status ON biz_template (status);
CREATE INDEX idx_template_category_sort ON biz_template_category (sort_order);

-- ─── 资源域 ──────────────────────────────────────────────────
-- 注意: idx_resource_filename 已移除（listResources 不按 file_name 查询）

-- ─── 日志域 ──────────────────────────────────────────────────
-- 注意: idx_log_user 已移除（getLogs 仅按 module 过滤+created_time 排序）
CREATE INDEX idx_loginlog_user    ON sys_login_log (user_id);
CREATE INDEX idx_loginlog_status  ON sys_login_log (status);

-- ─── 高频联合查询索引（补充） ────────────────────────────────
CREATE INDEX idx_page_project_status ON biz_page (project_id, status, deleted);
CREATE INDEX idx_project_user_deleted ON biz_project (user_id, deleted);
CREATE INDEX idx_template_status_usecount ON biz_template (status, use_count);
CREATE INDEX idx_component_category_status ON biz_component (category_id, status);
CREATE INDEX idx_resource_user_type_deleted ON res_file (user_id, resource_type, deleted);
