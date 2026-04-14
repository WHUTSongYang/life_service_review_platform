# 在仓库根目录执行: .\scripts\package-for-centos.ps1
# 生成 dist-package\ 下的 jar、前端静态文件，便于上传到 CentOS

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$out = Join-Path $root "dist-package"
$jarOut = Join-Path $out "backend-jar"
$feOut = Join-Path $out "frontend-dist"

Write-Host ">>> Maven package (backend)..."
Push-Location (Join-Path $root "backend")
& mvn -q -DskipTests clean package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Pop-Location

New-Item -ItemType Directory -Force -Path $jarOut, $feOut | Out-Null
$jar = Get-ChildItem (Join-Path $root "backend\target\*.jar") | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
if (-not $jar) { Write-Error "No backend jar found in backend/target" }
Copy-Item $jar.FullName (Join-Path $jarOut "life-review-backend.jar") -Force

Write-Host ">>> npm build (frontend, same-origin API: empty VITE_API_BASE)..."
$env:VITE_API_BASE = ""
Push-Location (Join-Path $root "frontend")
if (-not (Test-Path "node_modules")) { npm install }
npm run build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Pop-Location
Remove-Item Env:VITE_API_BASE -ErrorAction SilentlyContinue

Copy-Item -Path (Join-Path $root "frontend\dist\*") -Destination $feOut -Recurse -Force

$readme = @"
CentOS 部署包（由 scripts/package-for-centos.ps1 生成）

1. backend-jar/life-review-backend.jar
   上传到服务器 /opt/life-review/app/ ，配置好 MySQL/Redis 后:
   java -jar life-review-backend.jar
   详见 deploy/centos/README.md

2. frontend-dist/
   内容上传到 /opt/life-review/www/ （与 nginx-life-review.conf.example 中 root 一致）
   前端构建时已使用同域反代（VITE_API_BASE 为空），需配合 Nginx 将 /api 转发到后端。

3. 将 deploy/centos/ 下 nginx、systemd 示例拷到服务器并按 README 修改。
"@

Set-Content -Path (Join-Path $out "README.txt") -Value $readme -Encoding UTF8
Write-Host ">>> Done: $out"
Write-Host $readme
