# 可视化装修平台 — 后台系统完整设计方案

> 版本：v1.0 | 日期：2026-10-10 | 配套前端：edit-visio

---

## 目录

1. [技术栈选型](#一技术栈选型)
2. [系统架构](#二系统架构)
3. [Maven 模块设计](#三maven-模块设计)
4. [数据库设计](#四数据库设计)
5. [API 接口设计](#五api-接口设计)
6. [RBAC 权限设计](#六rbac-权限设计)
7. [安全方案](#七安全方案)
8. [部署方案](#八部署方案)
9. [开发计划](#九开发计划)

---

## 一、技术栈选型

| 层次 | 技术选型 | 版本 | 选型理由 |
|------|---------|------|---------|
| **框架** | Spring Boot | 3.2+ | 2024年主流，原生支持Java 17+，生态成熟 |
| **数据库** | MySQL | 8.0+ | JSON字段类型支持PageSchema存储，团队熟悉度高 |
| **ORM** | MyBatis-Plus | 3.5+ | 灵活SQL控制，代码生成器强大，适合复杂查询场景 |
| **缓存** | Redis | 7.x | Session管理、热点数据缓存、分布式锁 |
| **文件存储** | MinIO | 最新稳定版 | 自建对象存储，S3兼容API，可迁移至云OSS |
| **认证** | JWT + Spring Security | 6.x | 无状态认证，前后端分离友好，RBAC权限模型 |
| **文档** | Knife4j (Swagger增强) | 4.x | 自动生成API文档，UI更友好 |
| **消息队列** | RabbitMQ | 3.12+ | 发布异步任务、代码导出任务解耦 |
| **日志** | SLF4J + Logback | - | 结构化日志，便于问题排查 |

### 关键决策说明

**为什么选 MyBatis-Plus 而非 JPA？**
- PageSchema 是动态JSON结构，需要灵活的SQL操作
- 批量更新组件排序等场景，MyBatis 的批量操作更直观

**为什么选 MySQL 而非 PostgreSQL？**
- MySQL 8.0 的 JSON 类型 + 虚拟列可满足 PageSchema 部分查询需求
- 如未来需要更强的 JSON 操作能力（如 JSONB），可平滑迁移至 PostgreSQL

---

## 二、系统架构

### 2.1 整体架构图

```
┌──────────────────────────────────────────────────────────┐
│                    用户浏览器                              │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│                      Nginx                                │
│              (80/443 反向代理 + 静态资源)                  │
└──────────┬─────────────────────────────────┬─────────────┘
           │ /                               │ /api/*
    ┌──────▼──────┐                ┌─────────▼────────┐
    │ 前端静态资源  │                │  Spring Boot     │
    │ Vue dist    │                │    Backend       │
    └─────────────┘                │    (8080)        │
                                   └────────┬─────────┘
                         ┌──────────┬───────┼────────┬──────────┐
                   ┌─────▼───┐ ┌───▼───┐ ┌─▼───┐ ┌──▼───┐ ┌───▼────┐
                   │  MySQL  │ │ Redis │ │MinIO│ │ MQ   │ │  ES    │
                   │ (3306)  │ │(6379) │ │     │ │(5672)│ │(可选)   │
                   └─────────┘ └───────┘ │(9000)│ └──────┘ └────────┘
                                          └──────┘
```

### 2.2 模块职责

```
visual-builder-backend/
├── vb-common/              # 公共模块
├── vb-system/              # 系统管理模块
├── vb-user/                # 用户模块
├── vb-project/             # 项目管理模块
├── vb-page/                # 页面管理模块
├── vb-component/           # 组件管理模块
├── vb-template/            # 模板管理模块
├── vb-resource/            # 资源管理模块
├── vb-publish/             # 发布管理模块
└── vb-gateway/             # API网关/启动模块
```

| 模块 | 职责 |
|------|------|
| **vb-common** | 统一响应体 R\<T\>、全局异常处理、JWT工具类、文件存储抽象层、MyBatis配置 |
| **vb-user** | 注册/登录/个人信息/修改密码 |
| **vb-project** | 项目CRUD、复制、分享、成员协作 |
| **vb-page** | 页面Schema存储/读取/自动保存/版本管理（**核心模块**） |
| **vb-component** | 组件分类、16个内置组件管理、自定义组件 |
| **vb-template** | 模板CRUD、模板市场、应用模板到页面 |
| **vb-resource** | 图片/视频/音频上传、MinIO存储、资源管理 |
| **vb-publish** | 页面发布、代码导出（MQ异步任务）、域名绑定 |
| **vb-system** | 系统配置、操作日志、数据字典 |

---

## 三、Maven 模块设计

### 3.1 父 POM

```xml
<project>
    <groupId>com.visualbuilder</groupId>
    <artifactId>visual-builder-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>vb-common</module>
        <module>vb-system</module>
        <module>vb-user</module>
        <module>vb-project</module>
        <module>vb-page</module>
        <module>vb-component</module>
        <module>vb-template</module>
        <module>vb-resource</module>
        <module>vb-publish</module>
        <module>vb-gateway</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.5</spring-boot.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <knife4j.version>4.4.0</knife4j.version>
        <minio.version>8.5.10</minio.version>
        <jwt.version>0.12.5</jwt.version>
    </properties>
</project>
```

### 3.2 模块依赖关系

```
vb-gateway (启动入口)
  ├── vb-user         → vb-common
  ├── vb-project      → vb-common, vb-user
  ├── vb-page         → vb-common, vb-project
  ├── vb-component    → vb-common
  ├── vb-template     → vb-common, vb-page
  ├── vb-resource     → vb-common, vb-user
  ├── vb-publish      → vb-common, vb-page, vb-resource
  ├── vb-system       → vb-common
  └── vb-common (所有业务模块依赖)
```

### 3.3 业务模块内部结构（以 vb-page 为例）

```
vb-page/
├── src/main/java/com/visualbuilder/page/
│   ├── controller/
│   │   └── PageController.java
│   ├── service/
│   │   ├── PageService.java
│   │   └── impl/PageServiceImpl.java
│   ├── mapper/
│   │   ├── PageMapper.java
│   │   └── PageVersionMapper.java
│   ├── entity/
│   │   ├── Page.java
│   │   └── PageVersion.java
│   ├── dto/
│   │   ├── PageCreateDTO.java
│   │   ├── PageSchemaDTO.java
│   │   └── PageVO.java
│   ├── converter/
│   │   └── PageConverter.java
│   └── config/
│       └── PageModuleConfig.java
└── src/main/resources/
    └── mapper/
        └── PageMapper.xml
```

### 3.4 核心公共设计

**统一响应体**

```java
@Data
public class R<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> R<T> ok(T data) { ... }
    public static <T> R<T> fail(int code, String message) { ... }
}
```

**文件存储抽象层**

```java
public interface StorageService {
    String upload(InputStream stream, String path, String contentType);
    void delete(String path);
    String getPresignedUrl(String path, int expireMinutes);
}

// MinIO实现
public class MinioStorageService implements StorageService { ... }
// 阿里云OSS实现（可选）
public class OssStorageService implements StorageService { ... }
```

---

## 四、数据库设计

### 4.1 数据库总览

共 **19 张表**，分为 4 大模块：

```
visual_decorator/
├── 一、用户相关（5表）
│   ├── sys_user                  # 用户表
│   ├── sys_role                  # 角色表
│   ├── sys_user_role             # 用户角色关联
│   ├── sys_permission            # 权限表（树形结构）
│   └── sys_role_permission       # 角色权限关联
├── 二、业务核心（7表）
│   ├── biz_project               # 项目表
│   ├── biz_page                  # 页面表（存PageSchema JSON）
│   ├── biz_page_version          # 页面版本表（快照）
│   ├── biz_component             # 组件表（16种内置+自定义）
│   ├── biz_component_category    # 组件分类表
│   ├── biz_template              # 模板表
│   └── biz_template_category     # 模板分类表
├── 三、资源管理（2表）
│   ├── res_file                  # 文件资源表
│   └── res_image                 # 图片资源表
├── 四、系统管理（3表）
│   ├── sys_config                # 系统配置表
│   ├── sys_log                   # 操作日志表
│   └── sys_login_log             # 登录日志表
└── 补充（2表）
    ├── biz_project_member        # 项目成员表（多人协作）
    └── biz_publish_record        # 发布记录表
```

### 4.2 核心表 ER 关系

```
sys_user ──1:N──> biz_project ──1:N──> biz_page ──1:N──> biz_page_version
   │                   │                    │
   │                   ├──1:N──> biz_project_member
   │                   │
   │                   └──1:N──> biz_publish_record
   │
   ├──M:N──> sys_role (via sys_user_role)
   │              │
   │              └──M:N──> sys_permission (via sys_role_permission)
   │
   └──1:N──> res_file ──1:1──> res_image

biz_component_category ──1:N──> biz_component
biz_template_category  ──1:N──> biz_template
```

### 4.3 用户相关表

#### sys_user（用户表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NO | 0 | 租户ID（多租户预留） |
| username | VARCHAR(64) | NO | - | 用户名（唯一） |
| password | VARCHAR(200) | NO | - | 密码（BCrypt加密） |
| nickname | VARCHAR(64) | YES | '' | 昵称 |
| email | VARCHAR(128) | YES | '' | 邮箱 |
| phone | VARCHAR(20) | YES | '' | 手机号 |
| avatar | VARCHAR(500) | YES | '' | 头像URL |
| sex | TINYINT | YES | 0 | 性别 0未知 1男 2女 |
| dept_id | BIGINT | YES | NULL | 部门ID |
| status | TINYINT | NO | 1 | 状态 0禁用 1正常 2锁定 |
| login_ip | VARCHAR(128) | YES | '' | 最后登录IP |
| login_date | DATETIME | YES | NULL | 最后登录时间 |
| pwd_update_date | DATETIME | YES | NULL | 密码修改时间 |
| remark | VARCHAR(500) | YES | '' | 备注 |
| created_by | BIGINT | YES | NULL | 创建人 |
| created_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | YES | NULL | 更新人 |
| updated_time | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT | NO | 0 | 逻辑删除 0正常 1删除 |

**索引**：`uk_username(username, tenant_id, deleted)` 联合唯一、`idx_dept_id(dept_id)`

#### sys_role（角色表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NO | 0 | 租户ID |
| role_name | VARCHAR(64) | NO | - | 角色名称 |
| role_key | VARCHAR(64) | NO | - | 角色标识（如 SUPER_ADMIN） |
| sort_order | INT | NO | 0 | 排序 |
| data_scope | TINYINT | NO | 1 | 数据权限 1自己 2本部门 3本部门及下级 4全部 5自定义 |
| status | TINYINT | NO | 1 | 状态 0禁用 1正常 |
| is_system | TINYINT | NO | 0 | 系统内置角色不可删除 |
| remark | VARCHAR(500) | YES | '' | 备注 |
| created_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| updated_time | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT | NO | 0 | 逻辑删除 |

#### sys_permission（权限表 — 树形结构）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| parent_id | BIGINT | NO | 0 | 父权限ID（树结构） |
| permission_name | VARCHAR(100) | NO | - | 权限名称 |
| permission_key | VARCHAR(200) | NO | - | 权限标识（如 system:user:create） |
| permission_type | TINYINT | NO | - | 类型 1目录 2菜单 3按钮 4接口 |
| path | VARCHAR(200) | YES | '' | 路由路径 |
| component | VARCHAR(200) | YES | '' | 前端组件路径 |
| icon | VARCHAR(100) | YES | '' | 图标 |
| sort_order | INT | NO | 0 | 排序 |
| visible | TINYINT | NO | 1 | 是否可见 0隐藏 1显示 |
| status | TINYINT | NO | 1 | 状态 0禁用 1正常 |
| api_method | VARCHAR(10) | YES | '' | 接口方法 GET/POST/PUT/DELETE |
| api_path | VARCHAR(200) | YES | '' | 接口路径 |
| created_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| updated_time | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT | NO | 0 | 逻辑删除 |

#### sys_user_role / sys_role_permission（关联表）

```sql
-- 用户角色关联
sys_user_role (id, user_id, role_id) -- UNIQUE(user_id, role_id)

-- 角色权限关联
sys_role_permission (id, role_id, permission_id) -- UNIQUE(role_id, permission_id)
```

### 4.4 业务核心表

#### biz_project（项目表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) | NO | - | 项目名称 |
| description | VARCHAR(500) | YES | '' | 项目描述 |
| cover_url | VARCHAR(500) | YES | '' | 封面图URL |
| user_id | BIGINT UNSIGNED | NO | - | 创建者ID (FK → sys_user.id) |
| status | TINYINT | NO | 0 | 0-草稿 1-已发布 2-已归档 |
| settings | JSON | YES | NULL | 项目设置（主题色、全局CSS等） |
| published_at | DATETIME | YES | NULL | 发布时间 |
| created_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| updated_time | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT | NO | 0 | 逻辑删除 |

**索引**：`idx_user_id(user_id)`

#### biz_page（页面表 — ★核心表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| project_id | BIGINT UNSIGNED | NO | - | 所属项目ID (FK → biz_project.id) |
| name | VARCHAR(100) | NO | - | 页面名称 |
| page_path | VARCHAR(200) | NO | - | URL路径 |
| page_type | VARCHAR(20) | NO | 'h5' | 页面类型 h5/pc/mini-program |
| schema_content | **JSON** | YES | NULL | **完整PageSchema，MySQL JSON类型** |
| schema_version | INT | NO | 0 | Schema版本号，每次保存+1 |
| cover_url | VARCHAR(500) | YES | '' | 页面封面 |
| status | TINYINT | NO | 0 | 0-编辑中 1-已发布 |
| is_home | TINYINT | NO | 0 | 是否首页 |
| user_id | BIGINT UNSIGNED | NO | - | 创建者ID |
| created_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| updated_time | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT | NO | 0 | 逻辑删除 |

**索引**：`uk_project_path(project_id, page_path, deleted)` 联合唯一、`idx_user_id(user_id)`

**PageSchema JSON 结构示例（存储在 schema_content 字段）：**

```json
{
  "version": "1.0",
  "meta": { "title": "首页", "description": "活动首页" },
  "props": { "backgroundColor": "#ffffff", "width": 375 },
  "children": [
    {
      "id": "comp_001",
      "type": "container",
      "props": { "direction": "column", "gap": "16px" },
      "styles": { "width": "100%", "padding": "16px" },
      "children": [
        {
          "id": "comp_002",
          "type": "text",
          "props": { "content": "欢迎", "fontSize": 24, "color": "#333" },
          "styles": { "textAlign": "center" }
        },
        {
          "id": "comp_003",
          "type": "image",
          "props": { "src": "https://cdn.example.com/banner.jpg" },
          "styles": { "width": "100%", "borderRadius": "8px" }
        }
      ]
    }
  ]
}
```

#### biz_page_version（页面版本表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| page_id | BIGINT UNSIGNED | NO | - | 页面ID (FK → biz_page.id) |
| version_no | INT | NO | - | 版本号 |
| schema_snapshot | JSON | NO | - | 该版本的Schema快照 |
| changelog | VARCHAR(500) | YES | '' | 版本说明 |
| user_id | BIGINT UNSIGNED | NO | - | 操作人 |
| created_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`uk_page_version(page_id, version_no)` 联合唯一

**自动保存策略：**
```
前端：每30秒 or Schema变更时触发
  ↓
后端：接收 → 更新 biz_page.schema_content → schema_version+1
  ↓
异步线程：每5个版本 或 间隔超过30分钟 → 快照写入 biz_page_version
  ↓
版本上限：每个页面最多保留50个版本，超出删除最早的
```

#### biz_component（组件表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| category_id | BIGINT UNSIGNED | NO | - | 分类ID (FK) |
| type | VARCHAR(30) | NO | - | 组件类型 text/button/image/container... |
| name | VARCHAR(50) | NO | - | 显示名称 |
| description | VARCHAR(200) | YES | '' | 组件描述 |
| thumbnail | VARCHAR(500) | YES | '' | 缩略图 |
| schema_define | JSON | YES | NULL | 组件属性定义（JSON Schema） |
| default_props | JSON | YES | NULL | 默认属性值 |
| default_styles | JSON | YES | NULL | 默认样式 |
| is_system | TINYINT | NO | 1 | 1-系统内置 0-用户上传 |
| user_id | BIGINT UNSIGNED | YES | NULL | 上传者ID |
| sort_order | INT | NO | 0 | 排序 |
| status | TINYINT | NO | 1 | 0-禁用 1-启用 |
| created_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| updated_time | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT | NO | 0 | 逻辑删除 |

**16个系统内置组件：** `is_system=1`

| 分类 | 组件 |
|------|------|
| 基础组件 | text, button, image, container, divider, spacer |
| 媒体组件 | video, audio, carousel, map |
| 业务组件 | card, badge, richText, input, countdown, progress |

#### biz_component_category（组件分类表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| name | VARCHAR(50) | NO | - | 分类名称 |
| parent_id | BIGINT | NO | 0 | 父分类ID，0为顶级 |
| icon | VARCHAR(100) | YES | '' | 图标 |
| sort_order | INT | NO | 0 | 排序 |

#### biz_template（模板表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| category_id | BIGINT UNSIGNED | NO | - | 分类ID |
| name | VARCHAR(100) | NO | - | 模板名称 |
| description | VARCHAR(500) | YES | '' | 模板描述 |
| cover_url | VARCHAR(500) | YES | '' | 封面图 |
| page_schema | JSON | NO | - | 完整Schema快照 |
| tags | VARCHAR(200) | YES | '' | 标签（逗号分隔） |
| use_count | INT | NO | 0 | 使用次数 |
| favorite_count | INT | NO | 0 | 收藏次数 |
| is_official | TINYINT | NO | 0 | 官方模板 |
| user_id | BIGINT UNSIGNED | NO | - | 上传者 |
| status | TINYINT | NO | 0 | 0-待审核 1-已上架 2-已下架 |
| created_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| updated_time | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT | NO | 0 | 逻辑删除 |

### 4.5 资源管理表

#### res_file（文件资源表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| user_id | BIGINT UNSIGNED | NO | - | 上传者ID |
| resource_type | VARCHAR(20) | NO | - | image/video/audio/file |
| file_name | VARCHAR(200) | NO | - | 原始文件名 |
| storage_path | VARCHAR(500) | NO | - | MinIO存储路径 |
| url | VARCHAR(500) | NO | - | 访问URL |
| file_size | BIGINT | NO | 0 | 文件大小(bytes) |
| mime_type | VARCHAR(100) | NO | - | image/png, video/mp4 |
| width | INT | YES | NULL | 图片/视频宽度 |
| height | INT | YES | NULL | 图片/视频高度 |
| md5_hash | VARCHAR(32) | YES | NULL | 文件MD5（去重） |
| group_name | VARCHAR(50) | YES | '' | 分组名 |
| created_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| deleted | TINYINT | NO | 0 | 逻辑删除 |

**索引**：`idx_md5(md5_hash)` 去重检测、`idx_user_type(user_id, resource_type)`

**MinIO 存储策略：**
```
Bucket: visual-builder
路径规则: {resourceType}/{userId}/{yyyyMMdd}/{uuid}.{ext}
示例: image/10001/20261010/a3f5c8e2.png

访问策略:
- 图片资源：公开读
- 私有文件：预签名URL，有效期30分钟
```

### 4.6 系统管理表

#### sys_config（系统配置表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| config_key | VARCHAR(100) | 配置键（唯一） |
| config_value | VARCHAR(500) | 配置值 |
| description | VARCHAR(200) | 说明 |
| updated_by | BIGINT | 更新人 |
| updated_time | DATETIME | 更新时间 |

#### sys_log（操作日志表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 操作者ID |
| username | VARCHAR(64) | 操作者用户名 |
| module | VARCHAR(100) | 操作模块 |
| operation | VARCHAR(200) | 操作描述 |
| method | VARCHAR(200) | 请求方法 |
| request_url | VARCHAR(500) | 请求URL |
| request_params | TEXT | 请求参数 |
| status | TINYINT | 0失败 1成功 |
| error_msg | TEXT | 错误信息 |
| ip_address | VARCHAR(128) | IP |
| cost_time | BIGINT | 耗时(ms) |
| created_time | DATETIME | 创建时间 |

#### sys_login_log（登录日志表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| username | VARCHAR(64) | 用户名 |
| login_type | TINYINT | 1登录 2登出 |
| status | TINYINT | 0失败 1成功 |
| ip_address | VARCHAR(128) | IP |
| user_agent | VARCHAR(500) | 浏览器UA |
| message | VARCHAR(200) | 提示消息 |
| login_time | DATETIME | 登录时间 |

### 4.7 补充表

#### biz_project_member（项目成员表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| project_id | BIGINT | 项目ID |
| user_id | BIGINT | 用户ID |
| permission | VARCHAR(20) | view/edit/admin |
| created_time | DATETIME | 加入时间 |

#### biz_publish_record（发布记录表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| page_id | BIGINT | 页面ID |
| version_no | INT | 发布版本号 |
| status | TINYINT | 0处理中 1成功 2失败 |
| access_url | VARCHAR(500) | 访问URL |
| changelog | VARCHAR(500) | 发布说明 |
| published_by | BIGINT | 发布人 |
| published_at | DATETIME | 发布时间 |
| created_time | DATETIME | 创建时间 |

> 📎 **完整建表 SQL 文件**：`D:\me\learn\AI\visual_decorator_db.sql`（833行，含示例数据）

---

## 五、API 接口设计

### 5.1 API 规范

| 项目 | 说明 |
|------|------|
| Base URL | `/api/v1` |
| 协议 | HTTPS |
| 数据格式 | `application/json` |
| 认证方式 | JWT Bearer Token（`Authorization: Bearer <token>`） |
| 时间格式 | ISO 8601（`2026-10-10T12:00:00Z`） |
| 分页参数 | `page`（页码，默认1）、`pageSize`（每页条数，默认20，最大100） |

**统一响应格式：**

```json
{
  "code": 0,
  "message": "success",
  "data": { },
  "timestamp": "2026-10-10T12:00:00Z",
  "traceId": "a1b2c3d4-e5f6-7890"
}
```

**错误码规范：**

| 错误码 | 含义 | HTTP Status |
|--------|------|-------------|
| 0 | 成功 | 200 |
| 40001 | 用户名或密码错误 | 401 |
| 40002 | Token 过期 | 401 |
| 40003 | Token 无效 | 401 |
| 40004 | 权限不足 | 403 |
| 40005 | 资源不存在 | 404 |
| 41001 | 参数校验失败 | 400 |
| 41002 | 文件格式不支持 | 400 |
| 41003 | 文件大小超限 | 400 |
| 50001 | 服务器内部错误 | 500 |

### 5.2 接口总览（58个接口）

| # | 方法 | 路径 | 说明 | 权限 |
|---|------|------|------|------|
| **认证** | | | | |
| 1 | POST | `/auth/register` | 用户注册 | 公开 |
| 2 | POST | `/auth/login` | 用户登录 | 公开 |
| 3 | POST | `/auth/logout` | 登出 | 已认证 |
| 4 | POST | `/auth/refresh` | 刷新Token | 已认证 |
| 5 | GET | `/auth/captcha` | 获取验证码 | 公开 |
| **用户** | | | | |
| 6 | GET | `/users/me` | 获取个人信息 | 已认证 |
| 7 | PUT | `/users/me` | 更新个人信息 | 已认证 |
| 8 | PUT | `/users/me/password` | 修改密码 | 已认证 |
| 9 | POST | `/users/me/avatar` | 上传头像 | 已认证 |
| **项目** | | | | |
| 10 | GET | `/projects` | 项目列表 | editor+ |
| 11 | GET | `/projects/:id` | 项目详情 | 成员 |
| 12 | POST | `/projects` | 创建项目 | editor+ |
| 13 | PUT | `/projects/:id` | 更新项目 | 创建者 |
| 14 | DELETE | `/projects/:id` | 删除项目 | 创建者 |
| 15 | POST | `/projects/:id/copy` | 复制项目 | 已认证 |
| 16 | POST | `/projects/:id/share` | 分享项目 | 创建者 |
| 17 | DELETE | `/projects/:id/share/:sid` | 取消分享 | 创建者 |
| **页面** | | | | |
| 18 | GET | `/projects/:pid/pages` | 页面列表 | 成员 |
| 19 | GET | `/projects/:pid/pages/:id` | 页面详情(含Schema) | 成员 |
| 20 | POST | `/projects/:pid/pages` | 创建页面 | editor+ |
| 21 | PUT | `/projects/:pid/pages/:id` | 更新页面信息 | editor+ |
| 22 | **PUT** | **`/projects/:pid/pages/:id/schema`** | **★保存Schema** | editor+ |
| 23 | DELETE | `/projects/:pid/pages/:id` | 删除页面 | 创建者 |
| 24 | GET | `/projects/:pid/pages/:id/versions` | 版本列表 | 成员 |
| 25 | POST | `/projects/:pid/pages/:id/versions/:v/rollback` | 版本回滚 | editor+ |
| **组件** | | | | |
| 26 | GET | `/components/categories` | 组件分类 | 已认证 |
| 27 | GET | `/components` | 组件列表 | 已认证 |
| 28 | GET | `/components/:id` | 组件详情 | 已认证 |
| 29 | POST | `/components` | 创建自定义组件 | admin |
| 30 | PUT | `/components/:id` | 更新组件 | admin |
| 31 | DELETE | `/components/:id` | 删除组件 | admin |
| **模板** | | | | |
| 32 | GET | `/templates/categories` | 模板分类 | 已认证 |
| 33 | GET | `/templates` | 模板列表 | 已认证 |
| 34 | GET | `/templates/:id` | 模板详情 | 已认证 |
| 35 | **POST** | **`/templates/:id/apply`** | **★应用模板** | editor+ |
| 36 | POST | `/templates` | 发布模板 | admin |
| 37 | DELETE | `/templates/:id` | 删除模板 | admin |
| **资源** | | | | |
| 38 | GET | `/resources` | 资源列表 | 已认证 |
| 39 | **POST** | **`/resources/upload`** | **★文件上传** | editor+ |
| 40 | POST | `/resources/upload/batch` | 批量上传 | editor+ |
| 41 | POST | `/resources/upload/signature` | 获取上传凭证 | editor+ |
| 42 | DELETE | `/resources/:id` | 删除资源 | 上传者/admin |
| 43 | DELETE | `/resources/batch` | 批量删除 | admin |
| 44-47 | - | `/resources/folders/*` | 文件夹管理 | editor+ |
| **发布** | | | | |
| 48 | **POST** | **`/projects/:pid/pages/:id/publish`** | **★发布页面** | editor+ |
| 49 | POST | `/projects/:pid/publish` | 发布项目 | editor+ |
| 50 | GET | `/projects/:pid/publish-history` | 发布历史 | 成员 |
| 51 | POST | `/projects/:pid/domain` | 绑定域名 | 创建者/admin |
| 52 | DELETE | `/projects/:pid/domain/:did` | 解绑域名 | 创建者/admin |
| 53 | GET | `/projects/:pid/statistics` | 访问统计 | 成员 |
| **系统** | | | | |
| 54-56 | - | `/system/configs/*` | 配置管理 | admin |
| 57 | GET | `/system/logs` | 操作日志 | admin |
| 58 | GET | `/system/health` | 系统状态 | admin |

### 5.3 核心接口详细设计

#### ★ 保存页面 Schema（接口 #22）

```
PUT /api/v1/projects/:projectId/pages/:pageId/schema
Authorization: Bearer <token>
```

**请求 Body：**
```json
{
  "schema": {
    "version": "1.0",
    "props": {
      "title": "首页",
      "backgroundColor": "#ffffff",
      "backgroundImage": ""
    },
    "children": [
      {
        "id": "comp_001",
        "type": "text",
        "props": { "content": "欢迎", "fontSize": 24, "color": "#333" },
        "styles": { "textAlign": "center", "padding": "16px" }
      },
      {
        "id": "comp_002",
        "type": "image",
        "props": { "src": "https://cdn.example.com/banner.jpg", "alt": "Banner" },
        "styles": { "width": "100%", "height": "200px" }
      }
    ]
  },
  "isAutoSave": false
}
```

**响应 data：**
```json
{
  "pageId": "pg_001",
  "version": 13,
  "savedAt": "2026-10-10T12:00:00Z",
  "schemaSize": 48920,
  "componentCount": 35
}
```

#### ★ 文件上传（接口 #39）

```
POST /api/v1/resources/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

**请求：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 文件，单文件≤50MB |
| folderId | string | 否 | 目标文件夹ID |

**支持格式：**
| 类型 | 格式 |
|------|------|
| image | jpg, jpeg, png, gif, webp, svg |
| video | mp4, webm |
| audio | mp3, wav, ogg |

**响应 data：**
```json
{
  "resourceId": "r_002",
  "name": "product-photo.png",
  "type": "image",
  "mimeType": "image/png",
  "size": 512340,
  "url": "https://cdn.example.com/resources/product-photo.png",
  "thumbnailUrl": "https://cdn.example.com/resources/thumb/product-photo.png",
  "dimensions": { "width": 800, "height": 800 }
}
```

#### ★ 发布页面（接口 #48）

```
POST /api/v1/projects/:projectId/pages/:pageId/publish
Authorization: Bearer <token>
```

**发布流程：**
```
用户点击"发布"
    │
    ▼
biz_publish_record 记录 status=PROCESSING
    │
    ▼ RabbitMQ 发送发布任务
    │
    ▼ PublishConsumer 消费任务
    │
    ├─ 1. 从 biz_page 读取最新 Schema
    ├─ 2. Schema 解析 → 生成组件树
    ├─ 3. 渲染为 HTML/CSS/JS 静态文件
    ├─ 4. 上传静态文件至 MinIO
    ├─ 5. 更新 biz_publish_record status=SUCCESS
    │
    ▼ 前端轮询获取结果
```

### 5.4 PageSchema 数据结构规范

```typescript
interface PageSchema {
  version: string;                    // Schema协议版本，如 "1.0"
  meta?: PageMeta;                    // 页面元信息
  props: PageProps;                   // 页面全局属性
  children: ComponentNode[];          // 组件树
}

interface PageMeta {
  title: string;
  description?: string;
  keywords?: string[];
}

interface PageProps {
  title: string;
  backgroundColor: string;
  backgroundImage?: string;
  padding?: string;
}

interface ComponentNode {
  id: string;                         // 组件实例唯一ID
  type: string;                       // 组件类型（text/button/image/container...）
  props: Record<string, any>;         // 组件属性
  styles: Record<string, any>;        // 组件样式（CSS）
  events?: EventConfig[];             // 事件配置
  children?: ComponentNode[];         // 子组件（递归）
}

interface EventConfig {
  type: string;                       // 事件类型（click/tap...）
  action: {
    type: 'link' | 'script' | 'emit';
    payload: Record<string, any>;
  };
}
```

---

## 六、RBAC 权限设计

### 6.1 权限模型

```
┌─────────┐    N:N    ┌─────────┐    N:N    ┌────────────┐
│  User   │──────────▶│  Role   │──────────▶│ Permission │
│  用户表  │           │  角色表  │           │  权限表     │
└─────────┘           └─────────┘           └────────────┘
```

**权限类型：**

| 类型 | 标识 | 说明 | 示例 |
|------|------|------|------|
| 目录 | `1` | 一级菜单 | 系统管理 |
| 菜单 | `2` | 二级菜单 | 用户管理 |
| 按钮 | `3` | 页面操作 | 用户新增/删除 |
| 接口 | `4` | API级别 | POST /api/v1/users |

### 6.2 内置角色

| 角色 | 标识 | 数据权限 | 说明 |
|------|------|---------|------|
| 超级管理员 | SUPER_ADMIN | ALL | 全部权限，不可删除 |
| 普通用户 | USER | SELF | 管理自己的项目和页面 |
| 访客 | GUEST | SELF | 只读权限 |

**角色继承关系：**
```
SUPER_ADMIN > USER > GUEST
```

### 6.3 数据权限

| 级别 | 标识 | 说明 |
|------|------|------|
| 仅自己 | SELF | 只能查看自己创建的数据 |
| 本部门 | DEPARTMENT | 可查看本部门所有数据 |
| 全部 | ALL | 可查看全部数据 |
| 自定义 | CUSTOM | 按自定义部门范围 |

数据权限通过 **MyBatis 拦截器** 自动注入过滤条件，业务代码无侵入。

### 6.4 认证方案（双 Token 机制）

| Token | 有效期 | 存储 | 用途 |
|-------|--------|------|------|
| Access Token | 30 分钟 | 内存（Pinia） | 接口鉴权 |
| Refresh Token | 7 天 | HttpOnly Cookie | 刷新 Access Token |

**JWT Payload：**
```json
{
  "sub": "user_001",
  "username": "zhangsan",
  "roles": ["USER"],
  "permissions": ["project:list", "project:create", "page:view"],
  "dataScope": "SELF",
  "iat": 1697000000,
  "exp": 1697001800,
  "jti": "uuid-token-id"
}
```

**登出机制：** Access Token 加入 Redis 黑名单，TTL = token 剩余有效期

### 6.5 权限校验注解

```java
// 接口权限
@RequiresPermission("system:user:create")
@PostMapping
public void create(@RequestBody CreateUserDTO dto) { ... }

// 多权限 OR
@RequiresPermission(value = {"system:user:update", "system:user:audit"}, logical = Logical.OR)
@PutMapping("/{id}")
public void update(@PathVariable Long id, @RequestBody UpdateUserDTO dto) { ... }

// 数据权限
@DataScope(userAlias = "u", deptAlias = "d")
@GetMapping
public PageResult<UserVO> list(UserQueryDTO query) { ... }
```

### 6.6 前端路由守卫配合

```typescript
router.beforeEach(async (to, from, next) => {
  // 1. 无 Token → 跳转登录
  // 2. 有 Token 但无用户信息 → 获取权限
  // 3. 动态生成路由（根据菜单权限）
  // 4. 按钮级权限校验
  if (to.meta.permission && !authStore.hasPermission(to.meta.permission)) {
    return next('/403')
  }
})

// 按钮级权限指令
// <el-button v-permission="'system:user:create'">新增</el-button>
```

---

## 七、安全方案

### 7.1 密码加密

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10); // cost factor=10
}
```

### 7.2 SQL 注入防护

- 使用 MyBatis `#{}` 参数化查询（禁止 `${}`）
- ORDER BY 等使用白名单校验

### 7.3 XSS 防护

```java
// 全局 XSS 过滤器 + 响应头
response.setHeader("X-Content-Type-Options", "nosniff");
response.setHeader("X-XSS-Protection", "1; mode=block");
response.setHeader("Content-Security-Policy", "default-src 'self'");
```

### 7.4 CSRF 防护

JWT + Bearer Token 方案天然免疫 CSRF。Refresh Token 的 Cookie 设置 `SameSite=Strict`。

### 7.5 接口限流

基于 Redis + Lua 滑动窗口：

| 接口 | 策略 | 说明 |
|------|------|------|
| 登录 | 5次/分钟/IP | 防暴力破解 |
| 注册 | 3次/小时/IP | 防批量注册 |
| 普通接口 | 100次/分钟/用户 | 全局保护 |

```java
@RateLimit(key = "login", period = 60, count = 5, limitType = LimitType.IP)
@PostMapping("/auth/login")
public LoginVO login(@RequestBody LoginDTO dto) { ... }
```

### 7.6 敏感数据脱敏

```java
@Sensitivity(type = SensitivityType.PHONE)   // 138****1234
private String phone;

@Sensitivity(type = SensitivityType.EMAIL)   // z***@example.com
private String email;
```

### 7.7 安全配置清单

| 配置项 | 值 | 说明 |
|--------|-----|------|
| JWT 签名算法 | HMAC-SHA256 | 对称加密 |
| Access Token 有效期 | 30 分钟 | 平衡安全与体验 |
| Refresh Token 有效期 | 7 天 | 减少登录频率 |
| BCrypt cost factor | 10 | 防暴力破解 |
| 登录失败锁定 | 5次后锁定30分钟 | 防暴力破解 |
| 密码最小长度 | 8 位 | 基础要求 |

---

## 八、部署方案

### 8.1 Docker Compose

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: visual_builder
    volumes:
      - mysql-data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports: ["3306:3306"]

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes: [redis-data:/data]
    ports: ["6379:6379"]

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY}
    volumes: [minio-data:/data]
    ports: ["9000:9000", "9001:9001"]

  rabbitmq:
    image: rabbitmq:3-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    ports: ["5672:5672", "15672:15672"]

  backend:
    build: ./vb-gateway
    environment:
      SPRING_PROFILES_ACTIVE: prod
      MYSQL_HOST: mysql
      REDIS_HOST: redis
      MINIO_ENDPOINT: http://minio:9000
    depends_on: [mysql, redis, minio, rabbitmq]
    ports: ["8080:8080"]

  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./frontend/dist:/usr/share/nginx/html
    ports: ["80:80", "443:443"]
    depends_on: [backend]
```

### 8.2 Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        client_max_body_size 50m;
    }

    # MinIO 资源代理
    location /resources/ {
        proxy_pass http://minio:9000/visual-builder/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 九、开发计划

| 阶段 | 周期 | 内容 | 优先级 |
|------|------|------|--------|
| **P0 基础框架** | 第1-2周 | 项目骨架、用户认证、数据库初始化、MinIO集成 | 🔴 最高 |
| **P1 核心功能** | 第3-5周 | 项目管理、页面Schema存储/读取、自动保存 | 🔴 最高 |
| **P2 组件体系** | 第6-7周 | 组件管理、组件分类、16个内置组件Schema定义 | 🟡 高 |
| **P3 模板系统** | 第8-9周 | 模板CRUD、模板市场、从模板创建页面 | 🟡 高 |
| **P4 发布导出** | 第10-11周 | 页面发布、代码导出（MQ异步任务） | 🟢 中 |
| **P5 系统完善** | 第12周 | 日志、监控、权限完善、部署脚本 | 🟢 中 |

---

## 附录

### A. 关键配置文件

```yaml
# application.yml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/visual_builder
    username: root
    password: ${MYSQL_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
      password: ${REDIS_PASSWORD}

  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672

  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket: visual-builder

jwt:
  secret: ${JWT_SECRET}
  access-token-expire: 1800      # 30分钟
  refresh-token-expire: 604800   # 7天

business:
  page:
    max-version-count: 50
    autosave-interval: 30000
  resource:
    max-image-size: 10485760     # 10MB
    max-video-size: 104857600    # 100MB
```

### B. 相关文档

| 文档 | 路径 |
|------|------|
| 完整建表 SQL | `D:\me\learn\AI\visual_decorator_db.sql` |
| 前端项目 | `D:\project\ai-project\edit-visio` |
| 后台项目 | `D:\project\ai-project\edit-visio-console` |
