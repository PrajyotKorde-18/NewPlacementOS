@echo off
title PlacementOS - Internship Approval Backend (Port 8082)
cd /d "%~dp0\internship-approval\backend"
echo Starting Internship Approval Backend on port 8082...
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
pause
