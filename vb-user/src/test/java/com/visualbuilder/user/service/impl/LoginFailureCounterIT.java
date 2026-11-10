package com.visualbuilder.user.service.impl;

import com.visualbuilder.common.core.constant.RedisKeyConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.security.jwt.JwtUtils;
import com.visualbuilder.user.dto.LoginDTO;
import com.visualbuilder.user.entity.SysUser;
import com.visualbuilder.user.mapper.SysRoleMapper;
import com.visualbuilder.user.mapper.SysUserMapper;
import com.visualbuilder.user.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * t5 证据链：登录失败计数在<b>真实 Redis</b> 上的行为验证。
 *
 * <h3>被测对象与替身边界（必须显式记录，避免被误读为端到端证明）</h3>
 * <ul>
 *   <li>真实参与：Redis 服务端、Lua 脚本执行、键名与 TTL 语义。</li>
 *   <li>替身参与：所有 Mapper、PasswordEncoder、JwtUtils 均为 Mockito 替身——本测试
 *       <b>不经过 MySQL</b>，因此不证明数据库层行为。</li>
 * </ul>
 *
 * <h3>环境差异声明</h3>
 * 本机可用 Redis 为 {@code 3.0.504}（Windows 移植版，已 EOL），生产为 {@code redis:7-alpine}。
 * 被测脚本仅使用 {@code INCR} / {@code EXPIRE}（自 Redis 2.6 起语义稳定），故脚本语义可迁移；
 * 但连接层、淘汰策略、脚本缓存等与 7.x 的差异<b>不在本测试覆盖范围内</b>。
 *
 * <h3>可证伪性验证（变异实验，2026-11-05 实测）</h3>
 * 把 {@code SysUserServiceImpl#recordLoginFailure} 临时还原为 t5 之前的实现
 * （{@code increment(...)} 后紧跟 {@code expire(...)}，两次独立往返）后重跑本类：
 * <pre>
 * Tests run: 7, Failures: 1
 *   countingAndTtlHappenInsideOneServerSideScript -- 期望顶层命令 1 条，实际 2 条：
 *   [0 127.0.0.1:64417] "INCR"    "login:fail:..."
 *   [0 127.0.0.1:64417] "PEXPIRE" "login:fail:..." "900000"
 * </pre>
 * 结论有两面，都必须记录：
 * <ol>
 *   <li>新用例确实构成证伪链——回退即失败，且失败信息直接给出服务端原始命令流。</li>
 *   <li>其余 6 个用例<b>全部照常通过</b>，说明它们在 t5 上<b>不具鉴别力</b>。
 *       仅有"测试通过"不构成证据，唯有变异实验才能暴露这一点。</li>
 * </ol>
 *
 * <h3>不可证伪的部分（诚实声明）</h3>
 * 旧实现的真实危害是 {@code INCR} 与 {@code EXPIRE} 之间存在亚毫秒竞态窗口，
 * 该窗口<b>无法确定性复现</b>，因此本类<b>不声称</b>"证明旧实现会丢 TTL"。
 * 可被确定性证伪的是<b>机制</b>：计数与 TTL 是否落在同一条服务端脚本内。
 * 证据来源为 Redis 自身的 MONITOR 命令流（服务端用 {@code [0 lua]} 标记脚本内部命令），
 * 而非对代码的阅读或推断。
 *
 * <p>Redis 不可达时，全部用例判定为<b>跳过</b>而非通过。</p>
 */
class LoginFailureCounterIT {

    private static final String REDIS_HOST = System.getProperty("it.redis.host", "localhost");
    private static final int REDIS_PORT = Integer.parseInt(System.getProperty("it.redis.port", "6379"));
    private static final String REDIS_PASSWORD = System.getProperty("it.redis.password", "");
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_SECONDS = 15 * 60;

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static boolean redisReachable;
    private static String unreachableReason;

    private final List<String> createdKeys = new ArrayList<>();
    private String loginKey;
    private SysUserMapper userMapper;
    private SysUserServiceImpl service;

    @BeforeAll
    static void connectToRedis() {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT);
            if (StringUtils.hasText(REDIS_PASSWORD)) {
                config.setPassword(RedisPassword.of(REDIS_PASSWORD));
            }
            connectionFactory = new LettuceConnectionFactory(config);
            connectionFactory.afterPropertiesSet();
            redisTemplate = new StringRedisTemplate(connectionFactory);
            redisTemplate.afterPropertiesSet();

            String pong = redisTemplate.execute(connection -> connection.ping(), true);
            redisReachable = "PONG".equalsIgnoreCase(pong);
            if (!redisReachable) {
                unreachableReason = "PING 返回 " + pong;
            }
        } catch (Exception e) {
            redisReachable = false;
            unreachableReason = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        assumeTrue(redisReachable,
                "本机 Redis(" + REDIS_HOST + ":" + REDIS_PORT + ") 不可达，跳过："
                        + unreachableReason + "。注意：跳过不等于通过。");

        loginKey = "it_login_" + UUID.randomUUID();
        createdKeys.add(RedisKeyConst.loginFail(loginKey));
        createdKeys.add(RedisKeyConst.loginLock(loginKey));
        cleanupKeys();

        userMapper = mock(SysUserMapper.class);
        service = new SysUserServiceImpl(
                mock(PasswordEncoder.class),
                mock(JwtUtils.class),
                mock(SysUserRoleMapper.class),
                mock(SysRoleMapper.class),
                redisTemplate);
        // ServiceImpl 的 baseMapper 是 @Autowired 字段，非 Spring 上下文下需手动注入
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
    }

    @AfterEach
    void tearDown() {
        cleanupKeys();
    }

    private void cleanupKeys() {
        for (String key : createdKeys) {
            redisTemplate.delete(key);
        }
    }

    private LoginDTO wrongPasswordLogin() {
        LoginDTO dto = new LoginDTO();
        dto.setLogin(loginKey);
        dto.setPassword("definitely-wrong");
        return dto;
    }

    private void failedLogin() {
        assertThatThrownBy(() -> service.login(wrongPasswordLogin()))
                .isInstanceOf(BusinessException.class);
    }

    private String failKey() {
        return RedisKeyConst.loginFail(loginKey);
    }

    private String lockKey() {
        return RedisKeyConst.loginLock(loginKey);
    }

    @Test
    @DisplayName("并发失败登录时计数精确累计，不因竞态丢失")
    void concurrentFailuresAreCountedExactly() throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        failedLogin();
                    } catch (Throwable ignored) {
                        // 计数断言由下方统一完成
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS))
                    .as("并发登录未在 30s 内全部返回")
                    .isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(redisTemplate.opsForValue().get(failKey()))
                .as("20 次并发失败必须精确累计为 20；任何读-改-写式实现都会在此失败")
                .isEqualTo(String.valueOf(threads));
    }

    @Test
    @DisplayName("首次失败即写入 TTL，避免计数永不过期")
    void firstFailureSetsTtl() {
        failedLogin();

        Long ttl = redisTemplate.getExpire(failKey());
        assertThat(ttl).as("首次失败后必须带上过期时间").isNotNull().isGreaterThan(0L);
        assertThat(ttl).as("TTL 不得超过锁定窗口").isLessThanOrEqualTo(LOCK_SECONDS);
    }

    @Test
    @DisplayName("后续失败不刷新 TTL（否则暴力破解可无限续期）")
    void subsequentFailuresDoNotRefreshTtl() {
        failedLogin();
        // 人为把 TTL 压到 100s，用于确定性地区分"仅首次设置"与"每次都续期"
        redisTemplate.expire(failKey(), java.time.Duration.ofSeconds(100));

        failedLogin();
        failedLogin();

        Long ttl = redisTemplate.getExpire(failKey());
        assertThat(ttl).as("TTL 被重新设置为 %d 秒，说明每次失败都在续期", LOCK_SECONDS)
                .isNotNull()
                .isLessThanOrEqualTo(100L);
        assertThat(redisTemplate.opsForValue().get(failKey())).isEqualTo("3");
    }

    @Test
    @DisplayName("恰好第 5 次失败触发锁定，第 4 次不触发")
    void lockTriggersExactlyAtThreshold() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            failedLogin();
        }
        assertThat(redisTemplate.hasKey(lockKey()))
                .as("第 %d 次失败不应锁定", MAX_ATTEMPTS - 1)
                .isFalse();

        failedLogin();
        assertThat(redisTemplate.hasKey(lockKey()))
                .as("第 %d 次失败必须锁定", MAX_ATTEMPTS)
                .isTrue();
        Long lockTtl = redisTemplate.getExpire(lockKey());
        assertThat(lockTtl).isNotNull().isGreaterThan(0L).isLessThanOrEqualTo(LOCK_SECONDS);
    }

    @Test
    @DisplayName("锁定后再次登录被直接拒绝，且失败计数不再累加")
    void lockedAccountIsRejectedWithoutFurtherCounting() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            failedLogin();
        }
        String countAtLock = redisTemplate.opsForValue().get(failKey());

        assertThatThrownBy(() -> service.login(wrongPasswordLogin()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分钟");

        assertThat(redisTemplate.opsForValue().get(failKey()))
                .as("锁定后不应继续累加计数")
                .isEqualTo(countAtLock);
    }

    @Test
    @DisplayName("登录成功后失败计数被清除")
    void successfulLoginClearsFailureCounter() {
        failedLogin();
        failedLogin();
        assertThat(redisTemplate.opsForValue().get(failKey())).isEqualTo("2");

        SysUser user = new SysUser();
        user.setId(1001L);
        user.setUsername(loginKey);
        user.setPassword("stored-hash");
        user.setStatus(com.visualbuilder.common.core.constant.StatusConst.User.NORMAL);

        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.matches(any(), any())).thenReturn(true);
        JwtUtils jwt = mock(JwtUtils.class);
        when(jwt.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwt.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwt.getAccessTokenExpire()).thenReturn(1800L);

        // 实证（Mockito printInvocations）：MyBatis-Plus 3.5.7 的 ServiceImpl#getOne(Wrapper)
        // 实际调用的是双参重载 baseMapper.selectOne(wrapper, true)，而非单参重载。
        // 因此两个重载都需打桩，否则替身返回 null、登录会误走"用户不存在"分支。
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(user);
        when(userMapper.selectList(any())).thenReturn(new ArrayList<>(Collections.singletonList(user)));
        when(userMapper.selectRoleKeysByUserId(any())).thenReturn(new ArrayList<>(Collections.singletonList("USER")));
        when(userMapper.selectPermissionKeysByUserId(any())).thenReturn(new ArrayList<>());

        SysUserServiceImpl successService = new SysUserServiceImpl(
                encoder, jwt, mock(SysUserRoleMapper.class), mock(SysRoleMapper.class), redisTemplate);
        ReflectionTestUtils.setField(successService, "baseMapper", userMapper);

        LoginDTO dto = new LoginDTO();
        dto.setLogin(loginKey);
        dto.setPassword("correct");
        try {
            successService.login(dto);
        } catch (BusinessException e) {
            throw new AssertionError("期望命中登录成功分支，实际抛出 " + e.getMessage()
                    + "；mapper 实际调用 = " + Mockito.mockingDetails(userMapper).printInvocations(), e);
        }

        assertThat(redisTemplate.hasKey(failKey()))
                .as("登录成功后必须清除失败计数")
                .isFalse();
    }

    // ===================== 服务端命令流（原始材料）探针 =====================

    private final List<String> monitoredCommands = new CopyOnWriteArrayList<>();
    private Socket monitorSocket;
    private Thread monitorThread;

    /** 直连 Redis 发送 MONITOR，采集服务端自己的命令流水，作为不依赖代码判断的原始证据。 */
    private void startMonitor() throws Exception {
        monitorSocket = new Socket(REDIS_HOST, REDIS_PORT);
        monitorSocket.setTcpNoDelay(true);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(monitorSocket.getInputStream(), StandardCharsets.UTF_8));
        OutputStream out = monitorSocket.getOutputStream();
        out.write("MONITOR\r\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
        assertThat(reader.readLine()).as("MONITOR 指令未被 Redis 接受").isEqualTo("+OK");
        monitorThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    monitoredCommands.add(line);
                }
            } catch (Exception ignored) {
                // 关闭 socket 触发的正常退出
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void stopMonitor() {
        try {
            if (monitorSocket != null) {
                monitorSocket.close();
            }
        } catch (Exception ignored) {
            // ignore
        }
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }

    @Test
    @DisplayName("计数与 TTL 由服务端单条脚本原子完成（顶层命令恰好 1 条 EVALSHA）")
    void countingAndTtlHappenInsideOneServerSideScript() throws Exception {
        startMonitor();
        try {
            Thread.sleep(150);
            failedLogin();
            awaitMonitoredLine(2000);
        } finally {
            stopMonitor();
        }

        List<String> hits = monitoredCommands.stream()
                .filter(l -> l.contains(failKey()))
                .collect(Collectors.toList());

        // Redis 服务端在 MONITOR 流里用客户端标识区分命令来源：
        //   [0 127.0.0.1:xxxxx] "INCR"   —— 客户端直接发起的顶层命令（两次往返 = 非原子）
        //   [0 lua]             "INCR"   —— 脚本内部调用（与 EVAL 同批执行 = 原子）
        List<String> topLevel = hits.stream()
                .filter(l -> !l.contains("[0 lua]"))
                .collect(Collectors.toList());
        List<String> fromScript = hits.stream()
                .filter(l -> l.contains("[0 lua]"))
                .collect(Collectors.toList());

        assertThat(topLevel)
                .as("针对失败计数键的顶层命令必须只有 1 条（脚本本身）；"
                        + "若出现 2 条，说明回退成了 INCR/EXPIRE 两次独立往返。实际命令流 = %s", hits)
                .hasSize(1);
        assertThat(commandOf(topLevel.get(0)))
                .as("计数与 TTL 必须由服务端脚本完成，实际顶层命令 = %s", topLevel.get(0))
                .isIn("EVAL", "EVALSHA");

        assertThat(fromScript.stream().filter(l -> l.contains("\"INCR\"")).count())
                .as("脚本内部必须执行 INCR，实际命令流 = %s", hits)
                .isEqualTo(1L);
        assertThat(fromScript.stream().filter(l -> l.contains("\"EXPIRE\"")).count())
                .as("脚本内部必须执行 EXPIRE，实际命令流 = %s", hits)
                .isEqualTo(1L);
    }

    /** 轮询等待服务端命令流写入（异步推送，无同步点）。 */
    private void awaitMonitoredLine(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            boolean seen = monitoredCommands.stream().anyMatch(l -> l.contains(failKey()));
            if (seen) {
                Thread.sleep(150);
                return;
            }
            Thread.sleep(20);
        }
    }

    /** 从 MONITOR 行中取出命令名，例如 {@code +1.2 [0 lua] "INCR" "k"} -> {@code INCR}。 */
    private static String commandOf(String monitorLine) {
        int idx = monitorLine.indexOf("] ");
        String rest = monitorLine.substring(idx + 2);
        int first = rest.indexOf('"');
        int second = rest.indexOf('"', first + 1);
        return rest.substring(first + 1, second).toUpperCase(java.util.Locale.ROOT);
    }
}
