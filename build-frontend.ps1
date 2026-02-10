param(
    [switch]$SkipNpmInstall,
    [switch]$NoMavenInstall
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Assert-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $Name"
    }
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$FrontendDir = Join-Path $ScriptDir "sf-chain-config-center-frontend"
$StaticDir = Join-Path $ScriptDir "sf-chain-config-center-server\src\main\resources\static"
$DistDir = Join-Path $FrontendDir "dist"
$RepoRoot = $ScriptDir

Write-Step "Validate environment"
Assert-Command "npm"
Assert-Command "npx"
if (-not $NoMavenInstall) {
    Assert-Command "mvn"
}

if (-not (Test-Path $FrontendDir)) {
    throw "Frontend directory not found: $FrontendDir"
}
if (-not (Test-Path (Join-Path $FrontendDir "package.json"))) {
    throw "package.json not found in: $FrontendDir"
}

Push-Location $FrontendDir
try {
    if (-not $SkipNpmInstall) {
        Write-Step "Install frontend dependencies"
        if (Test-Path (Join-Path $FrontendDir "package-lock.json")) {
            & npm ci
        } else {
            & npm install
        }
    } else {
        Write-Step "Skip npm install"
    }

    Write-Step "Build frontend with Vite"
    & npx vite build
} finally {
    Pop-Location
}

if (-not (Test-Path $DistDir)) {
    throw "Build output not found: $DistDir"
}

Write-Step "Sync dist -> static resources"
if (-not (Test-Path $StaticDir)) {
    New-Item -ItemType Directory -Path $StaticDir | Out-Null
}
Get-ChildItem -Force $StaticDir | Remove-Item -Recurse -Force
Copy-Item -Recurse -Force (Join-Path $DistDir "*") $StaticDir

if (-not $NoMavenInstall) {
    Write-Step "Install sf-chain-config-center-server to local Maven repository"
    Push-Location $RepoRoot
    try {
        $mvnArgs = @(
            "-pl", "sf-chain-config-center-server",
            "-am",
            "install",
            "-DskipTests",
            "-Dgpg.skip=true",
            "-Dmaven.javadoc.skip=true"
        )
        & mvn @mvnArgs
    } finally {
        Pop-Location
    }
} else {
    Write-Step "Skip Maven install"
}

Write-Host ""
Write-Host "Frontend deployment update completed." -ForegroundColor Green
Write-Host "Static path: $StaticDir"
if (-not $NoMavenInstall) {
    Write-Host "Maven install: done"
} else {
    Write-Host "Maven install: skipped"
}
