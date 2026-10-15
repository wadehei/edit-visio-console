-- ============================================================
-- 可视化装修平台 - 数据库初始化脚本
-- 匹配 Java 实体定义 (BaseEntity: id, created_by, created_time, updated_by, updated_time, deleted)
-- ============================================================

CREATE DATABASE IF NOT EXISTS visual_builder DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE visual_builder;

SET NAMES utf8mb4;

-- --------------------------------------------------------------------------
-- 1. 用户权限相关
-- --------------------------------------------------------------------------

DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_role_dept;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dept;

-- 1.1 用户表
CREATE TABLE sys_user (
  id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
  tenant_id       BIGINT          NOT NULL DEFAULT 0       COMMENT '租户ID',
  username        VARCHAR(64)     NOT NULL                 COMMENT '用户名',
  password        VARCHAR(200)    NOT NULL                 COMMENT '密码(BCrypt)',
  nickname        VARCHAR(64)     NOT NULL DEFAULT ''      COMMENT '昵称',
  email           VARCHAR(128)    NOT NULL DEFAULT ''      COMMENT '邮箱',
  phone           VARCHAR(20)     NOT NULL DEFAULT ''      COMMENT '手机号',
  avatar          VARCHAR(500)    NOT NULL DEFAULT ''      COMMENT '头像URL',
  sex             TINYINT         NOT NULL DEFAULT 0       COMMENT '性别 0未知 1男 2女',
  dept_id         BIGINT          DEFAULT NULL             COMMENT '部门ID',
  status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态 0禁用 1正常 2锁定',
  login_ip        VARCHAR(128)    NOT NULL DEFAULT ''      COMMENT '最后登录IP',
  login_date      DATETIME        DEFAULT NULL             COMMENT '最后登录时间',
  pwd_update_date DATETIME        DEFAULT NULL             COMMENT '密码修改时间',
  remark          VARCHAR(500)    NOT NULL DEFAULT ''      COMMENT '备注',
  created_by      BIGINT          DEFAULT NULL             COMMENT '创建人',
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_by      BIGINT          DEFAULT NULL             COMMENT '更新人',
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- 1.2 角色表
CREATE TABLE sys_role (
  id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '角色ID',
  tenant_id       BIGINT          NOT NULL DEFAULT 0       COMMENT '租户ID',
  role_name       VARCHAR(64)     NOT NULL                 COMMENT '角色名称',
  role_key        VARCHAR(64)     NOT NULL                 COMMENT '角色标识',
  sort_order      INT             NOT NULL DEFAULT 0       COMMENT '排序',
  data_scope      TINYINT         NOT NULL DEFAULT 1       COMMENT '数据权限 1自己 2本部门 3本部门及下级 4全部 5自定义',
  status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态 0禁用 1正常',
  is_system       TINYINT         NOT NULL DEFAULT 0       COMMENT '系统内置 0否 1是',
  remark          VARCHAR(500)    NOT NULL DEFAULT '',
  created_by      BIGINT          DEFAULT NULL,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by      BIGINT          DEFAULT NULL,
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_key (role_key, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- 1.3 权限表
CREATE TABLE sys_permission (
  id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '权限ID',
  parent_id       BIGINT          NOT NULL DEFAULT 0       COMMENT '父权限ID',
  permission_name VARCHAR(100)    NOT NULL                 COMMENT '权限名称',
  permission_key  VARCHAR(200)    NOT NULL                 COMMENT '权限标识',
  permission_type TINYINT         NOT NULL                 COMMENT '类型 1目录 2菜单 3按钮 4接口',
  path            VARCHAR(200)    NOT NULL DEFAULT ''      COMMENT '路由路径',
  component       VARCHAR(200)    NOT NULL DEFAULT ''      COMMENT '前端组件路径',
  icon            VARCHAR(100)    NOT NULL DEFAULT '',
  sort_order      INT             NOT NULL DEFAULT 0,
  visible         TINYINT         NOT NULL DEFAULT 1       COMMENT '0隐藏 1显示',
  status          TINYINT         NOT NULL DEFAULT 1,
  api_method      VARCHAR(10)     NOT NULL DEFAULT '',
  api_path        VARCHAR(200)    NOT NULL DEFAULT '',
  remark          VARCHAR(500)    NOT NULL DEFAULT '',
  created_by      BIGINT          DEFAULT NULL,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by      BIGINT          DEFAULT NULL,
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='权限表';

-- 1.4 用户角色关联表
CREATE TABLE sys_user_role (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  user_id         BIGINT          NOT NULL,
  role_id         BIGINT          NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- 1.5 角色权限关联表
CREATE TABLE sys_role_permission (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  role_id         BIGINT          NOT NULL,
  permission_id   BIGINT          NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色权限关联表';

-- 1.6 部门表
CREATE TABLE sys_dept (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  tenant_id       BIGINT          NOT NULL DEFAULT 0,
  parent_id       BIGINT          NOT NULL DEFAULT 0,
  ancestors       VARCHAR(500)    NOT NULL DEFAULT '',
  dept_name       VARCHAR(100)    NOT NULL,
  sort_order      INT             NOT NULL DEFAULT 0,
  leader          VARCHAR(64)     NOT NULL DEFAULT '',
  phone           VARCHAR(20)     NOT NULL DEFAULT '',
  status          TINYINT         NOT NULL DEFAULT 1,
  created_by      BIGINT          DEFAULT NULL,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by      BIGINT          DEFAULT NULL,
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表';

-- --------------------------------------------------------------------------
-- 2. 业务核心表
-- --------------------------------------------------------------------------

DROP TABLE IF EXISTS biz_project_member;
DROP TABLE IF EXISTS biz_publish_record;
DROP TABLE IF EXISTS biz_page_version;
DROP TABLE IF EXISTS biz_page;
DROP TABLE IF EXISTS biz_project;
DROP TABLE IF EXISTS biz_component;
DROP TABLE IF EXISTS biz_component_category;
DROP TABLE IF EXISTS biz_template;
DROP TABLE IF EXISTS biz_template_category;

-- 2.1 项目表
CREATE TABLE biz_project (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  name            VARCHAR(100)    NOT NULL                 COMMENT '项目名称',
  description     VARCHAR(500)    NOT NULL DEFAULT '',
  cover_url       VARCHAR(500)    NOT NULL DEFAULT '',
  user_id         BIGINT          NOT NULL                 COMMENT '创建者ID',
  status          TINYINT         NOT NULL DEFAULT 0       COMMENT '0草稿 1已发布 2已归档',
  settings        JSON            DEFAULT NULL             COMMENT '项目设置JSON',
  published_at    DATETIME        DEFAULT NULL,
  created_by      BIGINT          DEFAULT NULL,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by      BIGINT          DEFAULT NULL,
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目表';

-- 2.2 页面表
CREATE TABLE biz_page (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  project_id      BIGINT          NOT NULL                 COMMENT '所属项目ID',
  name            VARCHAR(100)    NOT NULL                 COMMENT '页面名称',
  page_path       VARCHAR(200)    NOT NULL                 COMMENT 'URL路径',
  page_type       VARCHAR(20)     NOT NULL DEFAULT 'h5'    COMMENT 'h5/pc/mini-program',
  schema_content  JSON            DEFAULT NULL             COMMENT 'PageSchema JSON',
  schema_version  INT             NOT NULL DEFAULT 0       COMMENT '版本号',
  cover_url       VARCHAR(500)    NOT NULL DEFAULT '',
  status          TINYINT         NOT NULL DEFAULT 0       COMMENT '0编辑中 1已发布',
  is_home         TINYINT         NOT NULL DEFAULT 0,
  user_id         BIGINT          NOT NULL                 COMMENT '创建者',
  created_by      BIGINT          DEFAULT NULL,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by      BIGINT          DEFAULT NULL,
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_project_id (project_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='页面表';

-- 2.3 页面版本表
CREATE TABLE biz_page_version (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  page_id         BIGINT          NOT NULL,
  version_no      INT             NOT NULL,
  schema_snapshot JSON            NOT NULL                 COMMENT 'Schema快照',
  changelog       VARCHAR(500)    NOT NULL DEFAULT '',
  user_id         BIGINT          NOT NULL,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_page_version (page_id, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='页面版本表';

-- 2.4 组件分类表
CREATE TABLE biz_component_category (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  name            VARCHAR(50)     NOT NULL,
  parent_id       BIGINT          NOT NULL DEFAULT 0,
  icon            VARCHAR(100)    NOT NULL DEFAULT '',
  sort_order      INT             NOT NULL DEFAULT 0,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组件分类表';

-- 2.5 组件表
CREATE TABLE biz_component (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  category_id     BIGINT          NOT NULL,
  type            VARCHAR(30)     NOT NULL                 COMMENT 'text/button/image...',
  name            VARCHAR(50)     NOT NULL,
  description     VARCHAR(200)    NOT NULL DEFAULT '',
  thumbnail       VARCHAR(500)    NOT NULL DEFAULT '',
  schema_define   JSON            DEFAULT NULL,
  default_props   JSON            DEFAULT NULL,
  default_styles  JSON            DEFAULT NULL,
  is_system       TINYINT         NOT NULL DEFAULT 1       COMMENT '1系统内置 0自定义',
  user_id         BIGINT          DEFAULT NULL,
  sort_order      INT             NOT NULL DEFAULT 0,
  status          TINYINT         NOT NULL DEFAULT 1,
  created_by      BIGINT          DEFAULT NULL,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by      BIGINT          DEFAULT NULL,
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组件表';

-- 2.6 模板分类表
CREATE TABLE biz_template_category (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  name            VARCHAR(50)     NOT NULL,
  icon            VARCHAR(100)    NOT NULL DEFAULT '',
  sort_order      INT             NOT NULL DEFAULT 0,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模板分类表';

-- 2.7 模板表
CREATE TABLE biz_template (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  category_id     BIGINT          NOT NULL,
  name            VARCHAR(100)    NOT NULL,
  description     VARCHAR(500)    NOT NULL DEFAULT '',
  cover_url       VARCHAR(500)    NOT NULL DEFAULT '',
  page_schema     JSON            NOT NULL                 COMMENT 'Schema快照',
  tags            VARCHAR(200)    NOT NULL DEFAULT '',
  use_count       INT             NOT NULL DEFAULT 0,
  favorite_count  INT             NOT NULL DEFAULT 0,
  is_official     TINYINT         NOT NULL DEFAULT 0,
  user_id         BIGINT          NOT NULL,
  status          TINYINT         NOT NULL DEFAULT 0       COMMENT '0待审核 1已上架 2已下架',
  created_by      BIGINT          DEFAULT NULL,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by      BIGINT          DEFAULT NULL,
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模板表';

-- 2.8 项目成员表
CREATE TABLE biz_project_member (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  project_id      BIGINT          NOT NULL,
  user_id         BIGINT          NOT NULL,
  permission      VARCHAR(20)     NOT NULL DEFAULT 'view',
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_user (project_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目成员表';

-- 2.9 发布记录表
CREATE TABLE biz_publish_record (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  page_id         BIGINT          NOT NULL,
  version_no      INT             NOT NULL,
  status          TINYINT         NOT NULL DEFAULT 0       COMMENT '0处理中 1成功 2失败',
  access_url      VARCHAR(500)    NOT NULL DEFAULT '',
  changelog       VARCHAR(500)    NOT NULL DEFAULT '',
  published_by    BIGINT          NOT NULL,
  published_at    DATETIME        DEFAULT NULL,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_page_id (page_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布记录表';

-- --------------------------------------------------------------------------
-- 3. 资源管理表
-- --------------------------------------------------------------------------

DROP TABLE IF EXISTS res_file;

CREATE TABLE res_file (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  user_id         BIGINT          NOT NULL,
  resource_type   VARCHAR(20)     NOT NULL                 COMMENT 'image/video/audio/file',
  file_name       VARCHAR(200)    NOT NULL,
  storage_path    VARCHAR(500)    NOT NULL,
  url             VARCHAR(500)    NOT NULL,
  file_size       BIGINT          NOT NULL DEFAULT 0,
  mime_type       VARCHAR(100)    NOT NULL DEFAULT '',
  width           INT             DEFAULT NULL,
  height          INT             DEFAULT NULL,
  md5_hash        VARCHAR(32)     DEFAULT NULL,
  group_name      VARCHAR(50)     NOT NULL DEFAULT '',
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_type (user_id, resource_type),
  KEY idx_md5 (md5_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件资源表';

-- --------------------------------------------------------------------------
-- 4. 系统管理表
-- --------------------------------------------------------------------------

DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_log;
DROP TABLE IF EXISTS sys_login_log;

CREATE TABLE sys_config (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  config_key      VARCHAR(100)    NOT NULL,
  config_value    VARCHAR(500)    NOT NULL DEFAULT '',
  description     VARCHAR(200)    NOT NULL DEFAULT '',
  updated_by      BIGINT          DEFAULT NULL,
  updated_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统配置表';

CREATE TABLE sys_log (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  user_id         BIGINT          DEFAULT NULL,
  username        VARCHAR(64)     NOT NULL DEFAULT '',
  module          VARCHAR(100)    NOT NULL DEFAULT '',
  operation       VARCHAR(200)    NOT NULL DEFAULT '',
  method          VARCHAR(200)    NOT NULL DEFAULT '',
  request_url     VARCHAR(500)    NOT NULL DEFAULT '',
  request_params  TEXT            DEFAULT NULL,
  status          TINYINT         NOT NULL DEFAULT 1,
  error_msg       TEXT            DEFAULT NULL,
  ip_address      VARCHAR(128)    NOT NULL DEFAULT '',
  cost_time       BIGINT          NOT NULL DEFAULT 0,
  created_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

CREATE TABLE sys_login_log (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  user_id         BIGINT          DEFAULT NULL,
  username        VARCHAR(64)     NOT NULL,
  login_type      TINYINT         NOT NULL DEFAULT 1       COMMENT '1登录 2登出',
  status          TINYINT         NOT NULL DEFAULT 1,
  ip_address      VARCHAR(128)    NOT NULL DEFAULT '',
  user_agent      VARCHAR(500)    NOT NULL DEFAULT '',
  message         VARCHAR(200)    NOT NULL DEFAULT '',
  login_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- --------------------------------------------------------------------------
-- 5. 初始数据
-- --------------------------------------------------------------------------

-- 超级管理员 (密码: admin123)
INSERT INTO sys_user (id, tenant_id, username, password, nickname, status) VALUES
(1, 0, 'admin', '$2a$10$VQEDHFpFbGrgHFzaOqzHwe8KzU1RFkHEc1KjKb5kLqGcm3aIV0a2S', 'Super Admin', 1);

-- 默认角色
INSERT INTO sys_role (id, role_name, role_key, sort_order, data_scope, is_system) VALUES
(1, 'Super Admin', 'SUPER_ADMIN', 1, 4, 1),
(2, 'User',        'USER',        2, 1, 1),
(3, 'Guest',       'GUEST',       3, 1, 1);

-- 管理员角色绑定
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 组件分类
INSERT INTO biz_component_category (id, name, parent_id, sort_order) VALUES
(1, 'Basic',    0, 1),
(2, 'Media',    0, 2),
(3, 'Business', 0, 3);

-- 16个内置组件
INSERT INTO biz_component (id, category_id, type, name, is_system, sort_order) VALUES
(1,  1, 'text',      'Text',      1, 1),
(2,  1, 'button',    'Button',    1, 2),
(3,  1, 'image',     'Image',     1, 3),
(4,  1, 'container', 'Container', 1, 4),
(5,  1, 'divider',   'Divider',   1, 5),
(6,  1, 'spacer',    'Spacer',    1, 6),
(7,  3, 'card',      'Card',      1, 7),
(8,  1, 'badge',     'Badge',     1, 8),
(9,  1, 'richText',  'RichText',  1, 9),
(10, 1, 'input',     'Input',     1, 10),
(11, 2, 'video',     'Video',     1, 11),
(12, 2, 'audio',     'Audio',     1, 12),
(13, 2, 'map',       'Map',       1, 13),
(14, 2, 'carousel',  'Carousel',  1, 14),
(15, 3, 'countdown', 'Countdown', 1, 15),
(16, 3, 'progress',  'Progress',  1, 16);

-- 默认配置
INSERT INTO sys_config (config_key, config_value, description) VALUES
('site.name',           'Visual Builder',  'Site Name'),
('upload.maxSizeMB',    '50',              'Max Upload Size (MB)'),
('page.maxVersionCount','50',              'Max Page Versions');

-- --------------------------------------------------------------------------
-- Verify
-- --------------------------------------------------------------------------
SELECT CONCAT('Tables created: ', COUNT(*)) AS result
FROM information_schema.tables
WHERE table_schema = 'visual_builder';
