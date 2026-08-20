<#
.SYNOPSIS
XCimoc APK 一键构建脚本 (Windows / PowerShell)

.EXAMPLE
.\build.ps1             # 默认 release
.\build.ps1 -BuildType debug   # 编译 debug
#>
[CmdletBinding()]
param(
    [ValidateSet('release', 'debug')]
    [string]$BuildType = 'release'
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# 执行 Gradle 任务，失败即抛出（gradlew.bat 非零退出码不会自动触发 ErrorAction）
function Invoke-Gradle {
    param([string]$Task)
    Write-Host "`n>> 执行: .\gradlew.bat $Task" -ForegroundColor Cyan
    & .\gradlew.bat $Task
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle 任务失败: $Task (exit code $LASTEXITCODE)"
    }
}

# 任务名首字母大写：release -> assembleRelease / debug -> assembleDebug
$Task = "assemble$($BuildType.Substring(0, 1).ToUpper())$($BuildType.Substring(1))"

Write-Host ""
Write-Host "=============================================="
Write-Host " 编译 APK (:app:$Task)"
Write-Host "=============================================="
Invoke-Gradle ":app:$Task"

Write-Host ""
Write-Host "=============================================="
Write-Host " 构建完成 ✅  APK 输出目录："
Write-Host "   app\build\outputs\apk\$BuildType\"
Write-Host "=============================================="
