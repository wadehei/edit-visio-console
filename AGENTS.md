# AGENTS.md

可视化装修平台后台(Spring Boot 3.2 多模块 Maven)。给 AI 助手与新人贡献者的项目入口说明。前端仓库:`edit-visio-zcode`(Vue 3)。

## 项目速览

- 技术栈:Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + MySQL 8 + Redis/Redisson + RabbitMQ + JWT(JJWT 0.12.5) + MinIO
- 单体多模块:vb-gateway 是唯一启动入口(Spring Security 配置),其余模块为业务 jar
- 数据:MySQL `visual_builder` 库,全部表带 `deleted` 逻辑删除与 `tenant_id`/归属字段

## 必读文档

| 文档 | 内容 |
| --- | --- |
| [docs/backend-design.md](docs/backend-design.md) | 完整设计方案:架构、模块、库表、RBAC、安全 |
| [README.md](README.md) | 技术栈、模块结构、快速启动 |

## 开发约定

1. **依赖方向**:`vb-common`(BaseEntity/异常/JWT/工具)← 各业务模块 ← `vb-gateway`;业务模块之间禁止互相依赖,公共能力沉淀到 vb-common。
2. **写操作走 Service,历史/缓存变更必须在事务边界处理**:
   - 权限/角色变更后调用 `UserAuthCacheVersion.bump()`(事务提交后执行,用 `TransactionUtils.executeAfterCommit`,避免事务内失效被回滚吞掉);
   - 业务缓存 TTL 一律加随机抖动(防雪崩,参考 `ComponentServiceImpl.jitteredCacheTtl`);
   - 状态机推进下沉为数据库原子条件更新(如 `PublishServiceImpl.completePublish` 的 `status=PENDING` 条件),并发重复投递只有一个消费者成功。
3. **安全红线**:
   - 富文本等 HTML 内容必须经 Jsoup 白名单(`RichTextComponentRenderer`),禁止退回正则过滤(可被大小写/实体编码绕过);
   - MyBatis-Plus 查询禁止字符串拼接 SQL;按 id 查改必须校验资源归属(防水平越权);
   - 新增唯一约束用 `(业务键, deleted)` 组合键配合逻辑删除;`DuplicateKeyException` 统一 409(GlobalExceptionHandler)。
4. **Schema 变更**:改 `sql/init.sql` 并保持与实体注解一致;索引变更需同步 `vb-gateway` 的索引治理 IT(「回退即失败」的证据链测试)。

## 验证与提交

```bash
# 本机 JAVA_HOME 值带了引号会破坏 mvn,命令前缀正确值
JAVA_HOME='D:\software\jdk-17' mvn verify   # 编译 + 单测 + 覆盖率门禁(漂移阈值,低于即失败)
```

- 集成测试(IT)依赖 MySQL/Redis:本地无服务时按 `assumeTrue` 自动跳过;CI(.github/workflows/ci.yml)起真实 MySQL 8 + Redis 7 service 容器全量执行。
- 提交信息用中文一行式:`<类型>:<做了什么>`,类型 refactor/feat/fix/chore。
