<#
.SYNOPSIS
  data_server native-only build script.
  - Builds ONLY for the current host (GOOS/GOARCH detected via `go env`).
  - CGO is always enabled (gorm.io/driver/sqlite requires mattn/go-sqlite3 via CGO).

.DESCRIPTION
  Usage examples:
    .\build.ps1                       # native build -> .\dist\
    .\build.ps1 -OutDir .\artifacts   # custom output directory
    .\build.ps1 -DryRun               # print commands, do not execute
    .\build.ps1 -BuildVerbose         # verbose (passes -x to go build)
    Get-Help .\build.ps1 -Full        # full help
#>

[CmdletBinding()]
param(
  [string]$OutDir,
  [switch]$DryRun,
  [Alias('VerboseBuild')]
  [switch]$BuildVerbose
)

$ErrorActionPreference = 'Stop'

# -----------------------------------------------------------------------------
# Defaults
# -----------------------------------------------------------------------------
if ([string]::IsNullOrEmpty($OutDir)) {
  $OutDir = Join-Path $PSScriptRoot 'dist'
}

# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------
function Write-Step {
  param([string]$Msg)
  Write-Host ('==> ' + $Msg) -ForegroundColor Cyan
}

function Write-Hint {
  param([string]$Msg)
  Write-Host ('    hint: ' + $Msg) -ForegroundColor DarkGray
}

<#
.SYNOPSIS
  Invokes an external command with a temporary environment override.
  Uses splatting so an arg like "-ldflags=-s -w" is passed as a SINGLE string.
#>
function Invoke-Native {
  param(
    [Parameter(Mandatory=$true)][string]$FilePath,
    [string[]]$Arguments,
    [hashtable]$Env
  )

  if (-not $Arguments) { $Arguments = @() }
  if (-not $Env)       { $Env = @{} }

  # DryRun: pretty-print what would run, then return.
  if ($DryRun) {
    $envList = @()
    foreach ($k in $Env.Keys) {
      $envList += ('$env:' + $k + "='" + $Env[$k] + "'")
    }
    $argList = @()
    foreach ($a in $Arguments) {
      if ($a -match '\s') { $argList += ("'" + $a + "'") }
      else                { $argList += $a }
    }
    $envStr = ''
    if ($envList.Count -gt 0) { $envStr = '(' + ($envList -join '; ') + ')' }
    $argStr = ($argList -join ' ').TrimEnd()
    $line = ('RUN ' + $envStr + ': ' + $FilePath + ' ' + $argStr).TrimEnd()
    Write-Host $line
    return 0
  }

  $printArg = [bool]$BuildVerbose -or ($VerbosePreference -eq 'Continue')
  if ($printArg) {
    $argList = @()
    foreach ($a in $Arguments) {
      if ($a -match '\s') { $argList += ("'" + $a + "'") }
      else                { $argList += $a }
    }
    $argStr = ($argList -join ' ').TrimEnd()
    Write-Host ('RUN: ' + $FilePath + ' ' + $argStr).TrimEnd()
  }

  $saved = @{}
  foreach ($k in $Env.Keys) {
    $saved[$k] = [Environment]::GetEnvironmentVariable($k, 'Process')
    [Environment]::SetEnvironmentVariable($k, $Env[$k], 'Process')
  }
  try {
    if ($Arguments.Count -eq 0) { & $FilePath }
    else                        { & $FilePath @Arguments }
    $exitCode = $LASTEXITCODE
  }
  finally {
    foreach ($k in $saved.Keys) {
      [Environment]::SetEnvironmentVariable($k, $saved[$k], 'Process')
    }
  }
  return $exitCode
}

function Require-Command {
  param(
    [string]$Cmd,
    [string]$Hint
  )
  $found = Get-Command $Cmd -ErrorAction SilentlyContinue
  if (-not $found) {
    Write-Host ('ERROR: missing required command: ' + $Cmd) -ForegroundColor Red
    if (-not [string]::IsNullOrEmpty($Hint)) { Write-Hint $Hint }
    exit 2
  }
  return $found.Source
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------
Set-Location -LiteralPath $PSScriptRoot
$null = Require-Command -Cmd 'go' -Hint 'Install Go 1.20+: https://go.dev/dl/ . Restart the shell so "go" is on PATH.'

# gcc is only required for a real build; DryRun just prints the command.
if (-not $DryRun) {
  $gccHint = @(
    'Install a local C compiler (MinGW GCC) for CGO.',
    '  MSYS2 UCRT64: winget install -e --id msys2.msys2',
    '    then in UCRT64 shell: pacman -S --needed mingw-w64-ucrt-x86_64-gcc go',
    '  OR Chocolatey: choco install -y mingw --prefer-binary',
    '  Finally add <MSYS2>\ucrt64\bin or <MinGW>\bin to PATH; verify with "gcc --version".'
  ) -join [Environment]::NewLine
  $null = Require-Command -Cmd 'gcc' -Hint $gccHint
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$hostGoos = & go env GOOS
if ($LASTEXITCODE -ne 0) { throw 'go env GOOS failed' }
$hostGoarch = & go env GOARCH
if ($LASTEXITCODE -ne 0) { throw 'go env GOARCH failed' }

$ext = ''
if ($hostGoos -eq 'windows') { $ext = '.exe' }
$binName = 'xcimoc-data-server-' + $hostGoos + '-' + $hostGoarch + $ext
$outFile = Join-Path $OutDir $binName

# Build the go argument array.
$allArgs = @('build', '-ldflags=-s -w')
if ($BuildVerbose -or ($VerbosePreference -eq 'Continue')) { $allArgs += '-x' }
$allArgs += '-o'
$allArgs += $outFile
$allArgs += '.'

Write-Step ('Build native (' + $hostGoos + '/' + $hostGoarch + ')')
$e = Invoke-Native -FilePath go -Arguments $allArgs -Env @{ CGO_ENABLED = '1' }
if ($e -ne 0) { throw ('go build failed (exit=' + $e + ')') }

# -----------------------------------------------------------------------------
# Finalize
# -----------------------------------------------------------------------------
if (-not $DryRun) {
  $cfgSrc = Join-Path $PSScriptRoot 'config.example.yaml'
  $cfgDst = Join-Path $OutDir     'config.example.yaml'
  Copy-Item -Force -LiteralPath $cfgSrc -Destination $cfgDst
  Write-Host ''
  Write-Host ('Build succeeded. Binaries in: ' + $OutDir) -ForegroundColor Green
  $sizeExpr = @{ Label='SizeKB'; Expression={ [math]::Round($_.Length / 1KB, 1) } }
  Get-ChildItem -LiteralPath $OutDir -File |
    Sort-Object Name |
    Format-Table Name, $sizeExpr -AutoSize
}
