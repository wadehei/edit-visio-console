package com.visualbuilder.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.system.mapper.LoginLogMapper;
import com.visualbuilder.system.mapper.SysConfigMapper;
import com.visualbuilder.system.mapper.SysLogMapper;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

/**
 * t3 证据链：{@code SystemServiceImpl#updateConfig} 的「缓存失效推迟到事务提交之后」。
 *
 * <h3>被测对象</h3>
 * {@code updateConfig} 更新 DB 后通过 {@code TransactionUtils.executeAfterCommit(() -> redisTemplate.delete(CONFIG_CACHE_KEY))}
 * 注册缓存清理，使其只在事务提交后执行。被测的可证伪属性有两条：
 * <ol>
 *   <li><b>提交后清空</b>：事务提交时，缓存键 {@code system:configs:all} 必须消失。</li>
 *   <li><b>回滚不清空</b>：事务回滚时，缓存键必须原样保留，且 DB 值保持不变。</li>
 * </ol>
 *
 * <h3>真实参与</h3>
 * <ul>
 *   <li>真实 Redis（{@code 127.0.0.1:6379}）与真实 {@link StringRedisTemplate}：缓存的读写/删除全部落在真服务端。</li>
 *   <li>真实 {@link SystemServiceImpl}（非替身），真实 {@link SysConfigMapper} + 真实 MySQL
 *       （{@code 127.0.0.1:3307/visual_builder}）与真实 {@code sys_config} 表。</li>
 *   <li>真实 Spring 事务：{@link DataSourceTransactionManager} + {@link TransactionTemplate} 真正开启事务、提交/回滚，
 *       MyBatis 通过 {@link SpringManagedTransactionFactory} 使用<b>事务绑定的同一连接</b>，
 *       因此「回滚后 DB 值不变」是被真实回滚证明的，而非推断。</li>
 *   <li>所有 SQL、事务与缓存操作共用<b>同一条物理连接</b>（{@link SingleConnectionDataSource} 包裹），保证状态连续可比。</li>
 * </ul>
 *
 * <h3>替身参与（Mockito）</h3>
 * {@link SysLogMapper} / {@link LoginLogMapper} 为 Mockito 替身 —— 仅为满足构造参数，
 * {@code updateConfig} 不触达它们，本测试<b>不覆盖</b>日志链路。
 *
 * <h3>与生产调用方式的差异（必须显式声明）</h3>
 * 本测试用 {@link TransactionTemplate} 驱动事务，而非依赖 {@code updateConfig} 上的
 * {@code @Transactional} 代理（非 Spring 上下文中该注解不生效）。
 * 这不影响被测机制：{@code TransactionUtils.executeAfterCommit} 只读取
 * {@code TransactionSynchronizationManager} 的静态状态，与事务由谁发起无关；
 * 注册的同步回调由真实的 {@link DataSourceTransactionManager} 在提交/回滚时触发。
 *
 * <h3>环境差异</h3>
 * 本机 MySQL {@code 8.4.9} ≠ 生产 {@code mysql:8.0}；本机 Redis {@code 3.0.504}（Windows 移植版，EOL）
 * ≠ 生产 {@code redis:7-alpine}。被测语义仅用到事务提交/回滚与 {@code DEL}、{@code EXISTS}，
 * 这些在两侧语义一致；连接层与淘汰策略等差异<b>不在本测试覆盖范围内</b>。
 *
 * <h3>不可证伪的部分（诚实声明）</h3>
 * 旧实现（提交前直接删除缓存）的真实危害是一个<b>竞态窗口</b>：删除与提交之间，
 * 并发读者可能用旧值重新填充缓存，随后提交使该旧值长期驻留。该竞态<b>无法确定性复现</b>，
 * 因此本测试<b>不声称</b>证明了该竞态。可被确定性证伪的是<b>机制</b>：
 * 缓存删除命令是在事务提交之前还是之后发出 —— 这由「事务内缓存键是否仍存在」与
 * 「回滚后缓存键是否仍在」两个观测点确定。
 *
 * <h3>变异实验结果（2026-11-05 本机实测，MySQL 8.4.9 @3307 / Redis 3.0.504 @6379）</h3>
 * 把 {@code SystemServiceImpl#updateConfig} 中的
 * {@code TransactionUtils.executeAfterCommit(() -> redisTemplate.delete(CONFIG_CACHE_KEY))}
 * 临时改为直接 {@code redisTemplate.delete(CONFIG_CACHE_KEY)}（提交前即删除）后重跑本类：
 * <pre>
 * （见本类文件末尾「变异实验原始输出」）
 * </pre>
 * 两条用例对「回退形态」的鉴别力是互补的：改为提交前直接删除会同时打破
 * <b>提交前缓存仍存在</b>与<b>回滚后缓存仍存在</b>两个断言；
 * 而若仅把回调挂到 {@code afterCompletion}（提交与回滚都会触发），则只有第二个断言被打破。
 *
 * <p>MySQL 或 Redis 不可达时，全部用例判定为<b>跳过（skipped）而非通过</b>。</p>
 */
class ConfigCacheInvalidationIT {

    private static final String MYSQL_URL = System.getProperty("it.mysql.url",
            "jdbc:mysql://localhost:3306/visual_builder");
    private static final String MYSQL_USER = System.getProperty("it.mysql.user", "root");
    private static final String MYSQL_PASSWORD = System.getProperty("it.mysql.password", "");

    private static final String REDIS_HOST = System.getProperty("it.redis.host", "localhost");
    private static final int REDIS_PORT = Integer.parseInt(System.getProperty("it.redis.port", "6379"));
    private static final String REDIS_PASSWORD = System.getProperty("it.redis.password", "");

