param([int]$Port=8086,[string]$Grep='')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$qaDir=Join-Path $projectRoot '.runtime\browser-tests'
$sourceJar=Join-Path $projectRoot 'backend\target\cloud-his-1.0.0.jar'
if(!(Test-Path $sourceJar)){throw '请先运行 Build.ps1'}
New-Item -ItemType Directory -Force -Path $qaDir | Out-Null
$qaJar=Join-Path $qaDir 'app.jar'
Copy-Item -LiteralPath $sourceJar -Destination $qaJar -Force
$previousPassword=$env:HIS_INITIAL_PASSWORD
$previousUrl=$env:E2E_BASE_URL
$previousTestPassword=$env:E2E_PASSWORD
$process=$null
try {
    $env:HIS_INITIAL_PASSWORD='His@2026!!'
    $env:E2E_PASSWORD=$env:HIS_INITIAL_PASSWORD
    $env:E2E_BASE_URL="http://127.0.0.1:$Port"
    $process=Start-Process -FilePath (Get-Command java).Source -ArgumentList @('-jar',('"'+$qaJar+'"'),"--server.port=$Port",'--spring.profiles.active=default','--his.production=false','--his.seed=true','--spring.datasource.url=jdbc:h2:mem:browserqa;MODE=MySQL;DATABASE_TO_LOWER=TRUE') -WorkingDirectory $qaDir -WindowStyle Hidden -RedirectStandardOutput (Join-Path $qaDir 'server.out.log') -RedirectStandardError (Join-Path $qaDir 'server.err.log') -PassThru
    $ready=$false
    for($i=0;$i -lt 60;$i++) {
        if($process.HasExited){throw '测试服务启动失败，请查看 .runtime/browser-tests 日志'}
        try {if((Invoke-RestMethod "$env:E2E_BASE_URL/api/health" -TimeoutSec 1).status -eq 'UP'){$ready=$true;break}}catch{}
        Start-Sleep -Milliseconds 500
    }
    if(!$ready){throw '测试服务启动超时'}
    Push-Location (Join-Path $projectRoot 'frontend')
    try {
        $testArguments=@('playwright','test')
        if($Grep){$testArguments+=@('--grep',$Grep)}
        & npx.cmd @testArguments
        if($LASTEXITCODE -ne 0){throw '浏览器验收未通过，见 frontend/test-results'}
    }finally{Pop-Location}
} finally {
    if($process -and !$process.HasExited){Stop-Process -Id $process.Id;$process.WaitForExit(10000) | Out-Null}
    $env:HIS_INITIAL_PASSWORD=$previousPassword
    $env:E2E_BASE_URL=$previousUrl
    $env:E2E_PASSWORD=$previousTestPassword
}
