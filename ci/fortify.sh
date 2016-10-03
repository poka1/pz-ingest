#!/bin/bash

pushd `dirname $0`/.. > /dev/null
root=$(pwd -P)
echo scanning 
# Fortify Scan
echo 'Building' $BUILD_NUMBER
/opt/hp_fortify_sca/bin/sourceanalyzer -b $BUILD_NUMBER $root/src/main/java/ingest/Application.java
/opt/hp_fortify_sca/bin/sourceanalyzer -b $BUILD_NUMBER -scan -f fortifyResults-$BUILD_NUMBER.fpr 
cat fortifyResults-$BUILD_NUMBER.fpr 
#sh "/bin/curl -v --insecure -H 'Accept: application/json' -X POST --form file=@fortifyResults-${env.BUILD_NUMBER}.fpr https://threadfix.devops.geointservices.io/rest/applications/1/upload?apiKey=${threadfixApiKey}"

popd > /dev/null

