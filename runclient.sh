#!/bin/sh
#
# Copyright 2017 Jari Hämäläinen / https://github.com/nuumio
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

SERVER=localhost
CLIENT=client
GROUP=group
GROUPTOKEN=groupToken
SYNCPOINT=syncPoint
MEMBERS=2

if [ "XXX${1}" != "XXX" ]
then
    SERVER=${1}
fi
if [ "XXX${2}" != "XXX" ]
then
    CLIENT=${2}
fi
if [ "XXX${3}" != "XXX" ]
then
    GROUP=${3}
fi
if [ "XXX${4}" != "XXX" ]
then
    GROUPTOKEN=${4}
fi
if [ "XXX${5}" != "XXX" ]
then
    SYNCPOINT=${5}
fi
if [ "XXX${6}" != "XXX" ]
then
    MEMBERS=${6}
fi

CP=`find build/libs/ -maxdepth 1 -name netsync*.jar`
CP=${CP}:`find locallibs/ -maxdepth 1 -name commons-cli-*.jar`
java -cp ${CP} fi.nuumio.netsync.NetSyncClientExample ${SERVER} ${CLIENT} ${GROUP} ${GROUPTOKEN} ${SYNCPOINT} ${MEMBERS}
