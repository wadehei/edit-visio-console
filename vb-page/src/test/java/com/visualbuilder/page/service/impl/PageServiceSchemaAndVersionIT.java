package com.visualbuilder.page.service.impl;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.page.dto.PageSchemaDTO;
import com.visualbuilder.page.mapper.PageMapper;
import com.visualbuilder.page.mapper.PageVersionMapper;
import com.visualbuilder.project.service.ProjectMemberService;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * t6 / t8 证据链：{@code PageServiceImpl} 两处改动的「回退即失败」集成测试。
 *
 * <h3>被测对象</h3>
 * <ul>
 *   <li><b>t6</b>：{@code listPages} 中 {@code wrapper.select(Page.class, info -> ...)} 排除
 *       {@code biz_page.schema_content}（MySQL {@code JSON} 类型大字段）。</li>
 *   <li><b>t8</b>：{@code createVersionSnapshot} 中清理过期版本用
 *       {@code pageVersionMapper.delete(...in(ids))} 单条 SQL 完成。</li>
 * </ul>
 *
 * <h3>真实参与</h3>
 * <ul>
 *   <li>真实 MySQL 8.4.9 服务端（{@code 127.0.0.1:3307/visual_builder}）、真实 InnoDB 存储引擎、
 *       真实 {@code JSON}/DYNAMIC 行格式与 off-page 溢出页。</li>
 *   <li>真实 MyBatis-Plus {@code SqlSessionFactory} + {@link PaginationInnerInterceptor}，
 *       因此 {@code listPages} 走的是真实的分页 SQL 与 {@code COUNT} 查询。</li>
 *   <li>证据取自 MySQL 自身会话级状态计数器
 *       （{@code Innodb_buffer_pool_read_requests} / {@code Com_delete}），
 *       而非对代码的阅读或对返回值的推断。所有 SQL 与状态计数运行在<b>同一条物理连接</b>
 *       （{@link SingleConnectionDataSource} 包裹的单个 {@code Connection}）上，保证会话计数连续可比。</li>
 * </ul>
 *
 * <h3>替身参与（Mockito）</h3>
 * <ul>
 *   <li>{@link ProjectMemberService} 为 Mockito 替身，{@code isProjectOwnerOrMember(...)} 恒返回 {@code true}——
 *       本测试不覆盖权限链路（权限已有独立单测）。</li>
 *   <li>{@code SecurityUtils} 用其自带的 ThreadLocal 上下文注入当前用户，未使用替身。</li>
 *   <li>{@link PageMapper} / {@link PageVersionMapper} 均为<b>真实</b> Mapper（真实 SQL），不是替身。</li>
 * </ul>
 *
 * <h3>环境差异</h3>
 * 本机 MySQL 为 {@code 8.4.9}，生产镜像为 {@code mysql:8.0}。二者 InnoDB 页大小（16KB）、
 * DYNAMIC 行格式、大字段 off-page 存储策略与 {@code Innodb_buffer_pool_read_requests} /
 * {@code Com_delete} 计数语义一致；但优化器代价模型、缓冲池热状态、并行读等存在版本差异，
 * 故本测试采用<b>相对比较</b>（被测查询 vs 对照查询）与<b>等值断言</b>（Com_delete == 1），
 * 不依赖任何绝对页数常量。
 *
 * <h3>不可证伪的部分（诚实声明）</h3>
 * <ul>
 *   <li>t6 不证明「生产环境网络传输字节数下降」——本测试只观测服务端 InnoDB 逻辑页请求数，
 *       不测量 JDBC 结果集字节数。</li>
 *   <li>t8 不证明「并发场景下批量删除的原子性优于循环删除」——只证明删除动作由<b>几条</b> DELETE 语句发起。</li>
 * </ul>
 *
 * <h3>变异实验结果（2026-11-05 本机实测，MySQL 8.4.9 @127.0.0.1:3307）</h3>
 * 基线（未变异）：{@code mvn -pl vb-page -am verify} → {@code Tests run: 2, Failures: 0, Errors: 0}；
 * t6 实测 {@code Innodb_buffer_pool_read_requests} 增量 listPages=26、controlSelectAll=845（32×）。
 * <ol>
 *   <li><b>t6 变异</b>（注释掉 {@code wrapper.select(...)}）：{@code Tests run: 2, Failures: 1, Errors: 0}，
 *       失败断言原文：{@code [listPages（排除 schema_content）逻辑页请求数=859 应显著小于全列对照=845；...]
 *       Expecting actual: 859L to be less than: 845L}（listPages 骤升至 859，与全列对照 845 同量级）。</li>
 *   <li><b>t8 变异</b>（改为循环 {@code deleteById}）：{@code Tests run: 2, Failures: 1, Errors: 0}，
 *       失败断言原文：{@code [过期版本清理必须由恰好 1 条 DELETE 语句完成...] expected: 1L but was: 3L}
 *       （{@code Com_delete} 增量由 1 变为被删版本数 3）。</li>
 * </ol>
 * 两次变异均被各自用例捕获，证明两项测试对被测改动具备鉴别力。
 *
 * <p>环境不可达时全部用例判定为<b>跳过（skipped）而非通过</b>。</p>
 */
