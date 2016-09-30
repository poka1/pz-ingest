#!/bin/bash

sh "echo scanning" 
# Fortify Scan
sh "env"
sh "/opt/hp_fortify_sca/bin/sourceanalyzer -b 000xxx111" "../src/main/" 

