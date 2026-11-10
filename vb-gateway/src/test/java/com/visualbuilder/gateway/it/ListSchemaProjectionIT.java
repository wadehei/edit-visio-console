package com.visualbuilder.gateway.it;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.component.dto.ComponentVO;
import com.visualbuilder.component.entity.Component;
import com.visualbuilder.component.mapper.ComponentMapper;
import com.visualbuilder.component.service.impl.ComponentServiceImpl;
import com.visualbuilder.page.entity.Page;
import com.visualbuilder.page.entity.PageVersion;
import com.visualbuilder.page.mapper.PageMapper;
import com.visualbuilder.page.mapper.PageVersionMapper;
import com.visualbuilder.page.service.impl.PageServiceImpl;
import com.visualbuilder.template.entity.Template;
import com.visualbuilder.template.mapper.TemplateMapper;
import com.visualbuilder.template.service.impl.TemplateServiceImpl;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A1 证据链：三处列表查询排除 JSON 大字段的「回退即失败」集成测试。
 *
 * <h3>被测改动</h3>
 * <ul>
 *   <li>{@code TemplateServiceImpl#listTemplates}：{@code wrapper.select(Template.class, ...)} 排除
 *       {@code biz_template.page_schema}。</li>
 *   <li>{@code ComponentServiceImpl#listComponents}：{@code wrapper.select(Component.class, ...)} 排除
 *       {@code biz_component.schema_define / default_props / default_styles}。</li>
 *   <li>{@code PageServiceImpl#getVersionList}：{@code wrapper.select(PageVersion.class, ...)} 排除
 *       {@code biz_page_version.schema_snapshot}。</li>
 * </ul>
 *
 * <h3>真实参与</h3>
 * <ul>
 *   <li>真实 MySQL 8.4.9 服务端（默认 {@code 127.0.0.1:3306/visual_builder}，本机以
 *       {@code -Dit.mysql.url=jdbc:mysql://127.0.0.1:3307/visual_builder} 覆盖）、真实 InnoDB、
 *       真实 {@code JSON} 类型与 DYNAMIC 行格式的 off-page 溢出页。</li>
 *   <li>真实 Service 实现（{@link TemplateServiceImpl}、{@link ComponentServiceImpl}、{@link PageServiceImpl}）
 *       与真实 Mapper，配真实 MyBatis-Plus {@code SqlSessionFactory}，因此被测的是生产查询路径本身。</li>
 *   <li>证据取自 MySQL 会话级状态计数器 {@code Innodb_buffer_pool_read_requests}
 *       （InnoDB 逻辑页请求数），而非对代码的阅读或对返回值大小的推断。被测查询与对照查询
 *       运行在<b>同一条物理连接</b>上（{@link SingleConnectionDataSource} 包裹单个 {@link Connection}），
 *       保证会话计数连续可比。</li>
 * </ul>
 *
 * <h3>替身参与</h3>
 * <ul>
 *   <li><b>无 Mockito 替身</b>。{@code redisTemplate} / {@code projectMemberService} / {@code pageMapper}
 *       等构造参数按「本测试不触达该路径」传入 {@code null}：
 *       模板与组件列表以非空 {@code categoryId} 调用，从而跳过 Redis 缓存分支（读与写都在
 *       {@code categoryId == null} 时发生）；版本列表的页面 {@code user_id} 即当前登录用户，
 *       故权限分支在「创建者本人」处短路，不会触达 {@code projectMemberService}。</li>
 * </ul>
 *
 * <h3>环境差异</h3>
 * 本机 MySQL 为 {@code 8.4.9}，生产镜像为 {@code mysql:8.0}。InnoDB 页大小（16KB）、DYNAMIC 行格式、
 * 大字段 off-page 存储策略与 {@code Innodb_buffer_pool_read_requests} 计数语义一致；
 * 优化器代价模型与缓冲池热状态存在版本差异，故断言采用<b>相对比较</b>
 * （被测查询 vs 全列对照查询），不依赖任何绝对页数常量。
 *
 * <h3>不可证伪的部分（诚实声明）</h3>
 * <ul>
 *   <li>本测试只观测<b>服务端 InnoDB 逻辑页请求数</b>，<b>不测量</b> JDBC 结果集字节数，
 *       因此不直接证明「客户端收到的响应体变小」——后者由 Bean 序列化路径决定，本测试只证明
 *       「这些大字段确实没被数据库读出」。</li>
 *   <li>不证明生产数据分布下的接口耗时下降幅度。</li>
 *   <li>本测试使用本测试自行播种的数据（每表 40 行 × 约 300KB JSON），
 *       结论限于「投影排除大字段即少读页」这一机制，不外推到其他表。</li>
 * </ul>
 *
 * <h3>变异实验结果（2026-11-05 本机实测，MySQL 8.4.9 @127.0.0.1:3307）</h3>
 * <p>把三处 {@code wrapper.select(...)} 各自注释掉（即回退为读取全部列）后，本类
 * <b>3 个用例全部失败</b>（3/3）；具体失败断言原文与页请求数见类末尾注释。</p>
 *
 * <p>环境不可达时全部用例判定为<b>跳过（skipped）而非通过</b>。</p>
 */
class ListSchemaProjectionIT {

    private static final String MYSQL_URL = System.getProperty("it.mysql.url",
            "jdbc:mysql://localhost:3306/visual_builder");
    private static final String MYSQL_USER = System.getProperty("it.mysql.user", "root");
    private static final String MYSQL_PASSWORD = System.getProperty("it.mysql.password", "");

    private static final long TEST_USER_ID = 700200L;
    private static final long TEMPLATE_CATEGORY_ID = 987001L;
    private static final long COMPONENT_CATEGORY_ID = 987002L;
    private static final long VERSION_PROJECT_ID = 987003L;

    /** 单行 JSON 体积：确保落入 off-page 溢出页（行格式 DYNAMIC）。 */
    private static final int BIG_JSON_BYTES = 300 * 1024;
    /** 每个列表的行数（远小于 LimitsConst.MAX_LIST_SIZE=500，故会被整表返回）。 */
    private static final int ROWS = 40;
    private static final int STATUS_ON_SHELF = 1;
    private static final int STATUS_ENABLED = 1;

    private static boolean dbReachable;
    private static String unreachableReason;
    private static Connection physical;
    private static SqlSession session;
    private static TemplateMapper templateMapper;
    private static ComponentMapper componentMapper;
    private static PageVersionMapper pageVersionMapper;
    private static PageMapper pageMapper;

    private Long versionPageId;

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
            configuration.addMapper(TemplateMapper.class);
            configuration.addMapper(ComponentMapper.class);
            configuration.addMapper(PageVersionMapper.class);
            configuration.addMapper(PageMapper.class);

            SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
            session = factory.openSession(true);
            templateMapper = session.getMapper(TemplateMapper.class);
            componentMapper = session.getMapper(ComponentMapper.class);
            pageVersionMapper = session.getMapper(PageVersionMapper.class);
            pageMapper = session.getMapper(PageMapper.class);

            // 对照查询要 filesort 含 300KB JSON 的行，默认 sort_buffer_size 不足会报
            // "Out of sort memory"；这是<b>对照侧</b>的取样需要，被测侧的投影查询本身排序量很小。
            try (Statement st = physical.createStatement()) {
                st.execute("SET SESSION sort_buffer_size = 268435456");
            }
            try (Statement st = physical.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                rs.next();
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
            if (physical != null) {
                cleanup();
                physical.close();
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(dbReachable,
                "本机 MySQL(" + MYSQL_URL + ") 不可达，跳过：" + unreachableReason + "。注意：跳过不等于通过。");
        SecurityUtils.setContext(TEST_USER_ID, "it-tester");
        cleanup();
        versionPageId = null;
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dbReachable) {
            cleanup();
        }
        SecurityUtils.clearContext();
    }

    // ==================================================================================
    // A1-① 模板列表排除 page_schema
    // ==================================================================================

    @Test
    @DisplayName("A1-①：listTemplates 排除 page_schema 后，InnoDB 逻辑页请求数显著低于全列对照")
    void listTemplatesExcludesPageSchema() throws Exception {
        seedTemplates();

        TemplateServiceImpl service = new TemplateServiceImpl(
                null, null, new ObjectMapper(), null);
        ReflectionTestUtils.setField(service, "baseMapper", templateMapper);

        List<Template> warmup = service.listTemplates(TEMPLATE_CATEGORY_ID, null, null);
        runControl("SELECT * FROM biz_template WHERE category_id = " + TEMPLATE_CATEGORY_ID
                + " AND status = " + STATUS_ON_SHELF + " AND deleted = 0 ORDER BY use_count DESC LIMIT 500",
                "page_schema");

        long actual = measure(() -> service.listTemplates(TEMPLATE_CATEGORY_ID, null, null));
        long control = measure(() -> runControl("SELECT * FROM biz_template WHERE category_id = "
                + TEMPLATE_CATEGORY_ID + " AND status = " + STATUS_ON_SHELF
                + " AND deleted = 0 ORDER BY use_count DESC LIMIT 500", "page_schema"));

        System.out.println("[A1-①] Innodb_buffer_pool_read_requests: listTemplates=" + actual
                + " controlSelectAll=" + control);

        // 断言只用布尔/数值，绝不把携带 300KB JSON 的实体打进失败信息（否则报告会膨胀到上百 MB）
        assertThat((long) warmup.size()).as("列表应取到 %d 行，否则「读得少」可能只是没读到数据", ROWS)
                .isEqualTo(ROWS);
        assertThat(warmup.stream().allMatch(t -> t.getPageSchema() == null))
                .as("排除投影下 pageSchema 应全为 null（行已读出，仅大字段未读）；为 false 说明大字段被读出（A1 已回退）")
                .isTrue();
        assertLess(actual, control, "listTemplates（排除 page_schema）", "全列对照（含 page_schema）");
    }

    // ==================================================================================
    // A1-② 组件列表排除 schema_define / default_props / default_styles
    // ==================================================================================

    @Test
    @DisplayName("A1-②：listComponents 排除 3 个 JSON 大字段后，InnoDB 逻辑页请求数显著低于全列对照")
    void listComponentsExcludesSchemaColumns() throws Exception {
        seedComponents();

        ComponentServiceImpl service = new ComponentServiceImpl(new ObjectMapper(), null);
        ReflectionTestUtils.setField(service, "baseMapper", componentMapper);

        List<ComponentVO> warmup = service.listComponents(COMPONENT_CATEGORY_ID, null);
        runControl("SELECT * FROM biz_component WHERE category_id = " + COMPONENT_CATEGORY_ID
                + " AND status = " + STATUS_ENABLED + " AND deleted = 0 ORDER BY sort_order ASC LIMIT 500",
                "schema_define", "default_props", "default_styles");

        long actual = measure(() -> service.listComponents(COMPONENT_CATEGORY_ID, null));
        long control = measure(() -> runControl("SELECT * FROM biz_component WHERE category_id = "
                + COMPONENT_CATEGORY_ID + " AND status = " + STATUS_ENABLED
                + " AND deleted = 0 ORDER BY sort_order ASC LIMIT 500",
                "schema_define", "default_props", "default_styles"));

        System.out.println("[A1-②] Innodb_buffer_pool_read_requests: listComponents=" + actual
                + " controlSelectAll=" + control);

        assertThat((long) warmup.size()).as("列表应取到 %d 行，否则「读得少」可能只是没读到数据", ROWS)
                .isEqualTo(ROWS);
        assertThat(warmup.stream().allMatch(vo -> vo.getPropsSchema() == null
                        && vo.getDefaultProps() == null
                        && vo.getDefaultStyles() == null))
                .as("排除投影下三个 JSON 字段应全为 null（行已读出，仅大字段未读）；为 false 说明大字段被读出（A1 已回退）")
                .isTrue();
        assertLess(actual, control, "listComponents（排除 3 个大字段）", "全列对照（含 3 个大字段）");
    }

    // ==================================================================================
    // A1-③ 版本列表排除 schema_snapshot
    // ==================================================================================

    @Test
    @DisplayName("A1-③：getVersionList 排除 schema_snapshot 后，InnoDB 逻辑页请求数显著低于全列对照")
    void getVersionListExcludesSchemaSnapshot() throws Exception {
        seedVersions();

        PageServiceImpl service = new PageServiceImpl(pageVersionMapper, new ObjectMapper(), null);
        ReflectionTestUtils.setField(service, "baseMapper", pageMapper);

        List<PageVersion> warmup = service.getVersionList(versionPageId);
        runControl("SELECT * FROM biz_page_version WHERE page_id = " + versionPageId
                + " ORDER BY version_no DESC", "schema_snapshot");

        long actual = measure(() -> service.getVersionList(versionPageId));
        long control = measure(() -> runControl("SELECT * FROM biz_page_version WHERE page_id = "
                + versionPageId + " ORDER BY version_no DESC", "schema_snapshot"));

        System.out.println("[A1-③] Innodb_buffer_pool_read_requests: getVersionList=" + actual
                + " controlSelectAll=" + control);

        assertThat((long) warmup.size()).as("版本列表应取到 %d 行，否则「读得少」可能只是没读到数据", ROWS)
                .isEqualTo(ROWS);
        assertThat(warmup.stream().allMatch(v -> v.getSchemaSnapshot() == null))
                .as("排除投影下 schemaSnapshot 应全为 null（行已读出，仅大字段未读）；为 false 说明大字段被读出（A1 已回退）")
                .isTrue();
        assertLess(actual, control, "getVersionList（排除 schema_snapshot）", "全列对照（含 schema_snapshot）");
    }

    /**
     * 公共断言：被测查询的读取量必须显著低于全列对照——「小于」之外再要求「不足其一半」，
     * 以留出足够鉴别余量（回退形态与被测形态用同一 SQL，仅列清单不同）。
     */
    private void assertLess(long actual, long control, String subject, String reference) {
        assertThat(actual)
                .as("%s 逻辑页请求数=%d 应显著小于%s=%d；若接近或超过对照，说明大字段未被排除（A1 已回退）",
                        subject, actual, reference, control)
                .isLessThan(control);
        assertThat(actual * 2)
                .as("%s 逻辑页请求数=%d 应不足%s=%d 的一半（留出足够鉴别余量）",
                        subject, actual, reference, control)
                .isLessThan(control);
    }

    // ==================================================================================
    // 播种 / 清理 / 测量
    // ==================================================================================

    private void seedTemplates() throws Exception {
        String json = bigJson();
        String dbJsonLiteral = new ObjectMapper().writeValueAsString(json);
        String sql = "INSERT INTO biz_template"
                + " (category_id,name,description,cover_url,page_schema,tags,use_count,favorite_count,"
                + "  is_official,user_id,status,deleted)"
                + " VALUES (?,?,?,'',CAST(? AS JSON),'',0,0,0,?," + STATUS_ON_SHELF + ",0)";
        try (PreparedStatement ps = physical.prepareStatement(sql)) {
            for (int i = 0; i < ROWS; i++) {
                ps.setLong(1, TEMPLATE_CATEGORY_ID);
                ps.setString(2, "IT-TPL-" + i);
                ps.setString(3, "it");
                ps.setString(4, dbJsonLiteral);
                ps.setLong(5, TEST_USER_ID);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedComponents() throws Exception {
        String json = bigJson();
        String dbJsonLiteral = new ObjectMapper().writeValueAsString(json);
        String sql = "INSERT INTO biz_component"
                + " (category_id,type,name,description,thumbnail,schema_define,default_props,default_styles,"
                + "  is_system,user_id,sort_order,status,deleted)"
                + " VALUES (?,'it',?,'','',CAST(? AS JSON),CAST(? AS JSON),CAST(? AS JSON),0,?,0,"
                + STATUS_ENABLED + ",0)";
        try (PreparedStatement ps = physical.prepareStatement(sql)) {
            for (int i = 0; i < ROWS; i++) {
                ps.setLong(1, COMPONENT_CATEGORY_ID);
                ps.setString(2, "IT-CMP-" + i);
                ps.setString(3, dbJsonLiteral);
                ps.setString(4, dbJsonLiteral);
                ps.setString(5, dbJsonLiteral);
                ps.setLong(6, TEST_USER_ID);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedVersions() throws Exception {
        String pagePath = "/it-schema-projection-" + System.nanoTime();
        String pageSql = "INSERT INTO biz_page"
                + " (project_id,name,page_path,page_type,schema_content,schema_version,cover_url,status,is_home,"
                + "  user_id,deleted) VALUES (?,'IT-PAGE',?,'h5',NULL,0,'',0,0,?,0)";
        try (PreparedStatement ps = physical.prepareStatement(pageSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, VERSION_PROJECT_ID);
            ps.setString(2, pagePath);
            ps.setLong(3, TEST_USER_ID);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                versionPageId = keys.getLong(1);
            }
        }

        String json = bigJson();
        String dbJsonLiteral = new ObjectMapper().writeValueAsString(json);
        String versionSql = "INSERT INTO biz_page_version"
                + " (page_id,version_no,schema_snapshot,changelog,user_id)"
                + " VALUES (?,?,CAST(? AS JSON),'it',?)";
        try (PreparedStatement ps = physical.prepareStatement(versionSql)) {
            for (int i = 1; i <= ROWS; i++) {
                ps.setLong(1, versionPageId);
                ps.setInt(2, i);
                ps.setString(3, dbJsonLiteral);
                ps.setLong(4, TEST_USER_ID);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void cleanup() throws SQLException {
        try (Statement st = physical.createStatement()) {
            st.executeUpdate("DELETE FROM biz_template WHERE category_id = " + TEMPLATE_CATEGORY_ID);
            st.executeUpdate("DELETE FROM biz_component WHERE category_id = " + COMPONENT_CATEGORY_ID);
            st.executeUpdate("DELETE FROM biz_page_version WHERE page_id IN"
                    + " (SELECT id FROM biz_page WHERE project_id = " + VERSION_PROJECT_ID + ")");
            st.executeUpdate("DELETE FROM biz_page WHERE project_id = " + VERSION_PROJECT_ID);
        }
    }

    private interface Action {
        void run() throws Exception;
    }

    private long measure(Action action) throws Exception {
        long before = counter();
        action.run();
        return counter() - before;
    }

    private long counter() throws SQLException {
        try (Statement st = physical.createStatement();
             ResultSet rs = st.executeQuery("SHOW SESSION STATUS LIKE 'Innodb_buffer_pool_read_requests'")) {
            rs.next();
            return rs.getLong(2);
        }
    }

    /** 对照查询：读取<b>全部列</b>（含大字段），模拟「未排除大字段」的回退形态。 */
    private void runControl(String sql, String... bigColumns) {
        try (Statement st = physical.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                for (String column : bigColumns) {
                    rs.getString(column);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bigJson() {
        StringBuilder sb = new StringBuilder(BIG_JSON_BYTES + 64);
        sb.append("{\"children\":[],\"pad\":\"");
        sb.append("x".repeat(BIG_JSON_BYTES));
        sb.append("\"}");
        return sb.toString();
    }
}

/*
 * ==========================================================================================
 * 基线 / 变异实验结果（2026-11-05 本机实测，MySQL 8.4.9 @127.0.0.1:3307）
 * ==========================================================================================
 * 命令：
 *   mvn -o -pl vb-gateway -am verify "-Dit.test=ListSchemaProjectionIT"
 *       "-Dfailsafe.failIfNoSpecifiedTests=false"
 *       "-Dit.mysql.url=jdbc:mysql://127.0.0.1:3307/visual_builder"
 *
 * 基线（A1 生效）：Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
 *   · A1-① listTemplates  : Innodb_buffer_pool_read_requests = 12    （全列对照 = 845）
 *   · A1-② listComponents : 86                                        （全列对照 = 2485）
 *   · A1-③ getVersionList : 85                                        （全列对照 = 883）
 *   三处均满足「不足对照的一半」，且排除的大字段在各行均为 null（行已读出、仅大字段未读）。
 *
 * 变异（把 TemplateServiceImpl / ComponentServiceImpl / PageServiceImpl 中三处
 *       wrapper.select(...) 各自注释掉，即回退为读取全部列）：
 *   Tests run: 3, Failures: 3, Errors: 0, Skipped: 0   —— 3/3 全部失败
 *   · listTemplatesExcludesPageSchema
 *       Innodb_buffer_pool_read_requests = 885（对照 885，差值为 0）
 *       [排除投影下 pageSchema 应全为 null…] Expecting value to be true but was false
 *   · listComponentsExcludesSchemaColumns
 *       Innodb_buffer_pool_read_requests = 2485（对照 2485，差值为 0）
 *       [排除投影下三个 JSON 字段应全为 null…] Expecting value to be true but was false
 *   · getVersionListExcludesSchemaSnapshot
 *       Innodb_buffer_pool_read_requests = 884（对照 883，被测甚至略高）
 *       [排除投影下 schemaSnapshot 应全为 null…] Expecting value to be true but was false
 *
 * 变异后已还原三处生产代码，与 IndexGovernanceIT 合并重跑基线：
 *   Tests run: 9, Failures: 0, Errors: 0, Skipped: 0（BUILD SUCCESS）。
 *
 * 注：对照查询需对含 300KB JSON 的行做 filesort，故本类在连接建立后设置
 *   SET SESSION sort_buffer_size = 268435456（否则 MySQL 报 Out of sort memory）。
 *   该设置只作用于<b>对照侧取样</b>，不影响被测查询的页请求数读数。
 * 注：断言刻意只用布尔/数值，不把携带 300KB JSON 的实体写进失败信息——否则 AssertJ 会
 *   把整个实体列表 dump 进报告（实测会产生上百 MB 的 surefire 报告）。
 * ==========================================================================================
 */
