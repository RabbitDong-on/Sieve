#!/bin/bash

sysName=$1
Num=$2

function genSeqWorkload {
    cp zkCli.sh workload_seq.sh
    python genWorkload.py $sysName $Num 0 >> workload_seq.sh
    chmod +x workload_seq.sh
}
# gen workload for each op
function genDCWorkload {
    # printf "\$CMD<<EOF\n" >> workload_dc.sh
    for ((i=1;i<=5;i++))
    do
        cp zkCli.sh workload_dc$i.sh  
        python genWorkload.py $sysName $Num $i >> workload_dc$i.sh
        chmod +x workload_dc$i.sh
    done
    # printf "EOF\n" >> workload_dc.sh
}
genSeqWorkload
genDCWorkload

