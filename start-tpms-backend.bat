@echo off
title PlacementOS - TPMS Backend (Port 8081)
cd /d "%~dp0\tpms\backend"
echo Starting TPMS Backend on port 8081...
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
pause
