#!/bin/bash

sh "echo scanning" 
# Fortify Scan
sh "/opt/hp_fortify_sca/bin/sourceanalyzer -b ${env.BUILD_NUMBER}" "../src/main/" 

