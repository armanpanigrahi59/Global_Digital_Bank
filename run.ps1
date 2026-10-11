# Run script for Global Digital Bank
param(
    [string]$Mode = "file"
)

$projectRoot = $PSScriptRoot
if (-not $projectRoot) { $projectRoot = Get-Location }

$jarPath = Join-Path $projectRoot "target\global-digital-bank.jar"
$outDir = Join-Path $projectRoot "out"

if (-not (Test-Path $jarPath) -and -not (Test-Path $outDir)) {
    & (Join-Path $projectRoot "build.ps1")
}

if (Test-Path $jarPath) {
    java -jar $jarPath $Mode
} else {
    java -cp $outDir com.gdb.GlobalDigitalBankApp $Mode
}
