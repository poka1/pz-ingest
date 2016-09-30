#!/bin/bash

sh "echo scanning" 
# Fortify Scan
sh "/opt/hp_fortify_sca/bin/sourceanalyzer -b ${env.BUILD_NUMBER} "../src/main/" 
#sh "/opt/hp_fortify_sca/bin/sourceanalyzer -b ${env.BUILD_NUMBER}  -scan -f fortifyResults-${env.BUILD_NUMBER}.fpr" 

#sh "/bin/curl -v --insecure -H 'Accept: application/json' -X POST --form file=@fortifyResults-${env.BUILD_NUMBER}.fpr https://threadfix.devops.geointservices.io/rest/applications/1/upload?apiKey=${threadfixApiKey}"
