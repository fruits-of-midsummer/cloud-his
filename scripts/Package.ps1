$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $projectRoot 'backend\target\cloud-his-1.0.0.jar'
if (!(Test-Path $jar)) { throw '请先运行 Build.ps1 完成构建与验证' }
$delivery = Join-Path $projectRoot 'delivery'
New-Item -ItemType Directory -Force -Path $delivery | Out-Null
Copy-Item -LiteralPath $jar -Destination (Join-Path $delivery 'cloud-his-1.0.0.jar') -Force
$releaseDocs = Join-Path $PSScriptRoot 'release'
Copy-Item -LiteralPath (Join-Path $releaseDocs 'README.md') -Destination $delivery -Force
Copy-Item -LiteralPath (Join-Path $releaseDocs '运行说明.txt') -Destination $delivery -Force
foreach ($scriptName in @('Start.ps1','Stop.ps1','启动.cmd','停止.cmd')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot $scriptName) -Destination $delivery -Force
}
Copy-Item -LiteralPath (Join-Path $projectRoot '启动.cmd') -Destination (Join-Path $delivery 'run.cmd') -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'docs') -Destination $delivery -Recurse -Force
Get-FileHash -LiteralPath (Join-Path $delivery 'cloud-his-1.0.0.jar') -Algorithm SHA256 | Select-Object Hash | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $delivery 'sha256.json') -Encoding UTF8
$archive=Join-Path $delivery 'cloud-his-1.0.0.zip'
$entries=@('cloud-his-1.0.0.jar','README.md','docs','运行说明.txt','sha256.json','run.cmd','Start.ps1','Stop.ps1','启动.cmd','停止.cmd') | ForEach-Object {Join-Path $delivery $_}
Compress-Archive -LiteralPath $entries -DestinationPath $archive -Force
Write-Host "发布文件已生成：$delivery"
