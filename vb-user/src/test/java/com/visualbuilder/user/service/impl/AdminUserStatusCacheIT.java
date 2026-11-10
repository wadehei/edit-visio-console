package com.visualbuilder.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.visualbuilder.common.core.constant.RedisKeyConst;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.user.mapper.SysUserMapper;
import com.visualbuilder.user.mapper.SysUserRoleMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

/**
 * B1 证据链：{@code AdminUserServiceImpl#updateStatus} 的 Redis 禁用标记「推迟到事务提交之后」。
 *
 * <h3>被测对象</h3>
 * {@code updateStatus} 更新 DB 后通过
 * {@code TransactionUtils.executeAfterCommit(...)} 注册 Redis 禁用标记的写入/删除，
 * 使其只在事务提交后落到 Redis（供 JWT Filter 判定）。被测的可证伪属性有两条：
 * <ol>
 *   <li><b>提交后才置位</b>：禁用用户时，事务提交前 Redis 键 {@code user:disabled:{userId}} 必须
 *       尚不存在，提交后才出现且值为 {@code "1"}。</li>
 *   <li><b>回滚不置位/不删除</b>：事务回滚时 Redis 键必须原样保留，且 DB 状态保持不变。</li>
 * </ol>
 *
 * <h3>真实参与</h3>
 * <ul>
 *   <li>真实 Redis（{@code 127.0.0.1:6379}）与真实 {@link StringRedisTemplate}：标记的读写/删除
 *       全部落在真服务端。</li>
 *   <li>真实 {@link AdminUserServiceImpl}（非替身），真实 {@link SysUserMapper} + 真实 MySQL
 *       （{@code 127.0.0.1:3307/visual_builder}）与真实 {@code sys_user} 表。</li>
 *   <li>真实 Spring 事务：{@link DataSourceTransactionManager} + {@link TransactionTemplate} 真正
 *       开启事务、提交/回滚；MyBatis 通过 {@link SpringManagedTransactionFactory} 使用<b>事务绑定的
 *       同一连接</b>，因此「回滚后 DB 状态不变」是被真实回滚证明的，而非推断。</li>
 *   <li>所有 SQL、事务与缓存操作共用<b>同一条物理连接</b>（{@link SingleConnectionDataSource} 包裹），
 *       保证状态连续可比。</li>
 * </ul>
 *
 * <h3>替身参与（Mockito）</h3>
 * {@link PasswordEncoder} 与 {@link SysUserRoleMapper} 为 Mockito 替身 —— 仅为满足构造参数，
 * {@code updateStatus} 不触达它们，本测试<b>不覆盖</b>密码与角色链路。
 *
 * <h3>与生产调用方式的差异（必须显式声明）</h3>
 * 本测试用 {@link TransactionTemplate} 驱动事务，而非依赖 {@code updateStatus} 上的
 * {@code @Transactional} 代理（非 Spring 上下文中该注解不生效）。这不影响被测机制：
 * {@code TransactionUtils.executeAfterCommit} 只读取 {@code TransactionSynchronizationManager}
 * 的静态状态，与事务由谁发起无关；注册的同步回调由真实的 {@link DataSourceTransactionManager}
 * 在提交/回滚时触发。
 *
 * <h3>环境差异</h3>
 * 本机 MySQL {@code 8.4.9} ≠ 生产 {@code mysql:8.0}；本机 Redis {@code 3.0.504}（Windows 移植版，EOL）
 * ≠ 生产 {@code redis:7-alpine}。被测语义仅用到事务提交/回滚与 {@code SET} / {@code GET} / {@code DEL}，
 * 这些在两侧语义一致；连接层与淘汰策略等差异<b>不在本测试覆盖范围内</b>。
 *
 * <h3>不可证伪的部分（诚实声明）</h3>
 * 旧实现（提交前直接写/删 Redis）的真实危害是一个<b>状态不一致窗口</b>：DB 回滚后 Redis 已置位
 * （用户被错误锁死）或已删除（改为放行）。本测试<b>确定性证伪</b>的是<b>机制</b>：
 * Redis 写入命令是在事务提交之前还是之后发出 —— 这由「事务内键是否存在」与「回滚后键是否仍在」
 * 两个观测点确定；而不声称复现了某次真实回滚事故。
 *
 * <h3>变异实验结果（2026-11-05 本机实测，MySQL 8.4.9 @3307 / Redis 3.0.504 @6379）</h3>
 * <p>把 {@code updateStatus} 中的 {@code TransactionUtils.executeAfterCommit(...)} 还原为
 * 提交前直接 {@code set}/{@code delete}（即改动前形态）后重跑本类，
 * <b>2 个用例全部失败</b>（2/2）；具体失败断言原文见本类文件末尾「变异实验原始输出」。</p>
 *
 * <p>MySQL 或 Redis 不可达时，全部用例判定为<b>跳过（skipped）而非通过</b>。</p>
 */
