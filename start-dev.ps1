# ============================================================
# 本地开发启动脚本（dev profile）
#
# 依赖项目根目录的 .env（参考 .env.example，已被 .gitignore 忽略）。
# .env 会被作为 Spring 附加配置源加载，因此其中可以放 DB_PASSWORD 等敏感值，
# 且支持空值语义（例如本机 Redis 无密码可写 REDIS_PASSWORD=）。
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File .\start-dev.ps1
# ============================================================
$ErrorActionPreference = 'Stop'

$root    = $PSScriptRoot
$envFile = Join-Path $root '.env'
$jar     = Join-Path $root 'vb-gateway\target\vb-gateway-1.0.0-SNAPSHOT.jar'

if (-not (Test-Path $jar)) {
    throw "未找到 $jar，请先执行：mvn clean install -DskipTests"
}
if (-not (Test-Path $envFile)) {
    throw "未找到 $envFile，请先执行：Copy-Item .env.example .env  并填写 DB_PASSWORD 等"
}

# 以项目根目录为工作目录，保证 ./uploads 等相对路径正确
Set-Location $root

Write-Host "[start-dev] jar    : $jar"
Write-Host "[start-dev] config : $envFile"
Write-Host "[start-dev] 接口文档: http://localhost:8080/doc.html"

# .env 以 properties 语义导入，profile 固定为 dev
java -jar $jar --spring.profiles.active=dev "--spring.config.import=optional:file:./.env[.properties]"
