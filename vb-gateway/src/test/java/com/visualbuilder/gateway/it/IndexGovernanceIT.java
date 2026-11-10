package com.visualbuilder.gateway.it;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.page.mapper.PageMapper;
import com.visualbuilder.publish.entity.PublishRecord;
import com.visualbuilder.publish.mapper.PublishRecordMapper;
import com.visualbuilder.user.entity.SysUser;
import com.visualbuilder.user.mapper.SysUserMapper;
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A2 / A3 / A4 索引治理的证据链：「回退即失败」的集成测试。
 *
 * <h3>被测改动（全部落在 {@code sql/init.sql}）</h3>
 * <ul>
 *   <li><b>A2</b>：{@code sys_user} 新增 {@code idx_user_email(email)}、{@code idx_user_phone(phone)}，
 *       支撑 {@code SysUserServiceImpl#login} 的 {@code username OR email OR phone} 查询。</li>
 *   <li><b>A3a</b>：{@code biz_publish_record} 新增 {@code idx_publish_project_published(project_id, published_at)}，
 *       支撑 {@code PublishServiceImpl#getPublishHistory} 的 {@code project_id + ORDER BY published_at DESC}。</li>
 *   <li><b>A3b</b>：{@code biz_page} 新增 {@code idx_page_project_user_deleted(project_id, user_id, deleted)}，
 *       支撑 {@code PageMapper#selectTrashPage} 的回收站查询。</li>
 *   <li><b>A4</b>：删除 4 个被更宽索引最左前缀覆盖的冗余索引
 *       （{@code biz_page.idx_project_id}、{@code biz_component.idx_category_id}、
 *       {@code biz_template.idx_template_status}、{@code res_file.idx_user_type}）。</li>
 * </ul>
 *
 * <h3>真实参与</h3>
 * <ul>
 *   <li>真实 MySQL 8.4.9 服务端（默认 {@code 127.0.0.1:3306/visual_builder}，本机以
 *       {@code -Dit.mysql.url=jdbc:mysql://127.0.0.1:3307/visual_builder} 覆盖）与真实 InnoDB 索引结构。</li>
 *   <li>查询由<b>真实 Mapper</b> 发出（{@link SysUserMapper}、{@link PublishRecordMapper}、{@link PageMapper}），
 *       配真实 MyBatis-Plus {@code SqlSessionFactory} 与 {@link PaginationInnerInterceptor}，
 *       因此被测 SQL（含逻辑删除条件、分页 COUNT）与生产完全一致；测试内以 {@link SqlCapture}
 *       记录实际下发的 SQL 原文，供人工核对。</li>
 *   <li>证据取自 MySQL 自身会话级计数器（{@code performance_schema.session_status} 的
 *       {@code Handler_read_rnd_next} / {@code Handler_read_next} / {@code Handler_read_prev} /
 *       {@code Sort_rows} / {@code Sort_scan}），而非对 SQL 文本的阅读或对执行计划的推断。
 *       所有查询与计数运行在<b>同一条物理连接</b>上（{@link SingleConnectionDataSource} 包裹单个 {@link Connection}），
 *       保证会话计数连续可比。</li>
 *   <li>A4 的证据取自 {@code information_schema.STATISTICS}：索引是否存在的<b>事实</b>，
 *       以及「某索引列清单是否为另一索引列清单的严格最左前缀」这一可由元数据判定的性质。</li>
 * </ul>
 *
 * <h3>替身参与</h3>
 * <p><b>无</b>。本测试不引入任何 Mockito 替身。</p>
 *
 * <h3>环境差异</h3>
 * 本机 MySQL 为 {@code 8.4.9}，生产镜像为 {@code mysql:8.0}。InnoDB 页大小、索引 B+ 树结构、
 * {@code Handler_read_*} / {@code Sort_*} 计数语义一致；但<b>优化器代价模型与
 * index_merge 取舍策略存在版本差异</b>。因此断言全部采用「与数据规模成比例的宽松阈值」
 * （例如「远小于行数」），而非任何绝对常量，也不断言 MySQL 一定选择某个具体索引名
 * 之外的细节。
 *
 * <h3>不可证伪的部分（诚实声明）</h3>
 * <ul>
 *   <li>本测试使用<b>本测试自行播种</b>的数据（每表 20000 行）来触发优化器选择索引；
 *       它证明的是「在具备该索引时这三条查询不再全表扫描 / 不再 filesort」，
 *       <b>不证明</b>生产数据分布下的查询耗时下降幅度。</li>
 *   <li>A4 只证明「被更宽索引最左前缀覆盖」这一结构性冗余已消除；
 *       <b>不证明</b>删除后写入吞吐提升的具体数值。</li>
 *   <li>{@code Handler_read_rnd_next} 为 0 只代表该次查询未做全表顺序扫描，
 *       <b>不证明</b>该查询永不回退为全表扫描（数据分布极端时仍可能）。</li>
 *   <li>本测试<b>不覆盖</b> {@code biz_project.idx_user_id} 与
 *       {@code biz_publish_record.idx_publish_project_id} 两处同类冗余——它们已被发现但未经确认，
 *       见 {@link #noUnregisteredRedundantIndex()} 中的登记表。</li>
 * </ul>
 *
 * <h3>变异实验结果（2026-11-05 本机实测，MySQL 8.4.9 @127.0.0.1:3307）</h3>
 * <p>把 3307 上的索引还原为治理前形态后，本类 <b>6 个用例全部失败</b>（6/6）；
 * 具体失败断言原文与计数器数值见类末尾注释。</p>
 *
 * <p>环境不可达时全部用例判定为<b>跳过（skipped）而非通过</b>。</p>
 */
class IndexGovernanceIT {

    private static final String MYSQL_URL = System.getProperty("it.mysql.url",
            "jdbc:mysql://localhost:3306/visual_builder");
    private static final String MYSQL_USER = System.getProperty("it.mysql.user", "root");
    private static final String MYSQL_PASSWORD = System.getProperty("it.mysql.password", "");

    /** 播种行数：需足够大，才能让优化器按索引基数做出与生产同类的选择。 */
    private static final int ROWS = 20000;

    private static final String USERNAME_PREFIX = "itidx";
    private static final long PUBLISH_PROJECT_ID = 999111L;
    private static final long PAGE_PROJECT_ID = 999222L;
    private static final long PAGE_USER_BASE = 700100L;
    /** 回收站播种：user_id 在 10 个值之间轮转，使复合索引的区分度体现出来。 */
    private static final int PAGE_USER_BUCKETS = 10;

    private static boolean dbReachable;
    private static String unreachableReason;
    private static Connection physical;
    private static SqlSession session;
    private static SysUserMapper sysUserMapper;
    private static PublishRecordMapper publishRecordMapper;
    private static PageMapper pageMapper;
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
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
            configuration.addInterceptor(interceptor);
            configuration.addInterceptor(new SqlCapture());
            configuration.addMapper(SysUserMapper.class);
            configuration.addMapper(PublishRecordMapper.class);
            configuration.addMapper(PageMapper.class);

            SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
            session = factory.openSession(true);
            sysUserMapper = session.getMapper(SysUserMapper.class);
            publishRecordMapper = session.getMapper(PublishRecordMapper.class);
            pageMapper = session.getMapper(PageMapper.class);

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
    // A4：结构性断言（不需要数据）
    // ==================================================================================

    @Test
    @DisplayName("A4：被更宽索引最左前缀覆盖的 4 个冗余索引已不存在")
    void redundantIndexesRemoved() throws Exception {
        assertThat(indexPrefixesOf("biz_page"))
                .as("biz_page.idx_project_id 已被 idx_page_project_status(project_id, status, deleted) 最左前缀覆盖，应删除")
                .doesNotContainKey("idx_project_id");
        assertThat(indexPrefixesOf("biz_component"))
                .as("biz_component.idx_category_id 已被 idx_component_category_status(category_id, status) 覆盖，应删除")
                .doesNotContainKey("idx_category_id");
        assertThat(indexPrefixesOf("biz_template"))
                .as("biz_template.idx_template_status 已被 idx_template_status_usecount(status, use_count) 覆盖，应删除")
                .doesNotContainKey("idx_template_status");
        assertThat(indexPrefixesOf("res_file"))
                .as("res_file.idx_user_type 已被 idx_resource_user_type_deleted(user_id, resource_type, deleted) 覆盖，应删除")
                .doesNotContainKey("idx_user_type");
    }

    @Test
    @DisplayName("A2/A3：新增索引存在且列顺序与设计一致")
    void expectedIndexesExistWithExactColumnOrder() throws Exception {
        assertThat(indexPrefixesOf("sys_user"))
                .as("A2：登录 OR 查询需要 email / phone 单列索引")
                .containsEntry("idx_user_email", "email")
                .containsEntry("idx_user_phone", "phone");
        assertThat(indexPrefixesOf("biz_publish_record"))
                .as("A3a：发布历史需要 (project_id, published_at) 复合索引以消除 filesort")
                .containsEntry("idx_publish_project_published", "project_id,published_at");
        assertThat(indexPrefixesOf("biz_page"))
                .as("A3b：回收站需要 (project_id, user_id, deleted) 复合索引")
                .containsEntry("idx_page_project_user_deleted", "project_id,user_id,deleted");
    }

    @Test
    @DisplayName("A4：全库不出现「未登记」的最左前缀冗余索引")
    void noUnregisteredRedundantIndex() throws Exception {
        // 已发现但尚未获授权处理的同类冗余（保持登记，不得新增）：
        //   biz_project.idx_user_id            ⊂ idx_project_user_deleted(user_id, deleted)
        //   biz_publish_record.idx_publish_project_id ⊂ idx_publish_project_published(project_id, published_at)
        Set<String> known = Set.of(
                "biz_project.idx_user_id<=idx_project_user_deleted",
                "biz_publish_record.idx_publish_project_id<=idx_publish_project_published");

        Set<String> found = findLeftmostPrefixRedundancies();
        System.out.println("[A4] 全库最左前缀冗余索引=" + found);
        assertThat(found)
                .as("除已登记的 2 处外不得新增最左前缀冗余索引；新增说明索引治理被回退")
                .isEqualTo(known);
    }

    // ==================================================================================
    // A2：登录 username OR email OR phone —— 不再全表扫描
    // ==================================================================================

    @Test
    @DisplayName("A2：登录 OR 查询不再全表扫描（Handler_read_rnd_next 远小于行数）")
    void loginOrQueryDoesNotFullScan() throws Exception {
        String login = USERNAME_PREFIX + "nonexistent";
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, login)
                .or().eq(SysUser::getEmail, login + "@example.com")
                .or().eq(SysUser::getPhone, "19999999999");

        capturedSql.clear();
        long rndNext = measure("Handler_read_rnd_next", () -> sysUserMapper.selectOne(wrapper));
        System.out.println("[A2] SQL=" + capturedSql);
        System.out.println("[A2] Handler_read_rnd_next=" + rndNext + "（播种行数=" + ROWS + "）");

        // 回退形态（缺少 idx_user_email / idx_user_phone）下 MySQL 只能顺序扫描整表，
        // 该计数器会等于行数（本机实测 20002）。加入两列索引后走 index_merge，实测为 0。
        assertThat(rndNext)
                .as("登录 OR 查询的顺序扫描行数=%d 应远小于播种行数=%d；若接近行数说明 email/phone 索引缺失（A2 已回退）",
                        rndNext, ROWS)
                .isLessThan(ROWS / 10);
    }

    // ==================================================================================
    // A3a：发布历史 project_id + ORDER BY published_at DESC —— 消除 filesort
    // ==================================================================================

    @Test
    @DisplayName("A3a：发布历史查询不再 filesort（Sort_rows/Sort_scan 增量为 0）")
    void publishHistoryAvoidsFilesort() throws Exception {
        capturedSql.clear();
        Map<String, Long> delta = measureAll(List.of("Sort_rows", "Sort_scan"), () ->
                publishRecordMapper.selectPage(
                        new Page<PublishRecord>(1, 20),
                        new LambdaQueryWrapper<PublishRecord>()
                                .eq(PublishRecord::getProjectId, PUBLISH_PROJECT_ID)
                                .orderByDesc(PublishRecord::getPublishedAt)));
        System.out.println("[A3a] SQL=" + capturedSql);
        System.out.println("[A3a] delta=" + delta + "（该项目行数=" + ROWS + "）");

        // 回退形态（只有 project_id 单列索引）下，必须把该项目全部记录取出再排序，
        // 本机实测 Sort_rows=20（LIMIT 使 filesort 用优先队列，故按 Sort_scan 更稳健）。
        // 有 (project_id, published_at) 复合索引时索引顺序即排序顺序，两者均为 0。
        assertThat(delta.get("Sort_rows"))
                .as("发布历史排序行数应为 0（索引顺序即 published_at 顺序）；非 0 说明 (project_id, published_at) 索引缺失或未被使用（A3a 已回退）")
                .isZero();
        assertThat(delta.get("Sort_scan"))
                .as("发布历史不得依赖扫描式 filesort（Sort_scan 增量应为 0）")
                .isZero();
    }

    // ==================================================================================
    // A3b：回收站 project_id + user_id + deleted —— 只读目标用户的那部分索引项
    // ==================================================================================

    @Test
    @DisplayName("A3b：回收站查询只扫描目标用户的索引项（远小于项目全部索引项）")
    void trashQueryReadsOnlyTargetUserEntries() throws Exception {
        long targetUser = PAGE_USER_BASE;
        long expectedTrashRows = (long) ROWS / PAGE_USER_BUCKETS / 2;

        capturedSql.clear();
        long readNext = measure("Handler_read_next", () -> pageMapper.selectTrashPage(
                new Page<com.visualbuilder.page.entity.Page>(1, 20), PAGE_PROJECT_ID, targetUser));
        System.out.println("[A3b] SQL=" + capturedSql);
        System.out.println("[A3b] Handler_read_next=" + readNext
                + "（项目总行数=" + ROWS + "，目标用户已删行数=" + expectedTrashRows + "）");

        // 回退形态（无 (project_id, user_id, deleted) 复合索引）下只能按 project_id 扫描，
        // 读取量约为项目总行数（本机实测 21999）。有该索引后只读目标用户那一段（本机实测 2000）。
        assertThat(readNext)
                .as("回收站查询扫描的索引项=%d 应远小于项目总行数=%d；若接近总行数说明复合索引缺失（A3b 已回退）",
                        readNext, ROWS)
                .isLessThan(ROWS / 4);
    }

    // ==================================================================================
    // 元数据 / 计数器工具
    // ==================================================================================

    /** 返回 table 上所有索引的「索引名 -> 逗号拼接列清单」。 */
    private Map<String, String> indexPrefixesOf(String table) throws SQLException {
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

    /** 扫描全库，列出「非唯一、非主键」索引中列清单为另一索引严格最左前缀的项。 */
    private Set<String> findLeftmostPrefixRedundancies() throws SQLException {
        Map<String, Map<String, String>> byTable = new LinkedHashMap<>();
        String sql = "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)"
                + " FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()"
                + " GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE";
        try (Statement st = physical.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                if (rs.getInt(3) != 1) continue;              // 只看非唯一索引
                if ("PRIMARY".equals(rs.getString(2))) continue;
                byTable.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>())
                        .put(rs.getString(2), rs.getString(4));
            }
        }
        Set<String> found = new LinkedHashSet<>();
        byTable.forEach((table, idx) -> idx.forEach((narrow, narrowCols) -> idx.forEach((wide, wideCols) -> {
            if (narrow.equals(wide)) return;
            if (narrowCols.length() < wideCols.length() && wideCols.startsWith(narrowCols + ",")) {
                found.add(table + "." + narrow + "<=" + wide);
            }
        })));
        return found;
    }

    private interface Action {
        void run() throws Exception;
    }

    private long measure(String counterName, Action action) throws Exception {
        return measureAll(List.of(counterName), action).get(counterName);
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

            st.executeUpdate("INSERT INTO sys_user (tenant_id, username, password, nickname, email, phone, status)"
                    + " WITH RECURSIVE seq AS (SELECT 1 n UNION ALL SELECT n + 1 FROM seq WHERE n < " + ROWS + ")"
                    + " SELECT 0, CONCAT('" + USERNAME_PREFIX + "', n), 'x', '',"
                    + "        CONCAT('" + USERNAME_PREFIX + "', n, '@example.com'),"
                    + "        CONCAT('139', LPAD(n, 8, '0')), 1 FROM seq");

            st.executeUpdate("INSERT INTO biz_publish_record"
                    + " (page_id, project_id, version_no, status, access_url, changelog, published_by, published_at)"
                    + " WITH RECURSIVE seq AS (SELECT 1 n UNION ALL SELECT n + 1 FROM seq WHERE n < " + ROWS + ")"
                    + " SELECT n, " + PUBLISH_PROJECT_ID + ", 1, 1, '', '', 1,"
                    + "        DATE_ADD('2024-01-01 00:00:00', INTERVAL n MINUTE) FROM seq");

            st.executeUpdate("INSERT INTO biz_page"
                    + " (project_id, name, page_path, page_type, schema_content, schema_version,"
                    + "  cover_url, status, is_home, user_id, deleted)"
                    + " WITH RECURSIVE seq AS (SELECT 1 n UNION ALL SELECT n + 1 FROM seq WHERE n < " + ROWS + ")"
                    + " SELECT " + PAGE_PROJECT_ID + ", CONCAT('p', n), CONCAT('/itidx-', n), 'h5', NULL, 0, '', 0, 0,"
                    + "        " + PAGE_USER_BASE + " + (n % " + PAGE_USER_BUCKETS + "),"
                    + "        IF((n DIV " + PAGE_USER_BUCKETS + ") % 2 = 0, 1, 0) FROM seq");

            st.execute("ANALYZE TABLE sys_user, biz_publish_record, biz_page");
        }
    }

    private static void cleanup() throws SQLException {
        try (Statement st = physical.createStatement()) {
            st.executeUpdate("DELETE FROM sys_user WHERE username LIKE '" + USERNAME_PREFIX + "%'");
            st.executeUpdate("DELETE FROM biz_publish_record WHERE project_id = " + PUBLISH_PROJECT_ID);
            st.executeUpdate("DELETE FROM biz_page WHERE project_id = " + PAGE_PROJECT_ID);
        }
    }

    /** 记录 Mapper 实际下发的 SQL 原文，便于人工核对与写进断言失败信息。 */
    @Intercepts(@Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
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
 *   mvn -o -pl vb-gateway -am verify "-Dit.test=IndexGovernanceIT"
 *       "-Dfailsafe.failIfNoSpecifiedTests=false"
 *       "-Dit.mysql.url=jdbc:mysql://127.0.0.1:3307/visual_builder"
 *
 * 基线（索引治理已应用到 3307）：Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
 *   · A2  Handler_read_rnd_next = 0            （播种 20000 行）
 *   · A3a Sort_rows = 0, Sort_scan = 0
 *   · A3b Handler_read_next = 2000             （项目 20000 行，目标用户桶已删 1000 行）
 *   · A4  全库最左前缀冗余索引 = 已登记的 2 处
 *
 * 变异（把 3307 索引还原为治理前形态：DROP sys_user.idx_user_email / sys_user.idx_user_phone /
 *       biz_publish_record.idx_publish_project_published / biz_page.idx_page_project_user_deleted，
 *       并还原 biz_page.idx_project_id、biz_component.idx_category_id、
 *       biz_template.idx_template_status、res_file.idx_user_type）：
 *
 *   Tests run: 6, Failures: 6, Errors: 0, Skipped: 0   —— 6/6 全部失败
 *   · loginOrQueryDoesNotFullScan
 *       [登录 OR 查询的顺序扫描行数=20002 应远小于播种行数=20000；若接近行数说明 email/phone 索引缺失（A2 已回退）]
 *       Expecting actual: 20002L to be less than: 2000L
 *   · publishHistoryAvoidsFilesort
 *       [发布历史排序行数应为 0（索引顺序即 published_at 顺序）…]
 *       expected: 0L but was: 20L     （实测 delta={Sort_rows=20, Sort_scan=1}）
 *   · trashQueryReadsOnlyTargetUserEntries
 *       [回收站查询扫描的索引项=43998 应远小于项目总行数=20000…]
 *       Expecting actual: 43998L to be less than: 5000L
 *   · expectedIndexesExistWithExactColumnOrder
 *       [A2：登录 OR 查询需要 email / phone 单列索引] could not find ["idx_user_email"="email"]
 *   · redundantIndexesRemoved
 *       [biz_page.idx_project_id 已被 idx_page_project_status(project_id, status, deleted) 最左前缀覆盖，应删除]
 *   · noUnregisteredRedundantIndex
 *       全库最左前缀冗余索引由已登记的 2 处变为 5 处（多出 biz_component.idx_category_id、
 *       biz_page.idx_project_id、biz_template.idx_template_status、res_file.idx_user_type）
 *
 * 变异后已把 3307 索引还原为治理后形态（与 sql/init.sql 一致），与 ListSchemaProjectionIT
 * 合并重跑基线：Tests run: 9, Failures: 0, Errors: 0, Skipped: 0（BUILD SUCCESS）。
 * ==========================================================================================
 */
