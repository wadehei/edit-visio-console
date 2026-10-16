# Visual Builder Backend（可视化装修平台后台）

基于 Spring Boot 3.2 + MyBatis-Plus 的多模块微服务后端，为可视化页面编辑器提供项目管理、页面 Schema 存储、组件管理、模板市场、资源上传、发布等能力。

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.4 |
| 缓存 | Redis + Redisson | 3.27.2 |
| 认证 | JWT (JJWT) | 0.12.5 |
| 对象存储 | MinIO / 本地 | 8.5.10 |
| 文档 | Knife4j (OpenAPI 3) | 4.4.0 |
| 工具 | Hutool | 5.8.27 |
| Java | JDK | 17 |

## 模块结构

```
edit-visio-console/
├── vb-common/       # 公共基础：BaseEntity、异常、JWT、存储、配置
├── vb-user/         # 用户模块：注册/登录/角色/权限
├── vb-project/      # 项目管理：CRUD/成员协作
├── vb-page/         # 页面管理：Schema 存储/版本/回滚
├── vb-component/    # 组件管理：16 个内置组件 + 自定义
├── vb-template/     # 模板市场：分类/应用/收藏
├── vb-resource/     # 资源管理：文件上传/类型校验
├── vb-publish/      # 发布管理：版本发布/历史
├── vb-system/       # 系统管理：配置/日志/健康检查
├── vb-gateway/      # 应用入口：Spring Security 配置
├── sql/             # 数据库脚本
└── docs/            # 设计文档
```

## 快速启动

### 前置条件
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis

### 1. 初始化数据库
```bash
mysql -u root -p < sql/init.sql
mysql -u root -p < sql/index-optimization.sql   # 可选，索引优化
```

### 2. 配置环境变量
```bash
cp .env.example .env
# 编辑 .env 填入实际值
```

### 3. 编译运行
```bash
mvn clean install -DskipTests
cd vb-gateway
mvn spring-boot:run
```

服务启动后访问：
- API: http://localhost:8080
- Swagger: http://localhost:8080/doc.html

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员 |

## 环境变量

参考 `.env.example`，关键变量：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| JWT_SECRET | JWT 签名密钥（生产必填） | 开发默认值 |
| DB_PASSWORD | MySQL 密码 | root123456 |
| MINIO_ACCESS_KEY | MinIO 访问密钥 | minioadmin |
| MINIO_SECRET_KEY | MinIO 密钥 | minioadmin |
| RABBITMQ_PASSWORD | RabbitMQ 密码 | guest |

## 核心 API

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证 | `/api/v1/auth/*` | 注册/登录/刷新 Token |
| 用户 | `/api/v1/users/*` | 个人信息/密码修改 |
| 项目 | `/api/v1/projects/*` | CRUD/成员/复制 |
| 页面 | `/api/v1/projects/{pid}/pages/*` | Schema/版本/回滚 |
| 组件 | `/api/v1/components/*` | 16 个内置组件 |
| 模板 | `/api/v1/templates/*` | 模板市场 |
| 资源 | `/api/v1/resources/*` | 文件上传/管理 |
| 发布 | `/api/v1/projects/{pid}/pages/{id}/publish` | 页面发布 |
| 系统 | `/api/v1/system/*` | 配置/日志/健康检查 |

## 错误码体系

| 范围 | 含义 |
|------|------|
| 1xxxx | 通用/参数校验 |
| 2xxxx | 用户/认证/权限 |
| 3xxxx | 项目管理 |
| 4xxxx | 页面/组件/模板/资源 |
| 5xxxx | 系统/内部错误 |

详见 `ErrorCode.java`。

## 开发约定

- 统一响应包装：`R<T>` (code/message/data/timestamp)
- 分页响应：`PageResult<T>`
- 审计字段自动填充：`AutoFillHandler` (created_by/time, updated_by/time)
- 逻辑删除：`@TableLogic deleted`
- 全局异常处理：`GlobalExceptionHandler`
- 权限注解：`@RequiresPermission` / `@RequiresRole`（AOP 切面）

## License

Private — Visual Builder Team
