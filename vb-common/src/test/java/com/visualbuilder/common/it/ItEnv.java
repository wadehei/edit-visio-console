package com.visualbuilder.common.it;

/**
 * 集成测试（{@code *IT}）环境参数的统一入口。
 *
 * <p>所有参数优先从系统属性读取，运行者用命令行覆盖，例如：</p>
 * <pre>
 * mvn -pl vb-user -am verify "-Dit.test=AdminUserStatusCacheIT" \
 *     "-Dit.mysql.url=jdbc:mysql://127.0.0.1:3307/visual_builder"
 * </pre>
 *
 * <p>未提供时回退到本机默认值（MySQL {@code localhost:3306}、Redis {@code localhost:6379}）。
 * 回退默认值<b>不构成对本机中间件的硬依赖</b>：中间件不可达时，各 *IT 通过
 * {@code Assume.assumeTrue} 把用例判定为<b>跳过（skipped）而非通过</b>。</p>
 *
 * <p>集中在此处是为了让"默认值 + 覆盖方式 + 跳过语义"只有一个来源；分散在各 IT 中复制
 * 同一套 {@code System.getProperty} 默认值，容易在改动时出现口径漂移。</p>
 */
public final class ItEnv {

    private ItEnv() {
    }

    /** MySQL JDBC 连接串，覆盖属性 {@code it.mysql.url}。 */
    public static String mysqlUrl() {
        return System.getProperty("it.mysql.url", "jdbc:mysql://localhost:3306/visual_builder");
    }

    /** MySQL 用户名，覆盖属性 {@code it.mysql.user}。 */
    public static String mysqlUser() {
        return System.getProperty("it.mysql.user", "root");
    }

    /** MySQL 密码，覆盖属性 {@code it.mysql.password}。 */
    public static String mysqlPassword() {
        return System.getProperty("it.mysql.password", "");
    }

    /** Redis 主机，覆盖属性 {@code it.redis.host}。 */
    public static String redisHost() {
        return System.getProperty("it.redis.host", "localhost");
    }

    /** Redis 端口，覆盖属性 {@code it.redis.port}。 */
    public static int redisPort() {
        return Integer.parseInt(System.getProperty("it.redis.port", "6379"));
    }

    /** Redis 密码（可为空），覆盖属性 {@code it.redis.password}。 */
    public static String redisPassword() {
        return System.getProperty("it.redis.password", "");
    }
}