class AdminUserStatusCacheIT {

    private static final String MYSQL_URL = System.getProperty("it.mysql.url",
            "jdbc:mysql://localhost:3306/visual_builder");
    private static final String MYSQL_USER = System.getProperty("it.mysql.user", "root");
    private static final String MYSQL_PASSWORD = System.getProperty("it.mysql.password", "");

    private static final String REDIS_HOST = System.getProperty("it.redis.host", "localhost");
    private static final int REDIS_PORT = Integer.parseInt(System.getProperty("it.redis.port", "6379"));
    private static final String REDIS_PASSWORD = System.getProperty("it.redis.password", "");

    /** 播种用户名；{@code sys_user} 唯一键为 (username, deleted)，故可安全反复增删。 */
    private static final String TEST_USERNAME = "it_admin_status";
    /** 与生产默认值同量级的标记 TTL（秒）。 */
    private static final long FLAG_TTL_SECONDS = 3600L;

    private static boolean envReachable;
    private static String unreachableReason;

    private static Connection physical;
    private static SingleConnectionDataSource dataSource;
    private static SqlSession session;
    private static LettuceConnectionFactory redisConnectionFactory;
    private static StringRedisTemplate redisTemplate;

    private AdminUserServiceImpl service;
    private TransactionTemplate txTemplate;
    private long userId;
    private String disabledKey;

