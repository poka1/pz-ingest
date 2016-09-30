#!/bin/bash

sh "echo scanning" 
# Fortify Scan
sh "env"
sh "/opt/hp_fortify_sca/bin/sourceanalyzer -b $BUILD_NUMBER" "../src/main/" 

