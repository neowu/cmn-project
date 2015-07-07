#!/usr/bin/env bash
set -e

echo "`date` sync code"
cd /opt/build/depot/core-project
git fetch origin -p
git pull

echo "`date` build war"
./gradlew -Penv=qa clean build

echo "`date` sync env"
cd /opt/build/depot/demo-env-project
git fetch origin -p
git pull

echo "`date` copy war file"
mkdir -p /opt/build/depot/demo-env-project/demo-env/packages/web
cp /opt/build/depot/demo-project/build/demo-service/libs/demo-service.war /opt/build/depot/demo-env-project/demo-env/packages/web

echo "`date` bake ami"
cd /opt/build/depot/demo-env-project/demo-env
/opt/build/cmn/bin/cmn bake --id=web