    @BeforeAll
    static void connect() {
        try {
            physical = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            physical.setAutoCommit(true);
            dataSource = new SingleConnectionDataSource(physical, true);

            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setEnvironment(new Environment("it-env",
                    new SpringManagedTransactionFactory(), dataSource));
            configuration.addMapper(SysUserMapper.class);
            SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
            session = factory.openSession(true);

            try (Statement st = physical.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                rs.next();
            }
        } catch (Exception e) {
            envReachable = false;
            unreachableReason = "MySQL: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            return;
        }

        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT);
            if (StringUtils.hasText(REDIS_PASSWORD)) {
                config.setPassword(RedisPassword.of(REDIS_PASSWORD));
            }
            redisConnectionFactory = new LettuceConnectionFactory(config);
            redisConnectionFactory.afterPropertiesSet();
            redisTemplate = new StringRedisTemplate(redisConnectionFactory);
            redisTemplate.afterPropertiesSet();
            String pong = redisTemplate.execute(connection -> connection.ping(), true);
            if (!"PONG".equalsIgnoreCase(pong)) {
                throw new IllegalStateException("PING 返回 " + pong);
            }
        } catch (Exception e) {
            envReachable = false;
            unreachableReason = "Redis: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            return;
        }

        envReachable = true;
    }

    @AfterAll
    static void disconnect() {
        closeQuietly(session);
        closeQuietly(physical);
        if (redisConnectionFactory != null) {
            redisConnectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(envReachable,
                "MySQL(" + MYSQL_URL + ") 或 Redis(" + REDIS_HOST + ":" + REDIS_PORT
                        + ") 不可达，跳过：" + unreachableReason + "。注意：跳过不等于通过。");

        service = new AdminUserServiceImpl(
                mock(PasswordEncoder.class), redisTemplate, mock(SysUserRoleMapper.class));
        // ServiceImpl 的 baseMapper 是 @Autowired 字段，非 Spring 上下文下需手动注入真实 Mapper
        ReflectionTestUtils.setField(service, "baseMapper", session.getMapper(SysUserMapper.class));
        // @Value 字段在非 Spring 上下文下不生效，显式注入与生产同量级的 TTL
        ReflectionTestUtils.setField(service, "disabledFlagTtlSeconds", FLAG_TTL_SECONDS);
        txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        cleanup();
        userId = insertUser(StatusConst.User.NORMAL);
        disabledKey = RedisKeyConst.userDisabled(userId);
        redisTemplate.delete(disabledKey);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (envReachable) {
            if (disabledKey != null) {
                redisTemplate.delete(disabledKey);
            }
            cleanup();
        }
    }

    // ==================================================================================
    // B1-① 提交后才置位：禁用标记被推迟到提交之后
    // ==================================================================================

    @Test
    @DisplayName("B1-① 事务提交前禁用标记尚未写入（被推迟），提交后才置为 1")
    void disabledFlagIsWrittenOnlyAfterCommit() throws Exception {
        AtomicBoolean flagPresentInsideTx = new AtomicBoolean(true);

        txTemplate.executeWithoutResult(status -> {
            service.updateStatus(userId, StatusConst.User.DISABLED);
            // 仍在事务内、尚未提交 —— 此刻标记必须还没写
            flagPresentInsideTx.set(Boolean.TRUE.equals(redisTemplate.hasKey(disabledKey)));
        });

        assertThat(flagPresentInsideTx.get())
                .as("事务提交前 Redis 禁用标记（key=%s）必须尚未写入；若此刻已存在，说明写发生在提交之前"
                        + "（事务回滚后会错误锁死用户）", disabledKey)
                .isFalse();
        assertThat(redisTemplate.opsForValue().get(disabledKey))
                .as("事务提交后 Redis 禁用标记（key=%s）必须为 \"1\"", disabledKey)
                .isEqualTo("1");
        assertThat(readDbStatus())
                .as("事务提交后 DB 状态应为 %d(禁用)，证明事务确实提交", StatusConst.User.DISABLED)
                .isEqualTo(StatusConst.User.DISABLED);
    }

    // ==================================================================================
    // B1-② 回滚不置位/不删除：标记与 DB 同时保持不变
    // ==================================================================================

    @Test
    @DisplayName("B1-② 事务回滚时禁用标记不被删除，且 DB 状态保持原样")
    void rollbackKeepsDisabledFlagAndDbUnchanged() throws Exception {
        // 造出「用户已禁用」的既有状态：DB status=0，Redis 标记=1
        updateDbStatus(StatusConst.User.DISABLED);
        redisTemplate.opsForValue().set(disabledKey, "1", Duration.ofSeconds(FLAG_TTL_SECONDS));

        txTemplate.executeWithoutResult(status -> {
            service.updateStatus(userId, StatusConst.User.NORMAL);
            status.setRollbackOnly();
        });

        assertThat(redisTemplate.opsForValue().get(disabledKey))
                .as("事务回滚时禁用标记（key=%s）不得被删除；把 Redis 操作提前到提交前的实现会在此失败",
                        disabledKey)
                .isEqualTo("1");
        assertThat(readDbStatus())
                .as("事务回滚后 DB 状态必须保持 %d(禁用)，证明回滚真实生效（否则缓存断言不成立）",
                        StatusConst.User.DISABLED)
                .isEqualTo(StatusConst.User.DISABLED);
    }

    // ==================================================================================
    // 通用工具
    // ==================================================================================

    private void cleanup() throws SQLException {
        try (Statement st = physical.createStatement()) {
            st.executeUpdate("DELETE FROM sys_user WHERE username = '" + TEST_USERNAME + "'");
        }
    }

    private long insertUser(int status) throws SQLException {
        try (PreparedStatement ps = physical.prepareStatement(
                "INSERT INTO sys_user (tenant_id, username, password, nickname, status)"
                        + " VALUES (0, ?, 'x', '', ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, TEST_USERNAME);
            ps.setInt(2, status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void updateDbStatus(int status) throws SQLException {
        try (Statement st = physical.createStatement()) {
            st.executeUpdate("UPDATE sys_user SET status = " + status + " WHERE id = " + userId);
        }
    }

    private int readDbStatus() throws SQLException {
        try (Statement st = physical.createStatement();
             ResultSet rs = st.executeQuery("SELECT status FROM sys_user WHERE id = " + userId)) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }
}

/*
 * ------------------------------------------------------------------------------------
 * 变异实验原始输出（2026-11-05，MySQL 8.4.9 @127.0.0.1:3307 / Redis 3.0.504 @127.0.0.1:6379）
 *
 * 基线（未变异）：mvn -pl vb-user -am verify "-Dit.test=AdminUserStatusCacheIT"
 *                "-Dit.mysql.url=jdbc:mysql://127.0.0.1:3307/visual_builder"
 *   Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
 *
 * 变异：AdminUserServiceImpl#updateStatus 中把
 *   TransactionUtils.executeAfterCommit(() -> { ...set/delete... });
 * 还原为提交前直接执行
 *   if (disabled) { redisTemplate.opsForValue().set(disabledKey, "1", TTL); }
 *   else { redisTemplate.delete(disabledKey); }
 * 结果：
 *   Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
 *     disabledFlagIsWrittenOnlyAfterCommit:232
 *       -- [事务提交前 Redis 禁用标记（key=user:disabled:131073）必须尚未写入；若此刻已存在，
 *           说明写发生在提交之前（事务回滚后会错误锁死用户）]
 *     rollbackKeepsDisabledFlagAndDbUnchanged:260
 *       -- [事务回滚时禁用标记（key=user:disabled:131072）不得被删除；
 *           把 Redis 操作提前到提交前的实现会在此失败]
 * 结论：两条断言对该回退形态均具鉴别力，回退即失败；回滚用例同时也验证了 DB 状态仍为 0（真实回滚生效）。
 *       还原生产代码后重跑本类恢复 2/2 绿。
 * ------------------------------------------------------------------------------------
 */
