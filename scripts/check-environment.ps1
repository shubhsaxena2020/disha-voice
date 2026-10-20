$ErrorActionPreference = 'Continue'
$ok = $true

function Check-Command($Name, $Label) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($cmd) { Write-Host "OK      $Label : $($cmd.Source)" -ForegroundColor Green }
    else { Write-Host "MISSING $Label" -ForegroundColor Yellow; $script:ok = $false }
}

Write-Host 'Disha Voice development environment' -ForegroundColor Cyan
Write-Host "OS: $([System.Runtime.InteropServices.RuntimeInformation]::OSDescription)"
Check-Command 'git' 'Git'
Check-Command 'java' 'Java'
Check-Command 'adb' 'Android Debug Bridge'
Check-Command 'gh' 'GitHub CLI'
Check-Command 'node' 'Node.js (optional; website is static)'

if ($env:JAVA_HOME) {
    Write-Host "JAVA_HOME=$env:JAVA_HOME"
    & "$env:JAVA_HOME\bin\java.exe" -version 2>&1 | Select-Object -First 3
} else { Write-Host 'JAVA_HOME is not set' -ForegroundColor Yellow; $ok = $false }

$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
if (Test-Path $sdk) {
    Write-Host "Android SDK: $sdk"
    if (Test-Path (Join-Path $sdk 'platforms\android-36\android.jar')) { Write-Host 'OK      Android SDK Platform 36' -ForegroundColor Green }
    else { Write-Host 'MISSING Android SDK Platform 36' -ForegroundColor Yellow; $ok = $false }
    if (Test-Path (Join-Path $sdk 'build-tools\36.0.0\aapt.exe')) { Write-Host 'OK      Android Build-Tools 36.0.0' -ForegroundColor Green }
    else { Write-Host 'MISSING Android Build-Tools 36.0.0' -ForegroundColor Yellow; $ok = $false }
    if (Test-Path (Join-Path $sdk 'platform-tools\adb.exe')) { & (Join-Path $sdk 'platform-tools\adb.exe') devices -l }
} else { Write-Host "MISSING Android SDK (expected $sdk)" -ForegroundColor Yellow; $ok = $false }

$free = (Get-PSDrive -Name C -ErrorAction SilentlyContinue).Free
if ($free) { Write-Host ('C: free space: {0:N1} GB' -f ($free / 1GB)) }
if ($ok) { Write-Host 'Core prerequisites appear present.' -ForegroundColor Green; exit 0 }
Write-Host 'Some prerequisites are missing; pure core tests require only JDK 17+.' -ForegroundColor Yellow
exit 1