    /** 与 {@code SystemServiceImpl#CONFIG_CACHE_KEY} 的契约一致：服务端若改用其它键名，本测试会失败发现。 */
    private static final String CACHE_KEY = "system:configs:all";
    private static final String CONFIG_KEY = "it.cache.invalidation.key";
    private static final String OLD_VALUE = "OLD";
    private static final String NEW_VALUE = "NEW";

    private static boolean envReachable;
    private static String unreachableReason;

    private static Connection physical;
    private static SingleConnectionDataSource dataSource;
    private static SqlSessionFactory sqlSessionFactory;
    private static SqlSession session;
    private static LettuceConnectionFactory redisConnectionFactory;
    private static StringRedisTemplate redisTemplate;

    private SystemServiceImpl systemService;
    private TransactionTemplate txTemplate;

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
            configuration.addMapper(SysConfigMapper.class);
            sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
            session = sqlSessionFactory.openSession(true);

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

        systemService = new SystemServiceImpl(
                mock(SysLogMapper.class), mock(LoginLogMapper.class), redisTemplate, new ObjectMapper());
        // ServiceImpl 的 baseMapper 是 @Autowired 字段，非 Spring 上下文下需手动注入真实 Mapper
        ReflectionTestUtils.setField(systemService, "baseMapper", session.getMapper(SysConfigMapper.class));
        txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        cleanup();
        insertConfigRow(OLD_VALUE);
        redisTemplate.opsForValue().set(CACHE_KEY, "[\"stale\"]");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (envReachable) {
            cleanup();
        }
    }

    // ==================================================================================
    // t3-① 提交后清空：失效被推迟到提交之后
    // ==================================================================================

    @Test
    @DisplayName("t3-① 事务提交前缓存仍存在（失效被推迟），提交后才被清除")
    void invalidationIsDeferredUntilAfterCommit() throws Exception {
        AtomicBoolean cachePresentInsideTx = new AtomicBoolean();

        txTemplate.executeWithoutResult(status -> {
            systemService.updateConfig(CONFIG_KEY, NEW_VALUE);
            // 仍在事务内、尚未提交 —— 此刻缓存必须还在
            cachePresentInsideTx.set(Boolean.TRUE.equals(redisTemplate.hasKey(CACHE_KEY)));
        });

        assertThat(cachePresentInsideTx.get())
                .as("事务提交前缓存（key=%s）必须仍然存在；若此刻已消失，说明删除发生在提交之前（可能已回退）",
                        CACHE_KEY)
                .isTrue();
        assertThat(redisTemplate.hasKey(CACHE_KEY))
                .as("事务提交后缓存（key=%s）必须被清除", CACHE_KEY)
                .isFalse();
        assertThat(readDbValue())
                .as("事务提交后 DB 值应为新值，证明事务确实提交")
                .isEqualTo(NEW_VALUE);
    }

    // ==================================================================================
    // t3-② 回滚不清空：缓存与 DB 同时保持不变
    // ==================================================================================

    @Test
    @DisplayName("t3-② 事务回滚时缓存不被清除，且 DB 值保持原样")
    void rollbackKeepsCacheAndDbUnchanged() throws Exception {
        txTemplate.executeWithoutResult(status -> {
            systemService.updateConfig(CONFIG_KEY, NEW_VALUE);
            status.setRollbackOnly();
        });

        assertThat(redisTemplate.hasKey(CACHE_KEY))
                .as("事务回滚时缓存（key=%s）不得被清除；提交前直接删除或挂到 afterCompletion 的实现会在此失败",
                        CACHE_KEY)
                .isTrue();
        assertThat(readDbValue())
                .as("事务回滚后 DB 值必须保持 %s，证明回滚真实生效（否则缓存断言不成立）", OLD_VALUE)
                .isEqualTo(OLD_VALUE);
    }

    // ==================================================================================
    // 通用工具
    // ==================================================================================

    private void cleanup() throws SQLException {
        try (Statement st = physical.createStatement()) {
            st.executeUpdate("DELETE FROM sys_config WHERE config_key = '" + CONFIG_KEY + "'");
        }
        redisTemplate.delete(CACHE_KEY);
    }

    private void insertConfigRow(String value) throws SQLException {
        try (PreparedStatement ps = physical.prepareStatement(
                "INSERT INTO sys_config (config_key, config_value, description, updated_by, updated_time)"
                        + " VALUES (?, ?, '', 0, NOW())")) {
            ps.setString(1, CONFIG_KEY);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    private String readDbValue() throws SQLException {
        try (Statement st = physical.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT config_value FROM sys_config WHERE config_key = '" + CONFIG_KEY + "'")) {
            return rs.next() ? rs.getString(1) : null;
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
 * 基线（未变异）：mvn -pl vb-system -am verify "-Dit.mysql.url=jdbc:mysql://127.0.0.1:3307/visual_builder"
 *   Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
 *
 * 变异：SystemServiceImpl#updateConfig 中把
 *   TransactionUtils.executeAfterCommit(() -> redisTemplate.delete(CONFIG_CACHE_KEY));
 * 改为
 *   redisTemplate.delete(CONFIG_CACHE_KEY);
 * 结果：
 *   Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
 *     invalidationIsDeferredUntilAfterCommit:227 -- [事务提交前缓存（key=system:configs:all）必须仍然存在；
 *         若此刻已消失，说明删除发生在提交之前（可能已回退）]
 *     rollbackKeepsCacheAndDbUnchanged:251      -- [事务回滚时缓存（key=system:configs:all）不得被清除；
 *         提交前直接删除或挂到 afterCompletion 的实现会在此失败]
 * 结论：两项改动均被捕获，回退即失败；回滚用例同时也验证了 DB 值仍为 OLD（真实回滚生效）。
 * ------------------------------------------------------------------------------------
 */
