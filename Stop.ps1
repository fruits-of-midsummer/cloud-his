$ErrorActionPreference = 'Stop'
$pidFile = Join-Path $PSScriptRoot '.runtime\server.json'
if (!(Test-Path $pidFile)) { Write-Host '没有本项目启动的服务记录'; exit 0 }
$saved = Get-Content -Raw -LiteralPath $pidFile | ConvertFrom-Json
$running = Get-Process -Id $saved.processId -ErrorAction SilentlyContinue
if ($running) {
    if ($running.StartTime.ToUniversalTime().Ticks.ToString() -ne $saved.start_ticks) { throw '进程编号已被其他程序复用，不会停止该进程' }
    Stop-Process -Id $running.Id
    if (!$running.WaitForExit(10000)) { throw '服务尚未退出，请稍后再试' }
    Write-Host '本项目服务已停止'
}
Remove-Item -LiteralPath $pidFile -Force