class PageServiceSchemaAndVersionIT {

    // ---- 连接参数：默认指向 3306，本机用 -Dit.mysql.url=... 覆盖到 3307 ----
    private static final String MYSQL_URL = System.getProperty("it.mysql.url",
            "jdbc:mysql://localhost:3306/visual_builder");
    private static final String MYSQL_USER = System.getProperty("it.mysql.user", "root");
    private static final String MYSQL_PASSWORD = System.getProperty("it.mysql.password", "");

    private static final long TEST_USER_ID = 700100L;
    private static final long T6_PROJECT_ID = 987654321L;
    private static final long T8_PAGE_ID_BASE = 500000000L;
    private static final String T6_PATH_PREFIX = "/it-schema-exclusion-";
    private static final String T8_PATH_PREFIX = "/it-version-cleanup-";

    /** t6：大字段体积，确保 JSON 落入 off-page 溢出页（远端格式 DYNAMIC）。 */
    private static final int BIG_SCHEMA_BYTES = 300 * 1024;
    /** t6：同一 project 下的行数。 */
    private static final int T6_ROWS = 40;
    /** t6：分页大小（<= LimitsConst.MAX_PAGE_SIZE=100）。 */
    private static final int T6_PAGE_SIZE = 50;

    private static boolean dbReachable;
    private static String unreachableReason;
    private static Connection physical;
    private static SingleConnectionDataSource dataSource;
    private static SqlSessionFactory sqlSessionFactory;
    private static SqlSession session;
    private static PageMapper pageMapper;
    private static PageVersionMapper pageVersionMapper;

    private PageServiceImpl pageService;
    private ProjectMemberService projectMemberServiceStub;
    private final List<Long> createdPageIds = new ArrayList<>();

    @BeforeAll
    static void connect() {
        try {
            physical = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            physical.setAutoCommit(true);
            dataSource = new SingleConnectionDataSource(physical, true);

            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
            configuration.setEnvironment(new Environment("it-env", new JdbcTransactionFactory(), dataSource));
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
            configuration.addInterceptor(interceptor);
            configuration.addMapper(PageMapper.class);
            configuration.addMapper(PageVersionMapper.class);

            sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
            session = sqlSessionFactory.openSession(true);
            pageMapper = session.getMapper(PageMapper.class);
            pageVersionMapper = session.getMapper(PageVersionMapper.class);

            try (Statement st = physical.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                rs.next();
            }
            // 参照查询会读取全部列（含大 JSON）并按 updated_time 排序，需要更大 sort buffer；
            // 会话语义：生产查询同样受益但不依赖此设置（其投影已排除大字段）。
            try (Statement st = physical.createStatement()) {
                st.execute("SET SESSION sort_buffer_size = 268435456");
            }
            dbReachable = true;
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
            if (physical != null) physical.close();
        } catch (Exception ignored) {
            // ignore
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(dbReachable,
                "本机 MySQL(" + MYSQL_URL + ") 不可达，跳过：" + unreachableReason + "。注意：跳过不等于通过。");

        SecurityUtils.setContext(TEST_USER_ID, "it-tester");
        projectMemberServiceStub = mock(ProjectMemberService.class);
        when(projectMemberServiceStub.isProjectOwnerOrMember(anyLong(), anyLong())).thenReturn(true);

        pageService = new PageServiceImpl(pageVersionMapper, new ObjectMapper(), projectMemberServiceStub);
        // ServiceImpl 的 baseMapper 是 @Autowired 字段，非 Spring 上下文下需手动注入真实 Mapper
        ReflectionTestUtils.setField(pageService, "baseMapper", pageMapper);

        cleanup();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dbReachable) {
            cleanup();
        }
        SecurityUtils.clearContext();
    }

