@echo off
REM
REM Copyright 2017 Jari Hämäläinen / https://github.com/nuumio
REM
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM
REM     http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM

IF "%~1" NEQ "" (
    set SERVER=%~1
) else (
    set SERVER=localhost
)
IF "%~2" NEQ "" (
    set CLIENT=%~2
) else (
    set CLIENT=client
)
IF "%~3" NEQ "" (
    set GROUP=%~3
) else (
    set GROUP=group
)
IF "%~4" NEQ "" (
    set GROUPTOKEN=%~4
) else (
    set GROUPTOKEN=groupToken
)
IF "%~5" NEQ "" (
    set SYNCPOINT=%~5
) else (
    set SYNCPOINT=syncPoint
)
IF "%~6" NEQ "" (
    set MEMBERS=%~6
) else (
    set MEMBERS=2
)

for %%a in (build\libs\netsync*.jar) do set CP=%%a
for %%a in (locallibs\*.jar) do set CP=%CP%;%%a
java -cp %CP% fi.nuumio.netsync.NetSyncClientExample %SERVER% %CLIENT% %GROUP% %GROUPTOKEN% %SYNCPOINT% %MEMBERS%
