#!/bin/bash

sh "echo scanning" 
# Fortify Scan
sh "echo 'Building $BUILD_NUMBER'"
sh "/opt/hp_fortify_sca/bin/sourceanalyzer -b $BUILD_NUMBER" "../src/main/" 