    private void cleanup() throws SQLException {
        try (Statement st = physical.createStatement()) {
            st.executeUpdate("DELETE FROM biz_page WHERE page_path LIKE '" + T6_PATH_PREFIX + "%'");
            st.executeUpdate("DELETE FROM biz_page WHERE page_path LIKE '" + T8_PATH_PREFIX + "%'");
        }
        if (!createdPageIds.isEmpty()) {
            String ids = createdPageIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("-1");
            try (Statement st = physical.createStatement()) {
                st.executeUpdate("DELETE FROM biz_page_version WHERE page_id IN (" + ids + ")");
            }
            createdPageIds.clear();
        }
    }

    // ==================================================================================
    // t6：listPages 排除大字段 —— 用 InnoDB 逻辑页请求数证明确实少读了
    // ==================================================================================

    @Test
    @DisplayName("t6：listPages 排除 schema_content 后，InnoDB 逻辑页请求数显著低于全列对照查询")
    void listPagesExcludesLargeSchemaContent() throws Exception {
        insertBigPages();

        // 预热：让两条查询都至少跑过一次，消除首次执行的额外开销差异（计数为逻辑请求，不受缓存影响）
        pageService.listPages(T6_PROJECT_ID, 1, T6_PAGE_SIZE, null);
        runControlFullSelect();

        long listPagesDelta = measureBufferPool(() ->
                pageService.listPages(T6_PROJECT_ID, 1, T6_PAGE_SIZE, null));
        long controlDelta = measureBufferPool(this::runControlFullSelect);

        System.out.println("[t6] Innodb_buffer_pool_read_requests delta:"
                + " listPages=" + listPagesDelta
                + " controlSelectAll=" + controlDelta
                + " ratio(control/listPages)=" + (listPagesDelta == 0 ? "inf" : (controlDelta / listPagesDelta)));

        // 当前实现（排除大字段）读取量必须显著低于「读取全部列（含 schema_content）」的对照查询。
        // 回退形态（删掉 select(...) 那行）会退化为读取全部列，其读取量 >= 对照查询，本断言即失败。
        assertThat(listPagesDelta)
                .as("listPages（排除 schema_content）逻辑页请求数=%d 应显著小于全列对照=%d；"
                                + "若接近或超过对照，说明大字段未被排除（可能已回退）",
                        listPagesDelta, controlDelta)
                .isLessThan(controlDelta);
        assertThat(listPagesDelta * 2)
                .as("listPages 逻辑页请求数=%d 应不足对照查询=%d 的一半（留出足够鉴别余量）",
                        listPagesDelta, controlDelta)
                .isLessThan(controlDelta);

        // 顺带确认列表确实取到了数据（否则“读得少”可能是没读到行）
        assertThat(pageService.listPages(T6_PROJECT_ID, 1, T6_PAGE_SIZE, null).getPagination().getTotal())
                .as("同一 project 下应有 %d 条数据", T6_ROWS)
                .isEqualTo(T6_ROWS);
    }

