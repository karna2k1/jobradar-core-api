# Starts the whole JobRadar stack locally (Windows PowerShell).
# Expects the six repos side by side:  <root>\jobradar-core-api, job-ingest-service, match-notify-service,
#                                      jobradar-shell, jobs-feed-mfe, insights-mfe
# Usage:  .\jobradar-core-api\infra\run-local.ps1            (everything)
#         .\jobradar-core-api\infra\run-local.ps1 -NoFrontend
param([switch]$NoFrontend)

$ErrorActionPreference = 'Stop'
$infra = $PSScriptRoot
$root = Resolve-Path (Join-Path $infra '..\..')

Write-Host '1/4 Infra: Postgres, Redis, Redpanda, MailHog' -ForegroundColor Cyan
docker compose -f (Join-Path $infra 'docker-compose.yml') --profile messaging up -d
for ($i = 0; $i -lt 30; $i++) {
    docker exec jobradar-postgres-1 pg_isready -U jobradar 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { break }
    Start-Sleep -Seconds 2
}

function Start-Window($title, $dir, $command) {
    Start-Process powershell -WorkingDirectory $dir -ArgumentList '-NoExit', '-Command', "`$host.UI.RawUI.WindowTitle='$title'; $command"
}

Write-Host '2/4 Backends (each capped at -Xmx256m)' -ForegroundColor Cyan
Start-Window 'core-api :8080' (Join-Path $root 'jobradar-core-api') 'mvn -q spring-boot:run'
Start-Sleep -Seconds 20   # core-api first: match-notify reads profiles from it
Start-Window 'ingest :8081' (Join-Path $root 'job-ingest-service') 'mvn -q spring-boot:run'
Start-Window 'match :8082' (Join-Path $root 'match-notify-service') 'mvn -q spring-boot:run'

if (-not $NoFrontend) {
    Write-Host '3/4 Frontends (built + preview: Module Federation serves remoteEntry.js from dist)' -ForegroundColor Cyan
    foreach ($fe in 'jobradar-shell', 'jobs-feed-mfe', 'insights-mfe') {
        Start-Window $fe (Join-Path $root $fe) 'if (-not (Test-Path node_modules)) { npm ci }; npm run build; npm run preview'
    }
}

Write-Host '4/4 Ready in about a minute:' -ForegroundColor Green
Write-Host '  App        http://localhost:3000'
Write-Host '  Swagger    http://localhost:8080/swagger-ui.html  :8081  :8082'
Write-Host '  MailHog    http://localhost:8025'
Write-Host 'Stop: close the windows, then  docker compose -f jobradar-core-api/infra/docker-compose.yml --profile messaging stop'
