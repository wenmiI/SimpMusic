@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  simpmusic-sync-host startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables, and ensure extensions are enabled
setlocal EnableExtensions

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and SIMPMUSIC_SYNC_HOST_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

1>&2 echo.
1>&2 echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
1>&2 echo.
1>&2 echo Please set the JAVA_HOME variable in your environment to match the
1>&2 echo location of your Java installation.

"%COMSPEC%" /c exit 1
goto exitWithErrorLevel

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

1>&2 echo.
1>&2 echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
1>&2 echo.
1>&2 echo Please set the JAVA_HOME variable in your environment to match the
1>&2 echo location of your Java installation.

"%COMSPEC%" /c exit 1
goto exitWithErrorLevel

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\syncServer.jar;%APP_HOME%\lib\data-jvm.jar;%APP_HOME%\lib\loginSync-jvm.jar;%APP_HOME%\lib\aiService-jvm.jar;%APP_HOME%\lib\lyricsService-jvm.jar;%APP_HOME%\lib\kotlinYtmusicScraper-jvm.jar;%APP_HOME%\lib\kizzy-jvm.jar;%APP_HOME%\lib\discord-jvm.jar;%APP_HOME%\lib\media-jvm.jar;%APP_HOME%\lib\domain-jvm.jar;%APP_HOME%\lib\autoEqService-jvm.jar;%APP_HOME%\lib\spotify-jvm.jar;%APP_HOME%\lib\listenTogether-jvm.jar;%APP_HOME%\lib\lastfm-empty-jvm.jar;%APP_HOME%\lib\common-jvm.jar;%APP_HOME%\lib\nowplayingcenter-0.0.3.jar;%APP_HOME%\lib\ktorExt-jvm.jar;%APP_HOME%\lib\ktor-server-cio-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-server-partial-content-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-server-conditional-headers-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-server-core-jvm-3.6.0.jar;%APP_HOME%\lib\kotlin-reflect-2.4.21.jar;%APP_HOME%\lib\kotlin-test-2.4.21.jar;%APP_HOME%\lib\kotlin-onetimepassword-3.0.0.jar;%APP_HOME%\lib\room-common-jvm-2.8.5.jar;%APP_HOME%\lib\room-runtime-jvm-2.8.5.jar;%APP_HOME%\lib\room-migration-jvm-2.8.5.jar;%APP_HOME%\lib\openai-client-jvm.jar;%APP_HOME%\lib\ktor-serialization-kotlinx-json-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-serialization-kotlinx-protobuf-jvm-3.6.0.jar;%APP_HOME%\lib\kotlinx-serialization-protobuf-jvm-1.12.0.jar;%APP_HOME%\lib\uri-jvm-0.0.21.jar;%APP_HOME%\lib\ktor-serialization-kotlinx-xml-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-serialization-kotlinx-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-openapi-schema-jvm-3.6.0.jar;%APP_HOME%\lib\kotaml-jvm-0.113.0.jar;%APP_HOME%\lib\ktor-client-okhttp-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-client-encoding-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-client-content-negotiation-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-client-logging-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-client-websockets-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-client-cio-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-client-auth-jvm-3.0.0.jar;%APP_HOME%\lib\ktor-client-core-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-http-cio-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-websocket-serialization-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-serialization-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-websockets-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-network-tls-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-http-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-events-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-network-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-sse-jvm-3.6.0.jar;%APP_HOME%\lib\ktor-utils-jvm-3.6.0.jar;%APP_HOME%\lib\openai-core-jvm.jar;%APP_HOME%\lib\serialization-jvm-0.91.3.jar;%APP_HOME%\lib\kotlinx-serialization-json-io-jvm-1.12.0.jar;%APP_HOME%\lib\core-jvmcommon-0.91.3.jar;%APP_HOME%\lib\kotlinx-serialization-core-jvm-1.12.0.jar;%APP_HOME%\lib\kotlinx-serialization-json-jvm-1.12.0.jar;%APP_HOME%\lib\datastore-preferences-external-protobuf-1.3.0.jar;%APP_HOME%\lib\datastore-core-okio-jvm-1.3.0.jar;%APP_HOME%\lib\datastore-core-jvm-1.3.0.jar;%APP_HOME%\lib\datastore-preferences-proto-1.3.0.jar;%APP_HOME%\lib\datastore-preferences-core-jvm-1.3.0.jar;%APP_HOME%\lib\datastore-jvm-1.3.0.jar;%APP_HOME%\lib\datastore-preferences-jvm-1.3.0.jar;%APP_HOME%\lib\sqlite-jvm-2.7.1.jar;%APP_HOME%\lib\sqlite-bundled-jvm-2.7.1.jar;%APP_HOME%\lib\kotlinx-coroutines-slf4j-1.11.0.jar;%APP_HOME%\lib\kermit-io-jvm-2.2.0.jar;%APP_HOME%\lib\paging-common-desktop-3.5.3.jar;%APP_HOME%\lib\quickjs-jvm.jar;%APP_HOME%\lib\ktor-io-jvm-3.6.0.jar;%APP_HOME%\lib\kotlinx-coroutines-core-jvm-1.11.0.jar;%APP_HOME%\lib\kotlinx-coroutines-swing-1.11.0.jar;%APP_HOME%\lib\nucleus.notification-common-2.5.18.jar;%APP_HOME%\lib\koin-core-jvm-4.2.2.jar;%APP_HOME%\lib\kermit-jvm-2.2.0.jar;%APP_HOME%\lib\PipePipeExtractor-27aac1e14e.jar;%APP_HOME%\lib\PipePipeExtractor-27aac1e14e.jar;%APP_HOME%\lib\extractor-27aac1e14e.jar;%APP_HOME%\lib\okhttp-jvm-5.5.1.jar;%APP_HOME%\lib\snakeyaml-engine-kmp-jvm-5.0.0.jar;%APP_HOME%\lib\BravePipeExtractor-105aad96c7.jar;%APP_HOME%\lib\wire-runtime-jvm-6.4.1.jar;%APP_HOME%\lib\okio-jvm-3.18.2.jar;%APP_HOME%\lib\ksoup-html-jvm-0.6.0.jar;%APP_HOME%\lib\ksoup-entities-jvm-0.6.0.jar;%APP_HOME%\lib\atomicfu-jvm-0.33.0.jar;%APP_HOME%\lib\nucleus.notification-linux-2.5.18.jar;%APP_HOME%\lib\nucleus.notification-windows-2.5.18.jar;%APP_HOME%\lib\nucleus.notification-macos-2.5.18.jar;%APP_HOME%\lib\nucleus.core-runtime-2.5.18.jar;%APP_HOME%\lib\nucleus.freedesktop-icons-2.5.18.jar;%APP_HOME%\lib\core-common-2.2.0.jar;%APP_HOME%\lib\collection-jvm-1.5.0.jar;%APP_HOME%\lib\annotation-jvm-1.9.1.jar;%APP_HOME%\lib\stately-concurrent-collections-jvm-2.1.0.jar;%APP_HOME%\lib\stately-concurrency-jvm-2.1.0.jar;%APP_HOME%\lib\kermit-core-jvm-2.2.0.jar;%APP_HOME%\lib\stately-strict-jvm-2.1.0.jar;%APP_HOME%\lib\urlencoder-lib-jvm-1.6.0.jar;%APP_HOME%\lib\kotlinx-io-core-jvm-0.9.1.jar;%APP_HOME%\lib\kotlinx-datetime-jvm-0.8.0-0.6.x-compat.jar;%APP_HOME%\lib\kotlinx-io-bytestring-jvm-0.9.1.jar;%APP_HOME%\lib\kotlin-stdlib-2.4.21.jar;%APP_HOME%\lib\annotations-23.0.0.jar;%APP_HOME%\lib\jna-platform-5.19.1.jar;%APP_HOME%\lib\jmtc-0.0.3.jar;%APP_HOME%\lib\jna-5.19.1.jar;%APP_HOME%\lib\kuromoji-ipadic-0.9.0.jar;%APP_HOME%\lib\pinyin4j-2.5.1.jar;%APP_HOME%\lib\commons-codec-1.23.0.jar;%APP_HOME%\lib\slf4j-android-1.7.36.jar;%APP_HOME%\lib\dec-0.1.2.jar;%APP_HOME%\lib\kuromoji-core-0.9.0.jar;%APP_HOME%\lib\dbus-java-transport-native-unixsocket-5.0.0.jar;%APP_HOME%\lib\dbus-java-core-5.0.0.jar;%APP_HOME%\lib\Java-WebSocket-1.6.0.jar;%APP_HOME%\lib\slf4j-api-2.0.19.jar;%APP_HOME%\lib\timeago-parser-27aac1e14e.jar;%APP_HOME%\lib\nanojson-c7a6c1c08d16b6d5ecded34758e6415e07be2166.jar;%APP_HOME%\lib\jsoup-1.23.2.jar;%APP_HOME%\lib\spotbugs-annotations-4.10.2.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\protobuf-javalite-4.36.2.jar;%APP_HOME%\lib\rhino-engine-1.8.1.jar;%APP_HOME%\lib\rhino-1.8.1.jar;%APP_HOME%\lib\hlsdownloader-1.0.0.jar;%APP_HOME%\lib\config-1.4.9.jar;%APP_HOME%\lib\autolink-0.12.0.jar;%APP_HOME%\lib\commons-lang3-3.20.0.jar;%APP_HOME%\lib\bcprov-jdk18on-1.85.jar;%APP_HOME%\lib\fastdoubleparser-2.0.1.jar


@rem Execute simpmusic-sync-host
@rem endlocal doesn't take effect until after the line is parsed and variables are expanded
@rem which allows us to clear the local environment before executing the java command
endlocal & "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %SIMPMUSIC_SYNC_HOST_OPTS%  -classpath "%CLASSPATH%" com.maxrave.simpmusic.syncserver.MainKt %* & call :exitWithErrorLevel & goto exitWithErrorLevel

@rem This label must not be changed. We rely on old scripts being able to jump to this point.
:exitWithErrorLevel
@rem Use "%COMSPEC%" /c exit to allow operators to work properly in scripts
"%COMSPEC%" /c exit %ERRORLEVEL%
