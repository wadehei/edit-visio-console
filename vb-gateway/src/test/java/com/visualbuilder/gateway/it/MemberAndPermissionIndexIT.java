package com.visualbuilder.gateway.it;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.entity.SysRolePermission;
import com.visualbuilder.common.mapper.SysRolePermissionMapper;
import com.visualbuilder.project.entity.ProjectMember;
import com.visualbuilder.project.mapper.ProjectMemberMapper;
import com.visualbuilder.system.entity.SysPermission;
import com.visualbuilder.system.mapper.SysPermissionMapper;
import com.visualbuilder.system.service.impl.AdminPermissionServiceImpl;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 方案 A（P1 / P2）补齐缺失索引的证据链：「回退即失败」的集成测试。
 *
 * <h3>被测改动（全部落在 {@code sql/init.sql}）</h3>
 * <ul>
 *   <li><b>P1</b>：{@code biz_project_member} 新增 {@code KEY idx_member_user (user_id)}。
 *       该表原仅有 {@code PRIMARY(id)} 与 {@code UNIQUE KEY uk_project_user(project_id, user_id)}，
 *       没有任何以 {@code user_id} 为前导列的索引，故
 *       {@code ProjectMemberServiceImpl#listProjectIdsByUser} 的 {@code WHERE user_id = ?}
 *       只能全表扫描。该方法被 {@code listProjects} 每次调用。</li>
 *   <li><b>P2</b>：{@code sys_role_permission} 新增 {@code KEY idx_rp_permission (permission_id)}。
 *       该表原仅有 {@code PRIMARY(id)} 与 {@code UNIQUE KEY uk_role_permission(role_id, permission_id)}，
 *       {@code permission_id} 不是任何索引的前导列，故
 *       {@code AdminPermissionServiceImpl#deletePermission} 中
 *       {@code rolePermissionMapper.delete(... eq(permissionId))} 只能全表（全索引）扫描。</li>
 * </ul>
 * <p>两处新增索引均<b>不构成最左前缀冗余</b>：{@code idx_member_user} 不是
 * {@code uk_project_user(project_id, user_id)} 的前缀，{@code idx_rp_permission} 也不是
 * {@code uk_role_permission(role_id, permission_id)} 的前缀。</p>
 *
 * <h3>真实参与</h3>
 * <ul>
 *   <li>真实 MySQL 8.4.9 服务端（默认 {@code 127.0.0.1:3306/visual_builder}，本机以
 *       {@code -Dit.mysql.url=jdbc:mysql://127.0.0.1:3307/visual_builder} 覆盖）与真实 InnoDB 索引结构。</li>
 *   <li>真实 Mapper（{@link ProjectMemberMapper}、{@link SysRolePermissionMapper}、
 *       {@link SysPermissionMapper}）与真实 Service（{@link AdminPermissionServiceImpl}），
 *       配真实 MyBatis-Plus {@code SqlSessionFactory}，因此被测 SQL 与生产完全一致；
 *       {@link SqlCapture} 记录实际下发的 SQL 原文（含 {@code update}，故 DELETE 也被捕获）。</li>
 *   <li>证据取自 MySQL 自身会话级计数器（{@code performance_schema.session_status} 的
 *       {@code Handler_read_rnd_next}＝聚簇索引顺序读行数（全表扫描特征）、
 *       {@code Handler_read_next}＝索引项顺序读行数（全索引扫描特征）），而非对 SQL 文本的阅读
 *       或对执行计划的推断。被测查询与计数运行在<b>同一条物理连接</b>上
 *       （{@link SingleConnectionDataSource} 包裹单个 {@link Connection}），保证会话计数连续可比。</li>
 * </ul>
 *
 * <h3>替身参与</h3>
 * <p><b>无 Mockito 替身</b>。仅用 {@code ReflectionTestUtils} 把真实 Mapper 注入
 * {@code ServiceImpl.baseMapper}（与生产 Spring 注入等价），不替换任何行为。</p>
 *
 * <h3>环境差异</h3>
 * 本机 MySQL 为 {@code 8.4.9}，生产镜像为 {@code mysql:8.0}。InnoDB 页大小、B+ 树结构、
 * {@code Handler_read_*} 计数语义一致；但优化器代价模型与 index_merge 取舍策略存在版本差异，
 * 故断言采用「与播种行数成比例的宽松阈值」（远小于行数），不使用任何绝对常量。</p>
 *
 * <h3>不可证伪的部分（诚实声明）</h3>
 * <ul>
 *   <li>本测试用<b>自行播种</b>的数据（每表 20000 行、目标键各占 100 行）触发优化器选择索引；
 *       它证明的是「在具备该索引时这两条语句不再全表扫描」，
 *       <b>不证明</b>生产数据分布下的耗时下降幅度。</li>
 *   <li>{@code Handler_read_rnd_next} 小只代表该次语句未做聚簇索引顺序扫描，
 *       <b>不证明</b>该语句永不回退为全表扫描（数据分布极端时仍可能）。</li>
 *   <li>P2 的被测语句通过真实 Service 调用，故测量窗口内还含同一权限的
 *       {@code getById} 与 {@code removeById} 两条主键单行语句；它们在被测/变异两种形态下完全相同，
 *       不影响差值方向。</li>
 * </ul>
 *
 * <h3>变异实验结果（2026-11-05 本机实测，MySQL 8.4.9 @127.0.0.1:3307）</h3>
 * <p>把 3307 上的 {@code idx_member_user} / {@code idx_rp_permission} 删除（回退为治理前形态）后，
 * 本类 <b>3 个用例全部失败</b>（3/3）；具体计数器数值与失败断言原文见类末尾注释。</p>
 *
 * <p>环境不可达时全部用例判定为<b>跳过（skipped）而非通过</b>。</p>
 */
class MemberAndPermissionIndexIT {

    private static final String MYSQL_URL = System.getProperty("it.mysql.url",
            "jdbc:mysql://localhost:3306/visual_builder");
    private static final String MYSQL_USER = System.getProperty("it.mysql.user", "root");
    private static final String MYSQL_PASSWORD = System.getProperty("it.mysql.password", "");

    /** 播种行数：需足够大，才能让优化器按索引基数做出与生产同类的选择。 */
    private static final int ROWS = 20000;

    /** P1：{@code user_id} 在 200 个值之间轮转，故目标用户命中 ROWS/200 = 100 行。 */
    private static final long MEMBER_PROJECT_BASE = 920000L;
    private static final long MEMBER_USER_BASE = 810000L;
    private static final int MEMBER_USER_BUCKETS = 200;
    private static final long TARGET_MEMBER_USER = MEMBER_USER_BASE;

    /** P2：{@code (role_id, permission_id)} 取双射，保证唯一键成立；目标权限命中 100 行。 */
    private static final long RP_ROLE_BASE = 830000L;
    private static final int RP_ROLE_BUCKETS = 100;
    private static final long RP_PERM_BASE = 970000L;
    /** 目标权限：既是 {@code sys_permission} 的固定主键，也是目标关联行的 {@code permission_id}。 */
    private static final long TARGET_PERMISSION_ID = RP_PERM_BASE;

    private static boolean dbReachable;
    private static String unreachableReason;
    private static Connection physical;
    private static SqlSession session;
    private static ProjectMemberMapper projectMemberMapper;
    private static SysRolePermissionMapper rolePermissionMapper;
    private static final List<String> capturedSql = new ArrayList<>();

    @BeforeAll
    static void connect() {
        try {
            physical = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            physical.setAutoCommit(true);

            SingleConnectionDataSource dataSource = new SingleConnectionDataSource(physical, true);
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
            configuration.setEnvironment(new Environment("it-env", new JdbcTransactionFactory(), dataSource));
            configuration.addInterceptor(new SqlCapture());
            configuration.addMapper(ProjectMemberMapper.class);
            configuration.addMapper(SysRolePermissionMapper.class);
            configuration.addMapper(SysPermissionMapper.class);

            SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
            session = factory.openSession(true);
            projectMemberMapper = session.getMapper(ProjectMemberMapper.class);
            rolePermissionMapper = session.getMapper(SysRolePermissionMapper.class);

            try (Statement st = physical.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                rs.next();
            }
            dbReachable = true;
            seed();
        } catch (Exception e) {
            dbReachable = false;
            unreachableReason = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    @AfterAll
    static void disconnect() {
        try {
            if (session != null) session.close();
        } catch (Exception ignored) {
            // ignore
        }
        try {
            if (physical != null) {
                cleanup();
                physical.close();
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    @BeforeEach
    void requireDb() {
        assumeTrue(dbReachable,
                "本机 MySQL(" + MYSQL_URL + ") 不可达，跳过：" + unreachableReason + "。注意：跳过不等于通过。");
    }

    // ==================================================================================
    // 元数据：新增索引存在且列顺序与设计一致
    // ==================================================================================

    @Test
    @DisplayName("P1/P2：两处新增索引存在且列顺序与设计一致")
    void expectedIndexesExistWithExactColumnOrder() throws Exception {
        assertThat(indexColumnsOf("biz_project_member"))
                .as("P1：按 user_id 反查项目需要 user_id 前导索引")
                .containsEntry("idx_member_user", "user_id");
        assertThat(indexColumnsOf("sys_role_permission"))
                .as("P2：按 permission_id 清理关联需要 permission_id 前导索引")
                .containsEntry("idx_rp_permission", "permission_id");
    }

    // ==================================================================================
    // P1：listProjectIdsByUser —— WHERE user_id = ? 不再全表扫描
    // ==================================================================================

    @Test
    @DisplayName("P1：按 user_id 反查项目成员不再全表扫描（Handler_read_rnd_next 远小于行数）")
    void memberLookupDoesNotFullScan() throws Exception {
        capturedSql.clear();
        List<ProjectMember> rows = projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getUserId, TARGET_MEMBER_USER));
        Map<String, Long> delta = measureAll(List.of("Handler_read_rnd_next", "Handler_read_next"), () ->
                projectMemberMapper.selectList(
                        new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getUserId, TARGET_MEMBER_USER)));
        System.out.println("[P1] SQL=" + capturedSql);
        System.out.println("[P1] delta=" + delta + "（播种行数=" + ROWS
                + "，目标用户命中=" + rows.size() + "）");

        assertThat((long) rows.size())
                .as("目标用户应命中 %d 行，否则「扫得少」可能只是没读到数据", ROWS / MEMBER_USER_BUCKETS)
                .isEqualTo(ROWS / MEMBER_USER_BUCKETS);
        // 回退形态（无 user_id 前导索引）下 MySQL 只能顺序扫描聚簇索引整表，
        // 该计数器会等于行数；有 idx_member_user 后走索引区间扫描，实测为 0。
        assertThat(delta.get("Handler_read_rnd_next"))
                .as("按 user_id 查询的聚簇索引顺序扫描行数=%d 应远小于播种行数=%d；"
                                + "若接近行数说明 idx_member_user 缺失（P1 已回退）",
                        delta.get("Handler_read_rnd_next"), ROWS)
                .isLessThan(ROWS / 10);
    }

    // ==================================================================================
    // P2：deletePermission 清理角色-权限关联 —— WHERE permission_id = ? 不再全表扫描
    // ==================================================================================

    @Test
    @DisplayName("P2：按 permission_id 清理角色权限关联不再全表扫描（Handler_read_rnd_next 远小于行数）")
    void rolePermissionDeleteDoesNotFullScan() throws Exception {
        AdminPermissionServiceImpl service = new AdminPermissionServiceImpl(rolePermissionMapper);
        ReflectionTestUtils.setField(service, "baseMapper", session.getMapper(SysPermissionMapper.class));

        long before = countRows("SELECT COUNT(*) FROM sys_role_permission WHERE permission_id = "
                + TARGET_PERMISSION_ID);
        assertThat(before)
                .as("目标权限应预置 %d 条关联行（每个角色一条），否则「扫得少」可能只是没读到数据", RP_ROLE_BUCKETS)
                .isEqualTo(RP_ROLE_BUCKETS);

        capturedSql.clear();
        Map<String, Long> delta = measureAll(List.of("Handler_read_rnd_next", "Handler_read_next"), () ->
                service.deletePermission(TARGET_PERMISSION_ID));
        System.out.println("[P2] SQL=" + capturedSql);
        System.out.println("[P2] delta=" + delta + "（播种行数=" + ROWS + "，目标权限关联行=" + before + "）");

        assertThat(countRows("SELECT COUNT(*) FROM sys_role_permission WHERE permission_id = "
                + TARGET_PERMISSION_ID))
                .as("目标权限的关联行应已被清理，否则「扫得少」可能只是没执行删除")
                .isZero();
        // 回退形态（无 permission_id 前导索引）下 MySQL 只能全表扫描（聚簇索引或唯一索引），
        // 该计数器会等于行数；有 idx_rp_permission 后走索引区间扫描，实测为 0。
        assertThat(delta.get("Handler_read_rnd_next"))
                .as("清理角色权限关联的聚簇索引顺序扫描行数=%d 应远小于播种行数=%d；"
                                + "若接近行数说明 idx_rp_permission 缺失（P2 已回退）",
                        delta.get("Handler_read_rnd_next"), ROWS)
                .isLessThan(ROWS / 10);
    }

    // ==================================================================================
    // 元数据 / 计数器工具
    // ==================================================================================

    /** 返回 table 上所有索引的「索引名 -> 逗号拼接列清单」。 */
    private Map<String, String> indexColumnsOf(String table) throws SQLException {
        Map<String, List<String>> acc = new LinkedHashMap<>();
        String sql = "SELECT INDEX_NAME, COLUMN_NAME FROM information_schema.STATISTICS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY INDEX_NAME, SEQ_IN_INDEX";
        try (java.sql.PreparedStatement ps = physical.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    acc.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2));
                }
            }
        }
        Map<String, String> out = new LinkedHashMap<>();
        acc.forEach((k, v) -> out.put(k, String.join(",", v)));
        return out;
    }

    private long countRows(String sql) throws SQLException {
        try (Statement st = physical.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private interface Action {
        void run() throws Exception;
    }

    /** 单次执行 action，返回多个会话计数器的增量（保证所有计数器覆盖同一次执行）。 */
    private Map<String, Long> measureAll(List<String> counterNames, Action action) throws Exception {
        Map<String, Long> before = new LinkedHashMap<>();
        for (String name : counterNames) {
            before.put(name, counter(name));
        }
        action.run();
        Map<String, Long> delta = new LinkedHashMap<>();
        for (String name : counterNames) {
            delta.put(name, counter(name) - before.get(name));
        }
        return delta;
    }

    private long counter(String name) throws SQLException {
        try (Statement st = physical.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT CAST(VARIABLE_VALUE AS SIGNED) FROM performance_schema.session_status"
                             + " WHERE VARIABLE_NAME = '" + name + "'")) {
            return rs.next() ? rs.getLong(1) : -1L;
        }
    }

    private static void seed() throws SQLException {
        cleanup();
        try (Statement st = physical.createStatement()) {
            st.execute("SET SESSION cte_max_recursion_depth = 100000");

            // 供 P2 走真实 Service 路径：deletePermission 先 getById，故需一条真实的权限行。
            st.executeUpdate("INSERT INTO sys_permission"
                    + " (id, parent_id, permission_name, permission_key, permission_type, path, component,"
                    + "  icon, sort_order, visible, status, remark, deleted)"
                    + " VALUES (" + TARGET_PERMISSION_ID + ", 0, 'IT-IDX-PERM', 'it:idx:perm', 2, '', '',"
                    + " '', 0, 1, 1, '', 0)");

            st.executeUpdate("INSERT INTO biz_project_member (project_id, user_id, permission, created_time)"
                    + " WITH RECURSIVE seq AS (SELECT 1 n UNION ALL SELECT n + 1 FROM seq WHERE n < " + ROWS + ")"
                    + " SELECT " + MEMBER_PROJECT_BASE + " + n,"
                    + "        " + MEMBER_USER_BASE + " + ((n - 1) % " + MEMBER_USER_BUCKETS + "),"
                    + "        'view', NOW() FROM seq");

            // (role_id, permission_id) = (n-1)%100 + base, (n-1)/100 + base —— 双射，满足 uk_role_permission
            st.executeUpdate("INSERT INTO sys_role_permission (role_id, permission_id)"
                    + " WITH RECURSIVE seq AS (SELECT 1 n UNION ALL SELECT n + 1 FROM seq WHERE n < " + ROWS + ")"
                    + " SELECT " + RP_ROLE_BASE + " + ((n - 1) % " + RP_ROLE_BUCKETS + "),"
                    + "        " + RP_PERM_BASE + " + ((n - 1) DIV " + RP_ROLE_BUCKETS + ") FROM seq");

            st.execute("ANALYZE TABLE biz_project_member, sys_role_permission");
        }
    }

    private static void cleanup() throws SQLException {
        try (Statement st = physical.createStatement()) {
            st.executeUpdate("DELETE FROM biz_project_member WHERE project_id BETWEEN "
                    + MEMBER_PROJECT_BASE + " AND " + (MEMBER_PROJECT_BASE + ROWS));
            st.executeUpdate("DELETE FROM sys_role_permission WHERE permission_id BETWEEN "
                    + RP_PERM_BASE + " AND " + (RP_PERM_BASE + ROWS / RP_ROLE_BUCKETS));
            st.executeUpdate("DELETE FROM sys_permission WHERE id = " + TARGET_PERMISSION_ID);
        }
    }

    /** 记录 Mapper 实际下发的 SQL 原文（含 DELETE），便于人工核对与写进断言失败信息。 */
    @Intercepts({
            @Signature(type = Executor.class, method = "query",
                    args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
            @Signature(type = Executor.class, method = "update",
                    args = {MappedStatement.class, Object.class})
    })
    static class SqlCapture implements Interceptor {
        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            BoundSql boundSql = ms.getBoundSql(invocation.getArgs()[1]);
            capturedSql.add(boundSql.getSql().replaceAll("\\s+", " ").trim());
            return invocation.proceed();
        }

        @Override
        public Object plugin(Object target) {
            return Plugin.wrap(target, this);
        }
    }
}

/*
 * ==========================================================================================
 * 基线 / 变异实验结果（2026-11-05 本机实测，MySQL 8.4.9 @127.0.0.1:3307）
 * ==========================================================================================
 * 命令（vb-gateway 聚合全部业务模块，是放置跨模块 IT 的唯一位置）：
 *   mvn -o -pl vb-gateway -am verify "-Dit.test=MemberAndPermissionIndexIT"
 *       "-Dfailsafe.failIfNoSpecifiedTests=false"
 *       "-Dit.mysql.url=jdbc:mysql://127.0.0.1:3307/visual_builder"
 *
 * 基线（方案 A 索引已应用到 3307）：Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
 *   · P1  delta={Handler_read_rnd_next=0, Handler_read_next=100}   （播种 20000 行，目标用户命中 100 行）
 *   · P2  delta={Handler_read_rnd_next=0, Handler_read_next=100}   （播种 20000 行，目标权限关联 100 行）
 *   · 元数据：biz_project_member.idx_member_user(user_id)、sys_role_permission.idx_rp_permission(permission_id) 均存在
 *   两条被测语句实际下发的 SQL（由 SqlCapture 捕获）：
 *     P1  SELECT id,project_id,user_id,permission,created_time FROM biz_project_member WHERE (user_id = ?)
 *     P2  DELETE FROM sys_role_permission WHERE (permission_id = ?)
 *
 * 变异（在 3307 上执行 DROP INDEX idx_member_user ON biz_project_member;
 *       DROP INDEX idx_rp_permission ON sys_role_permission;，回退为治理前形态）：
 *
 *   Tests run: 3, Failures: 3, Errors: 0, Skipped: 0   —— 3/3 全部失败
 *   · P1  delta={Handler_read_rnd_next=20001, Handler_read_next=0}
 *       [按 user_id 查询的聚簇索引顺序扫描行数=20001 应远小于播种行数=20000；若接近行数说明 idx_member_user 缺失（P1 已回退）]
 *       Expecting actual: 20001L to be less than: 2000L
 *   · P2  delta={Handler_read_rnd_next=20001, Handler_read_next=0}
 *       [清理角色权限关联的聚簇索引顺序扫描行数=20001 应远小于播种行数=20000；若接近行数说明 idx_rp_permission 缺失（P2 已回退）]
 *       Expecting actual: 20001L to be less than: 2000L
 *   · expectedIndexesExistWithExactColumnOrder
 *       [P1：按 user_id 反查项目需要 user_id 前导索引] could not find ["idx_member_user"="user_id"]
 *
 * 变异后已把 3307 索引还原为治理后形态（与 sql/init.sql 一致）：Tests run: 3, Failures: 0, BUILD SUCCESS。
 *
 * 注：新增索引不改变 A4 的最左前缀冗余登记表——idx_member_user 不是 uk_project_user(project_id, user_id)
 *   的前缀，idx_rp_permission 也不是 uk_role_permission(role_id, permission_id) 的前缀。
 * ==========================================================================================
 */
