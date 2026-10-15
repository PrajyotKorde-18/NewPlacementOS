# ========================================================
# PlacementOS - Start All Microservices Locally
# ========================================================

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "       Starting PlacementOS Microservices Locally       " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$root = $PSScriptRoot

Write-Host "`n[1/4] Starting TPMS Backend (Port 8081)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "cd /d `"$root\tpms\backend`" && .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo"

Write-Host "[2/4] Starting Internship Approval Backend (Port 8082)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "cd /d `"$root\internship-approval\backend`" && .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo"

Write-Host "`nWaiting 8 seconds for backends to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 8

Write-Host "[3/4] Starting TPMS Frontend (Port 3000)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "cd /d `"$root\tpms\frontend`" && npm run dev"

Write-Host "[4/4] Starting Internship Approval Frontend (Port 3001)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "cd /d `"$root\internship-approval\frontend`" && npm run dev"

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "All 4 microservices launched in separate windows!" -ForegroundColor Cyan
Write-Host "TPMS Portal:              http://localhost:3000" -ForegroundColor White
Write-Host "Internship Portal:        http://localhost:3001" -ForegroundColor White
Write-Host "TPMS Backend API:         http://localhost:8081/api" -ForegroundColor White
Write-Host "Internship Backend API:   http://localhost:8082/api" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor Cyan
