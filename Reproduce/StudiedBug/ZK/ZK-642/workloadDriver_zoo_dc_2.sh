#!/bin/bash

# trial count
count=$1
id=$2


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
        # internal checker
        ./bin/zkServer.sh status > status.log
        internalContent="$(grep -r "follower\|leader" status.log)"
        internalCount="$(grep -r "follower\|leader" status.log|wc -l)"
        if [ "$internalCount" -ne "0" ]; then
            error "has log level at $1"
            echo "$logContent" >> result.txt
            error "test has failed:"
            error "differential observability"
            # java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 5
            printf "$failedNum\n" >> result.txt
            failedNum=$[failedNum+1]
#            exit 0
        fi
    fi
}

function assertNodeNoBadFailure {
    ./bin/zkTxnLogToolkit.sh logs/version-2/* > system.log
    cat logs/*.out >> system.log
    assertNodeNoLogAtLevel ERROR
    assertNodeNoLogAtLevel FATAL
    assertNodeNoLogAtLevel WARN
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
        # java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 5
        printf "$failedNum\n" >> result.txt
        failedNum=$[failedNum+1]
#        exit 0
    fi
}


# conduct trial
function experiment {
    # ./bin/workload_seq.sh
    # ./bin/zkChecker.sh | grep "Data" | cut -d " " -f 4 > stand.txt
    printf "Start experiment!\n"
    for((i=0;i<$count;i++));
    do
        printf "The $i trial!\n"
        ./bin/zkServer.sh start
        sleep 5s
        ./bin/workload_dc2.sh
        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 1 $id
        sleep 2s
        cli_mt 172.30.0.2:9876
        # checker
        assertNodeNoBadFailure
        assertExceptionFailure

        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 2 $id
        ./bin/zkServer.sh stop
#        rm -rf logs/*
#        rm -rf data/version-2
        rm -rf status.log
 #       rm -rf system.log
#        rm -rf result.txt
    done
    # check hang
    java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 0 $id
    
}
# clienthang
experiment
# printf "$count $2\n"
