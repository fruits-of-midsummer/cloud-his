$ErrorActionPreference = 'Stop'
$mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
if ($mavenCommand) { return $mavenCommand.Source }
if ($env:MAVEN_HOME -and (Test-Path (Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'))) { return (Join-Path $env:MAVEN_HOME 'bin\mvn.cmd') }
$cachedMaven = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2\wrapper\dists'
if (Test-Path $cachedMaven) {
    $mavenFile = Get-ChildItem -LiteralPath $cachedMaven -Filter mvn.cmd -Recurse -File | Select-Object -First 1
    if ($mavenFile) { return $mavenFile.FullName }
}
$projectRoot = Split-Path $PSScriptRoot -Parent
$toolsDir = Join-Path $projectRoot '.runtime\tools'
$mavenPath = Join-Path $toolsDir 'apache-maven-3.9.9\bin\mvn.cmd'
if (!(Test-Path $mavenPath)) {
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $archive = Join-Path $toolsDir 'maven.zip'
    $source = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip'
    Write-Host '下载项目本地 Maven 3.9.9...'
    Invoke-WebRequest -Uri $source -OutFile $archive
    $expected = (Invoke-WebRequest -Uri ($source + '.sha512')).Content.Trim().Split(' ')[0]
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash.ToLower() -ne $expected.ToLower()) { throw 'Maven 下载校验失败' }
    Expand-Archive -LiteralPath $archive -DestinationPath $toolsDir -Force
}
return $mavenPath
