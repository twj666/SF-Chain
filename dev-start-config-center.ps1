param(
  [int]$Port = 19090,
  [switch]$SkipNpmInstall,
  [switch]$SkipKillPort,
  [switch]$BuildOnly
)

$ErrorActionPreference = "Stop"

function Require-Command {
  param([string]$Name)
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Command '$Name' not found. Please install it first."
  }
}

function Run-FrontendBuild {
  param([string]$WorkingDir)

  Push-Location $WorkingDir
  try {
    $npx = Get-Command npx -ErrorAction SilentlyContinue
    if ($npx) {
      & npx vite build
      return
    }

    # npm exec fallback for environments without npx.
    & npm exec vite build
  } finally {
    Pop-Location
  }
}

function Stop-PortProcess {
  param([int]$TargetPort)

  $connections = Get-NetTCPConnection -LocalPort $TargetPort -State Listen -ErrorAction SilentlyContinue
  if (-not $connections) {
    Write-Host "No running process found on port $TargetPort."
    return
  }

  $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
  foreach ($p in $pids) {
    if ($p -and $p -ne $PID) {
      try {
        Stop-Process -Id $p -Force -ErrorAction Stop
        Write-Host "Stopped process $p on port $TargetPort."
      } catch {
        Write-Warning "Failed to stop process $p on port ${TargetPort}: $($_.Exception.Message)"
      }
    }
  }
}

$repoRoot = $PSScriptRoot
$frontendDir = Join-Path $repoRoot "sf-chain-legacy-starter\src\main\frontend"
$distDir = Join-Path $frontendDir "dist"
$staticDir = Join-Path $repoRoot "sf-chain-legacy-starter\src\main\resources\static"

Require-Command npm
Require-Command mvn
Require-Command java

if (-not (Test-Path $frontendDir)) {
  throw "Frontend directory not found: $frontendDir"
}

if (-not (Test-Path (Join-Path $frontendDir "package.json"))) {
  throw "package.json not found: $frontendDir"
}

if (-not $SkipKillPort) {
  $portsToStop = @($Port, 19100) | Select-Object -Unique
  foreach ($p in $portsToStop) {
    Stop-PortProcess -TargetPort $p
  }
}

Push-Location $frontendDir
try {
  if ((-not $SkipNpmInstall) -and (-not (Test-Path (Join-Path $frontendDir "node_modules")))) {
    Write-Host "Installing frontend dependencies..."
    & npm install
  } else {
    Write-Host "Skipping npm install."
  }

  Write-Host "Building frontend with vite..."
  Run-FrontendBuild -WorkingDir $frontendDir

  if (-not (Test-Path $distDir)) {
    throw "Build output missing: $distDir"
  }
} finally {
  Pop-Location
}

if (-not (Test-Path $staticDir)) {
  New-Item -ItemType Directory -Path $staticDir | Out-Null
}

Get-ChildItem -Path $staticDir -Force | Remove-Item -Recurse -Force
Copy-Item -Recurse -Force (Join-Path $distDir "*") $staticDir

$indexPath = Join-Path $staticDir "index.html"
if (-not (Test-Path $indexPath)) {
  throw "Static deploy failed: index.html not found in $staticDir"
}

$indexHtml = Get-Content -Raw $indexPath
$jsMatch = [regex]::Match($indexHtml, "/sf/assets/([^`"]+\.js)")
$cssMatch = [regex]::Match($indexHtml, "/sf/assets/([^`"]+\.css)")

Write-Host "Frontend deployed to static dir:"
Write-Host "  $staticDir"
if ($jsMatch.Success) { Write-Host "  JS : $($jsMatch.Groups[1].Value)" }
if ($cssMatch.Success) { Write-Host "  CSS: $($cssMatch.Groups[1].Value)" }

if ($BuildOnly) {
  Write-Host "BuildOnly enabled. Backend start skipped."
  exit 0
}

Push-Location $repoRoot
try {
  Write-Host "Building backend dependencies..."
  & mvn "-pl" "sf-chain-config-center-server" "-am" "-DskipTests" "package"

  $jarCandidates = Get-ChildItem -Path "sf-chain-config-center-server\target" -Filter "sf-chain-config-center-server-*.jar" |
    Where-Object { $_.Name -notmatch "original" } |
    Sort-Object LastWriteTime -Descending

  if (-not $jarCandidates) {
    throw "Config-center jar not found under sf-chain-config-center-server\\target"
  }

  $runJar = $jarCandidates[0].FullName
  Write-Host "Starting backend config-center server (java -jar)..."
  Write-Host "  $runJar"
  & java "-jar" $runJar
} finally {
  Pop-Location
}
