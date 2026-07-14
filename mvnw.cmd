@echo off
setlocal
set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_HOME=%MAVEN_PROJECTBASEDIR%.tools\apache-maven-3.9.9

if exist "%MAVEN_HOME%\bin\mvn.cmd" (
  "%MAVEN_HOME%\bin\mvn.cmd" %*
  exit /b %ERRORLEVEL%
)

where mvn >nul 2>&1
if %ERRORLEVEL%==0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

echo Maven not found. Install Maven or extract Apache Maven 3.9.9 to:
echo   %MAVEN_HOME%
exit /b 1
