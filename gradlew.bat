@rem Gradle wrapper
@echo off
set DIR=%~dp0
if not defined JAVA_HOME goto noJavaHome
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
goto execute
:noJavaHome
set JAVA_EXE=java.exe
:execute
"%JAVA_EXE%" -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
