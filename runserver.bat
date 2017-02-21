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

for %%a in (build\libs\netsync*.jar) do set CP=%%a
for %%a in (locallibs\*.jar) do set CP=%CP%;%%a
java -cp %CP% fi.nuumio.netsync.NetSyncServer
