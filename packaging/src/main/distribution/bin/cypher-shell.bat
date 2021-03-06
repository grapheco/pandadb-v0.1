@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  cypher-shell startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set NEO4J_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and CYPHER_SHELL_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=
@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

SETLOCAL EnableDelayedExpansion

set MAIN_CLASS="org.neo4j.shell.Main"
set batchfile=%0
set full=%~f0
setlocal
  for %%d in (%~dp0.) do set Directory=%%~fd
  set Directory=%Directory%
  for %%d in (%~dp0..) do set ParentDirectory=%%~fd
  set ParentDirectory=%ParentDirectory%
  set CYPHER_SHELL_JAR=%ParentDirectory%\lib\pandadb-dist-0.1.0-SNAPSHOT.jar
@rem Execute cypher-shell
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% -cp "%CYPHER_SHELL_JAR%" %MAIN_CLASS%
endlocal
:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable CYPHER_SHELL_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%CYPHER_SHELL_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
