#!/usr/bin/env bash
set -e

echo "`date` sync env"
cd /opt/build/depot/demo-env-project
git fetch origin -p
git pull

echo "`date` deploy web"
cd /opt/build/depot/demo-env-project/demo-env
/opt/build/cmn/bin/cmn deploy --id=web