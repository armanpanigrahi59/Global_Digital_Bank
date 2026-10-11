# Build script for Global Digital Bank
$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
if (-not $projectRoot) { $projectRoot = Get-Location }

Write-Host "Building Global Digital Bank..." -ForegroundColor Cyan

$outDir = Join-Path $projectRoot "out"
$targetDir = Join-Path $projectRoot "target"
$srcJava = Join-Path $projectRoot "src\main\java"
$srcResources = Join-Path $projectRoot "src\main\resources"

if (Test-Path $outDir) { Remove-Item -Recurse -Force $outDir }
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
if (-not (Test-Path $targetDir)) { New-Item -ItemType Directory -Path $targetDir -Force | Out-Null }

$javaFiles = (Get-ChildItem -Path $srcJava -Recurse -Filter *.java).FullName
javac -d $outDir -encoding UTF-8 $javaFiles
Copy-Item -Recurse -Path "$srcResources\*" -Destination $outDir

# Find jar executable
$jarCmd = "jar"
if (-not (Get-Command jar -ErrorAction SilentlyContinue)) {
    $jdkPaths = Get-ChildItem "C:\Program Files\Java" -Filter "jdk*" -ErrorAction SilentlyContinue | Sort-Object Name -Descending
    foreach ($jdk in $jdkPaths) {
        $candidate = Join-Path $jdk.FullName "bin\jar.exe"
        if (Test-Path $candidate) {
            $jarCmd = $candidate
            break
        }
    }
}

if ($jarCmd -ne "jar" -or (Get-Command jar -ErrorAction SilentlyContinue)) {
    & $jarCmd cfe "$targetDir\global-digital-bank.jar" com.gdb.GlobalDigitalBankApp -C $outDir .
    Write-Host "Build successful! Created target\global-digital-bank.jar and out/ classes." -ForegroundColor Green
} else {
    Write-Host "Compiled classes to out/. (jar tool not found in PATH or standard JDK directories)" -ForegroundColor Green
}
