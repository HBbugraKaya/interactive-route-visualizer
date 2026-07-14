@echo off
setlocal
cd /d "%~dp0"

echo Building React canvas...
cd web
call npm install
if errorlevel 1 exit /b 1
call npm run build
if errorlevel 1 exit /b 1
cd ..

echo Copying web/dist -^> desktop/src/main/resources/web
if exist "desktop\src\main\resources\web" rmdir /s /q "desktop\src\main\resources\web"
mkdir "desktop\src\main\resources\web"
xcopy /e /i /y "web\dist\*" "desktop\src\main\resources\web\" >nul
if errorlevel 1 exit /b 1

echo Done. Run: mvnw -pl desktop exec:java
endlocal
