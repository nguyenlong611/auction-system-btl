@echo off
title Adjuge Auction System
echo ==========================================
echo    ADJUGE - STARTING APPLICATION
echo ==========================================
echo.

call mvn javafx:run

if %ERRORLEVEL% neq 0 (
    echo.
    echo [Adjuge] Application stopped with error code %ERRORLEVEL%.
    pause
)
