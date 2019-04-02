#!/usr/bin/env bash 

DEPLOYMENT=$1

dir=`date +%s`
mkdir -p logs/$dir
for p in $(oc get pods | grep ^${DEPLOYMENT}- | cut -f 1 -d ' '); do 
    echo --------------------------- 
    echo $p 
    echo --------------------------- 
    oc logs $p > "logs/${dir}/${p}.logs"
done
