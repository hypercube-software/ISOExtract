@echo on

set JAVA_HOME=C:\Java\graalvm-ce-java17-22.3.0

rem x64 Native Tools Command Prompt for VS 2019
call "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat"

mvnw -Pnative clean install native:compile


