#!/bin/bash

# trial count
count=$1
id=$2
# check diff
function checkDiff {
    if test -s diff.txt; then
        echo "diff"
        # exit 0
    else
        echo "same"
    fi
}

function error {
    printf "[error] " >> result.txt
    printf "$@" >> result.txt
    printf "\n" >> result.txt
}

# search log 
function assertNodeNoLogAtLevel {
    logContent="$(grep -r "$1" system.log|grep -v "connectToLeader exceeded on retries"|grep -v "Failed connect to"|grep -v "Unexpected exception, connectToLeader exceeded"|grep -v "Failed to send last message. Shutting down thread")"
    logCount="$(grep -r "$1" system.log|grep -v "connectToLeader exceeded on retries"|grep -v "Failed connect to"|grep -v "Unexpected exception, connectToLeader exceeded"|grep -v "Failed to send last message. Shutting down thread"|wc -l)"
    if [ "$logCount" -ne "0" ]; then
        ./bin/zkServer.sh status > status.log
        internalContent="$(grep -r "follower\|leader" status.log)"
        internalCount="$(grep -r "follower\|leader" status.log|wc -l)"
        if [ "$internalCount" -ne "0" ]; then
            error "has log level at $1"
            echo "$logContent" >> result.txt
            error "test has failed:"
            error "differential observability"
            printf "$2\n" >> result.txt
            #exit 0
        fi
    fi
}

function assertNodeNoBadFailure {
    ./bin/zkTxnLogToolkit.sh logs/version-2/* > system.log
    cat logs/*.out >> system.log
    assertNodeNoLogAtLevel ERROR $1
    assertNodeNoLogAtLevel FATAL $1
    # assertNodeNoLogAtLevel WARN
}

function assertExceptionFailure {
    exceptions=$( grep -nr "Exception" $(find ./logs/ -name "*.out") | \
    grep -v "ConnectException" | \
    grep -v "ConnectionClosedException" | \
    grep -v "ConnectTimeoutException" | \
    grep -v "Failed get of master address: java.io.IOException" | \
    grep -v "EOFException" | \
    grep -v "Exception.<init>" | \
    grep -v "CallTimeoutException" | \
    grep -v "EndOfStreamException" | \
    grep -v "IOException.<init>" | \
    grep -v "SocketException" | \
    grep -v "InterruptedException" | \
    grep -v "Exception when using channel" | \
    grep -v "IOException" | \
    grep -v "Exception when following the leader" | \
    grep -v "SocketTimeoutException" | \
    grep -v "Exception while shutting down" | \
    grep -v "reading or writing challenge" | \
    # grep -v "ClosedChannelException"
    # grep -v "CancelledKeyException" 
    # grep -v "Exception while sending packets in LearnHandler"
    grep -v "InstanceAlreadyExistsException" | \
    grep -v "NoRouteToHostException" )

    if [ "$exceptions" != "" ];then
        echo $exceptions >> result.txt
        printf "uncommon exception!\n" >> result.txt
        printf "$1\n" >> result.txt
        #exit 0
    fi
}

function clean {
    dataDir=/apache-zookeeper-3.10.0-SNAPSHOT-bin/data/version-2
    dataLogDir=/apache-zookeeper-3.10.0-SNAPSHOT-bin/logs/version-2
    # count=3;
    ls -t $dataDir/snapshot.*|tail -n +4|xargs rm -rf
    ls -t $dataLogDir/log.*|tail -n +4|xargs rm -rf
}

# conduct trial
function experiment {
    printf "Start experiment!\n"
    for((i=0;i<$count;i++));
    do
        printf "The $i trial!\n"
        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 1 $id
        ./bin/zkServer.sh start
        sleep 3
        ./bin/workload_seq.sh -timeout 15000
        
        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 4 $id
        
        # check 
        assertNodeNoBadFailure $i
        assertExceptionFailure $i

        # batch bug report

        # check data corruption
        # ./bin/zkChecker.sh | grep "Data" | cut -d " " -f 4 >res.txt
        # diff stand.txt res.txt > diff.txt
        # checkDiff
        
        # check data inconsistency 
        # compare res.txt 

        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 3 $id
        
        ./bin/zkServer.sh stop
        rm -rf data/version-2
        rm -rf logs/*
        # rm -rf system.log
        mv system.log system_$i.log
        # ./bin/zkServer.sh start

        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 2 $id
    done
    # check hang
    java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 0 $id
}
experiment
