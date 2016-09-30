#!/bin/bash

sh "echo scanning" 
# Fortify Scan
sh "/opt/hp_fortify_sca/bin/sourceanalyzer -b 000xxx111" "../src/main/" 

