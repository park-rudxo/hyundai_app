param([string[]]$Tasks = @(':app:assembleDemoDebug', ':app:assemblePhoneDebug', ':app:assembleVehicleDebug', ':app:testDemoDebugUnitTest'))
$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem 'C:/Program Files/Microsoft' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jdk) { throw 'JDK 17 경로를 JAVA_HOME으로 설정해 주세요.' }
    $env:JAVA_HOME = $jdk.FullName
}
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$outputRoot = Join-Path $env:LOCALAPPDATA 'Changyeoga/build'
Push-Location $PSScriptRoot
try {
    & ./gradlew.bat @Tasks "-PbuildRoot=$outputRoot"
    if ($LASTEXITCODE -ne 0) { throw "Gradle 실패: $LASTEXITCODE" }
    Write-Host "빌드 결과: $outputRoot/app/outputs"
} finally { Pop-Location }
