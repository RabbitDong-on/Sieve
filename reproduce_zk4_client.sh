#!/bin/bash

id=$1

cp /failslow/Reproduce/DetectedBug/ZK-4/workload_dc* bin/
chmod +x bin/*.sh
cp /failslow/Reproduce/DetectedBug/ZK-4/workloadDriver_zoo_dc_* .
chmod +x workloadDriver_zoo_dc_*
if [ $id -eq 1 ]; then
    ./workloadDriver_zoo_dc_1.sh 1 1
elif [ $id -eq 2 ]; then
    ./workloadDriver_zoo_dc_2.sh 1 2
elif [ $id -eq 3 ]; then
    ./workloadDriver_zoo_dc_3.sh 1 3
fi
