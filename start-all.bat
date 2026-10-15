@echo off
echo ========================================================
echo           Starting PlacementOS Services Locally
echo ========================================================
echo.
echo [1/4] Starting TPMS Backend (Port 8081)...
start "TPMS Backend (8081)" cmd /k "cd /d %~dp0\tpms\backend && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo"

echo [2/4] Starting Internship Approval Backend (Port 8082)...
start "Internship Approval Backend (8082)" cmd /k "cd /d %~dp0\internship-approval\backend && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo"

echo.
echo Waiting 8 seconds for backends to initialize before starting frontends...
timeout /t 8 /nobreak >nul

echo [3/4] Starting TPMS Frontend (Port 3000)...
start "TPMS Frontend (3000)" cmd /k "cd /d %~dp0\tpms\frontend && npm run dev"

echo [4/4] Starting Internship Approval Frontend (Port 3001)...
start "Internship Frontend (3001)" cmd /k "cd /d %~dp0\internship-approval\frontend && npm run dev"

echo.
echo ========================================================
echo All 4 microservices launched successfully!
echo.
echo - TPMS Portal:              http://localhost:3000
echo - Internship Portal:        http://localhost:3001
echo - TPMS Backend API:         http://localhost:8081/api
echo - Internship Backend API:   http://localhost:8082/api
echo ========================================================
