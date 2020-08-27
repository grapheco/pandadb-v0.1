@echo off
echo batchfile=%0
echo full=%~f0
setlocal
  for %%d in (%~dp0.) do set Directory=%%~fd
  echo Directory=%Directory%
  for %%d in (%~dp0..) do set ParentDirectory=%%~fd
  echo ParentDirectory=%ParentDirectory%
  set class_path=%ParentDirectory%\lib\pandadb-dist-0.1.0-SNAPSHOT.jar
  set MainClass="cn.pandadb.server.PandaServerStarter"
  set config=%ParentDirectory%\conf\neo4j.conf
  set log_path=%ParentDirectory%\logs\neo4j.log

echo Starting Pandadb...

for /f "tokens=1,2 delims==" %%a in (%config%) do (
  if "%%a"=="dbms.connector.bolt.listen_address" set bolt=%%b
)
echo Bolt enabled on %bolt%
echo Started

for /f "tokens=1,2 delims==" %%a in (%config%) do (
  if "%%a"=="dbms.connector.http.listen_address" set http=%%b
)
echo Remote interface available at %http%
echo There may be a short delay until the server is ready.

start java -jar %class_path% %ParentDirectory% %config%
endlocal
Pause
exit
