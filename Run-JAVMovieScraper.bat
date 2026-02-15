@echo off
cd /d "%~dp0"
java -jar "build\libs\JAVMovieScraper-0.3.7--1-all.jar" %*
if errorlevel 1 pause
