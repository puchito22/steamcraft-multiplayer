@echo off
title Subir SteamCraft a GitHub
cd /d "%~dp0"
echo ========================================================
echo   Subiendo SteamCraft a GitHub (puchito22)
echo ========================================================
echo.
echo Si se abre una ventana del navegador, inicia sesion con
echo tu cuenta de GitHub 'puchito22' para autorizar la subida.
echo.
git push -u origin main
echo.
if %ERRORLEVEL% equ 0 (
    echo ========================================================
    echo   EXITO: Todos los archivos se subieron correctamente!
    echo ========================================================
) else (
    echo Ocurrio un error. Revisa el mensaje de arriba.
)
echo.
pause
