#!/bin/bash

# trial count
count=$1
id=$2
# conduct trial
function experiment {
    printf "Start experiment!\n"
    for((i=0;i<$count;i++));
    do
        printf "The $i trial!\n"
        ./bin/workload_seq.sh 
    done
    # check hang
    java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 0 $id

}
experiment