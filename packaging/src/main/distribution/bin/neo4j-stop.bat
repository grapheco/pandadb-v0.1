@echo off
set base=..
set config=%base%/conf/neo4j.conf
for /f "tokens=1,2 delims==" %%a in (%config%) do (
  if "%%a"=="dbms.connector.http.listen_address" set port=%%b
)
set port=%port:~-4%

for /f "usebackq tokens=1-5" %%a in (`netstat -ano ^| findstr %port%`) do (
	if [%%d] EQU [LISTENING] (
		set pid=%%e
	)
)

for /f "usebackq tokens=1-5" %%a in (`tasklist ^| findstr %pid%`) do (
	set image_name=%%a
)

echo now will kill process : pid %pid%, Pandadb %image_name%

pause
rem 根据进程ID，kill进程
taskkill /f /pid %pid%
pause