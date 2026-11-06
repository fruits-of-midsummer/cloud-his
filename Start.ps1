param([int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$runtimeDir = Join-Path $projectRoot '.runtime'
$releaseJar = Join-Path $projectRoot 'cloud-his-1.0.0.jar'
$isRelease = Test-Path -LiteralPath $releaseJar
$jarPath = if ($isRelease) { $releaseJar } else { Join-Path $projectRoot 'backend\target\cloud-his-1.0.0.jar' }
$workingDir = if ($isRelease) { $projectRoot } else { $runtimeDir }
if (!(Test-Path -LiteralPath $jarPath)) {
    $buildScript = Join-Path $projectRoot 'Build.ps1'
    if (!(Test-Path -LiteralPath $buildScript)) { throw '未找到应用JAR，请将完整运行包解压后再启动' }
    & $buildScript
    if (!(Test-Path -LiteralPath $jarPath)) { throw '请先成功构建项目' }
}
New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
$pidFile = Join-Path $runtimeDir 'server.json'
if (Test-Path $pidFile) {
    $saved = Get-Content -Raw -LiteralPath $pidFile | ConvertFrom-Json
    $running = Get-Process -Id $saved.processId -ErrorAction SilentlyContinue
    if ($running -and $running.StartTime.ToUniversalTime().Ticks.ToString() -eq $saved.start_ticks) { Write-Host "服务已启动：http://127.0.0.1:$($saved.port)"; exit 0 }
}
$portBusy = $false
$portProbe = New-Object System.Net.Sockets.TcpClient
try {
    $pendingConnect = $portProbe.ConnectAsync('127.0.0.1', $Port)
    if ($pendingConnect.Wait(500)) { $portBusy = $portProbe.Connected }
} catch {} finally { $portProbe.Dispose() }
if ($portBusy) {
    throw "端口 $Port 已被其他实例占用。若已在终端通过 java -jar 启动，请先在原终端按 Ctrl+C 停止，再使用启动.cmd。"
}
$javaPath = (Get-Command java -ErrorAction Stop).Source
$runtimeJar = Join-Path $runtimeDir 'cloud-his.jar'
Copy-Item -LiteralPath $jarPath -Destination $runtimeJar -Force
$process = Start-Process -FilePath $javaPath -ArgumentList @('-jar', ('"' + $runtimeJar + '"'), "--server.port=$Port") -WorkingDirectory $workingDir -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtimeDir 'server.out.log') -RedirectStandardError (Join-Path $runtimeDir 'server.err.log') -PassThru
@{processId=$process.Id;start_ticks=$process.StartTime.ToUniversalTime().Ticks.ToString();port=$Port} | ConvertTo-Json | Set-Content -LiteralPath $pidFile -Encoding UTF8
for ($attempt=0; $attempt -lt 60; $attempt++) {
    if ($process.HasExited) { throw "服务启动失败，请查看 $runtimeDir\server.out.log 与 server.err.log" }
    try {
        $status = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/health" -TimeoutSec 1
        if ($status.status -eq 'UP') {
            Write-Host "服务已启动：http://127.0.0.1:$Port"
            Write-Host ('数据目录：' + (Join-Path $workingDir 'data'))
            Write-Host '停止服务：双击停止.cmd，或运行 .\Stop.ps1'
            exit 0
        }
    } catch {}
    Start-Sleep -Milliseconds 500
}
throw '启动超时，请查看 .runtime 下日志'
