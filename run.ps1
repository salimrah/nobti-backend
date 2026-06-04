#!/usr/bin/env pwsh
# run.ps1 — Windows PowerShell script for noba-backend
# Usage: .\run.ps1

$ErrorActionPreference = "Stop"

# === 1. Start Docker Desktop if not running ===
Write-Host "=== 1. Starting Docker services (PostgreSQL + Redis) ==="
$dockerRunning = $null
try {
    $dockerRunning = docker ps 2>&1 | Out-Null
} catch {}
if (-not $?) {
    Write-Host "   Starting Docker Desktop..."
    $dockerPath = "${env:ProgramFiles}\Docker\Docker\Docker Desktop.exe"
    if (-not (Test-Path $dockerPath)) {
        $dockerPath = "${env:LOCALAPPDATA}\Docker\Docker\Docker Desktop.exe"
    }
    if (Test-Path $dockerPath) {
        Start-Process -FilePath $dockerPath
    } else {
        Write-Host "   ERROR: Docker Desktop not found. Please install or start it manually." -ForegroundColor Red
        exit 1
    }
    Write-Host "   Waiting for Docker to be ready..."
    do {
        Start-Sleep -Seconds 2
        $ready = $null
        try { $ready = docker ps 2>&1 | Out-Null } catch {}
    } until ($?)
    Write-Host "   Docker ready."
}

docker compose -f docker-compose.dev.yml up -d
Write-Host "   PostgreSQL + Redis started."

# === 2. Wait for Postgres health check ===
Write-Host "=== 2. Waiting for Postgres health check ==="
do {
    Start-Sleep -Seconds 2
    $healthy = $null
    try { $healthy = docker exec noba-postgres-dev pg_isready -U postgres 2>&1 | Out-Null } catch {}
} until ($?)
Write-Host "   Postgres is healthy."

# === 3. Check Java 21 ===
Write-Host "=== 3. Checking Java 21 ==="
$javaVer = (java -version 2>&1 | Select-String "version" | ForEach-Object { $_ -replace '.*"([^"]+)".*', '$1' })
Write-Host "   Found: Java $javaVer"
if (-not ($javaVer -match "^21")) {
    Write-Host "   WARNING: Java 21 required but Java $javaVer detected." -ForegroundColor Yellow
    Write-Host "   Set JAVA_HOME to a Java 21 installation, e.g.:" -ForegroundColor Yellow
    Write-Host '   $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot\"' -ForegroundColor Yellow
    Write-Host "   Then re-run this script." -ForegroundColor Yellow
    exit 1
}

# === 4. Start Spring Boot ===
Write-Host "=== 4. Starting Spring Boot ==="
Write-Host ""
$launchMode = ""
if ($args.Count -gt 0) {
    $launchMode = $args[0]
} else {
    $response = Read-Host "  Do you want to run the app from your IDE or from the terminal? [y/N]"
    if ($response -match '^[yY](es)?$') {
        $launchMode = "terminal"
    } else {
        $launchMode = "ide"
    }
}
switch -regex ($launchMode) {
    '^(terminal|mvn|spring)$' {
        Write-Host ""
        Write-Host "  Starting Spring Boot from terminal..."
        ./mvnw.cmd clean spring-boot:run
    }
    default {
        Write-Host ""
        Write-Host "  Skipping terminal start. Run the app from your IDE (IntelliJ, VS Code, etc.)."
        Write-Host "  Entry point: src/main/java/ma/noba/NobaBackendApplication.java"
    }
}
