@echo off
title MediSync - Health & Medicine Management System
cd /d "%~dp0"

echo ===================================================================
echo   Starting MediSync (Java Swing Modern UI + MySQL 8.0)
echo ===================================================================
echo.

echo [1/2] Compiling Java files...
javac -cp ".;mysql-connector-j-8.3.0.jar;flatlaf-3.5.4.jar" DBConnection.java MedicineApp.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Launching Java Window...
java --enable-native-access=ALL-UNNAMED -cp ".;mysql-connector-j-8.3.0.jar;flatlaf-3.5.4.jar" MedicineApp

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [INFO] Application exited.
    pause
)