    private void insertBigPages() throws Exception {
        // 生产写入路径为「JSON 对象文本 -> JacksonTypeHandler 再序列化 -> 存为 JSON 字符串」，
        // 因此这里也以 JSON 字符串字面量落库，保证回退形态读取 schema_content 时不会因类型处理器报错，
        // 从而让变异只体现在「读取量」这一被测属性上。
        String schemaText = bigJson(BIG_SCHEMA_BYTES);
        String dbJsonStringLiteral = new ObjectMapper().writeValueAsString(schemaText);
        String sql = "INSERT INTO biz_page"
                + " (project_id,name,page_path,page_type,schema_content,schema_version,cover_url,status,is_home,"
                + "  user_id,created_time,updated_time,deleted)"
                + " VALUES (?,?,?,'h5',CAST(? AS JSON),0,'',0,0,?,NOW(),NOW(),0)";
        try (PreparedStatement ps = physical.prepareStatement(sql)) {
            for (int i = 0; i < T6_ROWS; i++) {
                ps.setLong(1, T6_PROJECT_ID);
                ps.setString(2, "IT-BIG-" + i);
                ps.setString(3, T6_PATH_PREFIX + i);
                ps.setString(4, dbJsonStringLiteral);
                ps.setLong(5, TEST_USER_ID);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void runControlFullSelect() {
        try (Statement st = physical.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM biz_page WHERE deleted=0 AND project_id="
                     + T6_PROJECT_ID + " ORDER BY updated_time DESC LIMIT " + T6_PAGE_SIZE)) {
            while (rs.next()) {
                // 强制把大字段取出（服务端本就按 SELECT * 读全列），确保对照查询包含 schema_content 的页读取
                rs.getString("schema_content");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    // ==================================================================================
    // t8：过期版本批量删除 —— 用 Com_delete 证明删除由单条 SQL 完成
    // ==================================================================================

    @Test
    @DisplayName("t8：清理过期版本时 Com_delete 增量 == 1（单条 IN(...) DELETE），回退为循环删除则为过期条数")
    void versionCleanupDeletesInSingleStatement() throws Exception {
        int maxVersionCount = 3;
        ReflectionTestUtils.setField(pageService, "maxVersionCount", maxVersionCount);

        // 页面已存在 5 个版本；schemaVersion=9 → 本次保存生成 versionNo=10（10%5==0 触发快照），
        // 快照后总版本数 = 6 > 3，excess = 3，需删除最早的 3 个版本。
        int preExistingVersions = 5;
        long pageId = insertPageWithVersions(T8_PATH_PREFIX + UUID.randomUUID(), 9, preExistingVersions);

        long before = sessionStatus("Com_delete");
        PageSchemaDTO dto = new PageSchemaDTO();
        dto.setSchema(Map.of("children", List.of()));
        pageService.saveSchema(pageId, dto);
        long after = sessionStatus("Com_delete");
        long comDeleteDelta = after - before;

        long remaining = countVersions(pageId);
        System.out.println("[t8] Com_delete delta=" + comDeleteDelta
                + " ; remainingVersions=" + remaining
                + " (expected:" + (preExistingVersions + 1 - (preExistingVersions + 1 - maxVersionCount)) + ")");

        // 单条 DELETE ... WHERE id IN (...) → Com_delete 计 1 条语句；
        // 回退为循环 deleteById 会发 3 条 DELETE → Com_delete == 3。此等值断言直接证伪回退形态。
        assertThat(comDeleteDelta)
                .as("过期版本清理必须由恰好 1 条 DELETE 语句完成（Com_delete 增量）；"
                        + "回退为循环逐条 deleteById 时该值会等于被删版本数（此处为 excess）")
                .isEqualTo(1L);

        assertThat(remaining)
                .as("清理后应仅保留 maxVersionCount=%d 个版本", maxVersionCount)
                .isEqualTo(maxVersionCount);
    }

    private long insertPageWithVersions(String pagePath, int schemaVersion, int versionCount) throws SQLException {
        long pageId;
        String pageSql = "INSERT INTO biz_page"
                + " (project_id,name,page_path,page_type,schema_content,schema_version,cover_url,status,is_home,"
                + "  user_id,created_time,updated_time,deleted)"
                + " VALUES (?,?,'" + pagePath + "','h5',NULL,?,'',0,0,?,NOW(),NOW(),0)";
        try (PreparedStatement ps = physical.prepareStatement(pageSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, T6_PROJECT_ID + 1);
            ps.setString(2, "IT-VER");
            ps.setInt(3, schemaVersion);
            ps.setLong(4, TEST_USER_ID);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                pageId = keys.getLong(1);
            }
        }
        createdPageIds.add(pageId);

        String versionSql = "INSERT INTO biz_page_version"
                + " (page_id,version_no,schema_snapshot,changelog,user_id,created_time)"
                + " VALUES (?,?,CAST('{\"children\":[]}' AS JSON),?,?,NOW())";
        try (PreparedStatement ps = physical.prepareStatement(versionSql)) {
            for (int v = 1; v <= versionCount; v++) {
                ps.setLong(1, pageId);
                ps.setInt(2, v);
                ps.setString(3, "seed");
                ps.setLong(4, TEST_USER_ID);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return pageId;
    }

    private long countVersions(long pageId) throws SQLException {
        try (Statement st = physical.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM biz_page_version WHERE page_id=" + pageId)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    // ==================================================================================
    // 通用工具
    // ==================================================================================

    private interface SqlAction {
        void run() throws Exception;
    }

    private long measureBufferPool(SqlAction action) throws Exception {
        long before = sessionStatus("Innodb_buffer_pool_read_requests");
        action.run();
        long after = sessionStatus("Innodb_buffer_pool_read_requests");
        return after - before;
    }

    private long sessionStatus(String name) throws SQLException {
        try (Statement st = physical.createStatement();
             ResultSet rs = st.executeQuery("SHOW SESSION STATUS LIKE '" + name + "'")) {
            rs.next();
            return rs.getLong(2);
        }
    }

    private static String bigJson(int bytes) {
        StringBuilder sb = new StringBuilder(bytes + 64);
        sb.append("{\"children\":[],\"pad\":\"");
        sb.append("x".repeat(bytes));
        sb.append("\"}");
        return sb.toString();
    }
}
