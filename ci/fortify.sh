#!/bin/bash

pushd `dirname $0`/.. > /dev/null
root=$(pwd -P)
echo scanning 
# Fortify Scan
echo 'Building' $BUILD_NUMBER
##/opt/hp_fortify_sca/bin/sourceanalyzer -b $BUILD_NUMBER $root/src/main/java/ingest/*.java
#/opt/hp_fortify_sca/bin/sourceanalyzer -b $BUILD_NUMBER -scan -f fortifyResults-$BUILD_NUMBER.fpr 
/jslave/tools/hudson.tasks.Maven_MavenInstallation/M3/bin/mvn install:install-file -Dfile=$root/pom.xml -DpomFile=$root/pom.xml
ls /jslave/workspace/venice/piazza/pz-ingest/9-fortify/sca-maven-plugin
#/jslave/tools/hudson.tasks.Maven_MavenInstallation/M3/bin/mvn install:install-file -Dfile=sca-maven-plugin/sca-maven-plugin-16.10.jar -DpomFile=sca-maven-plugin/pom.xml

#cat fortifyResults-$BUILD_NUMBER.fpr 
#sh "/bin/curl -v --insecure -H 'Accept: application/json' -X POST --form file=@fortifyResults-${env.BUILD_NUMBER}.fpr https://threadfix.devops.geointservices.io/rest/applications/1/upload?apiKey=${threadfixApiKey}"

popd > /dev/null



