@echo off
title PlacementOS - TPMS Frontend (Port 3000)
cd /d "%~dp0\tpms\frontend"
echo Starting TPMS Frontend on port 3000...
npm run dev
pause
