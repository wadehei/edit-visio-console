-- ============================================================
-- 发布流程增强 — biz_publish_record 表结构变更
-- 在 init.sql 基础上新增 project_id 和 failed_reason 字段
-- ============================================================

USE visual_builder;

-- 1. 新增 project_id 字段（替代 LIKE 查询 access_url 的方式）
ALTER TABLE biz_publish_record
    ADD COLUMN project_id     BIGINT        NOT NULL DEFAULT 0 COMMENT '所属项目ID' AFTER page_id;

-- 2. 新增 failed_reason 字段（记录发布失败原因）
ALTER TABLE biz_publish_record
    ADD COLUMN failed_reason  VARCHAR(1000) DEFAULT NULL       COMMENT '失败原因' AFTER changelog;

-- 3. 新增 project_id 索引（替代原有 LIKE 查询）
ALTER TABLE biz_publish_record
    ADD INDEX idx_publish_project_id (project_id);

-- 4. 回填已有记录的 project_id（从 biz_page 表关联获取）
UPDATE biz_publish_record pr
    INNER JOIN biz_page p ON pr.page_id = p.id
    SET pr.project_id = p.id
    WHERE pr.project_id = 0;
