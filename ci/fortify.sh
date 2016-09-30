#!/bin/bash

pushd `dirname $0`/.. > /dev/null
root=$(pwd -P)
echo scanning 
# Fortify Scan
echo 'Building' $BUILD_NUMBER
/opt/hp_fortify_sca/bin/sourceanalyzer -b $BUILD_NUMBER $root/src

popd > /dev/null

