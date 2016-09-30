#!/bin/bash

echo scanning 
# Fortify Scan
echo 'Building $BUILD_NUMBER'
/opt/hp_fortify_sca/bin/sourceanalyzer -b $BUILD_NUMBER ../src/main/

