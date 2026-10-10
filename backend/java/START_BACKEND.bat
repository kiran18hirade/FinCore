@echo off
echo Set DB_PASSWORD in your Windows environment before starting.
if "%DB_PASSWORD%"=="" (
  echo DB_PASSWORD is not set. Set it with: set DB_PASSWORD=your_local_mysql_password
  pause
  exit /b 1
)
call mvn spring-boot:run
pause
