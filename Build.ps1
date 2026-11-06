param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$mavenPath = & (Join-Path $projectRoot 'scripts\Resolve-Maven.ps1')
Push-Location (Join-Path $projectRoot 'frontend')
try {
    if (!(Test-Path 'node_modules')) { & npm.cmd ci --registry=https://registry.npmjs.org --no-audit --no-fund; if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败' } }
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw '前端构建失败' }
} finally { Pop-Location }
Push-Location (Join-Path $projectRoot 'backend')
try {
    $settingsPath = if ($env:HIS_MAVEN_SETTINGS) { $env:HIS_MAVEN_SETTINGS } else { Join-Path $projectRoot '.mvn\settings.xml' }
    $mavenArgs = @('-gs', $settingsPath, '-s', $settingsPath, '-B', '-ntp', 'clean', 'verify')
    if ($SkipTests) { $mavenArgs += '-DskipTests' }
    & $mavenPath @mavenArgs
    if ($LASTEXITCODE -ne 0) { throw '后端构建或验证失败' }
} finally { Pop-Location }
Write-Host '构建完成：backend\target\cloud-his-1.0.0.jar（已包含前端）'
