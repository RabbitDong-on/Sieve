#!/bin/bash
count=$1
id=$2

function error {
    printf "[error] " >> result.txt
    printf "$@" >> result.txt
    printf "\n" >> result.txt
}
# search log 
function assertNodeNoLogAtLevel {
    logContent="$(grep -r "$1" /hadoop/hadoop-3.3.6/logs/*.log)"
    logCount="$(grep -r "$1" /hadoop/hadoop-3.3.6/logs/*.log|wc -l)"
    if [ "$logCount" -ne "0" ]; then
        hdfs dfsamdin -report > status.log
        internalContent="$(grep -r "Normal" status.log)"
        internalCount="$(grep -r "Normal" status.log|wc -l)"
        if [ "$internalCount" -ne "3" ]; then
            error "has log level at $1"
            echo "$logContent" >>result.txt
            error "test has failed:"
            error "differential observability"
            exit 0
        fi
    fi
}

function assertNodeNoBadFailure {
    assertNodeNoLogAtLevel ERROR
    assertNodeNoLogAtLevel FATAL
    # assertNodeNoLogAtLevel WARN
}

function assertExceptionFailure {
    exceptions=$( grep -nr "Exception" /hadoop/hadoop-3.3.6/logs/*.log | \
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
    # grep -v "InterruptedException" | \
    grep -v "Exception when using channel" | \
    grep -v "IOException" | \
    grep -v "HttpGetFailedException" | \
    # grep -v "Exception when following the leader" | \
    grep -v "SocketTimeoutException" | \
    # grep -v "Exception while shutting down" | \
    # grep -v "reading or writing challenge" | \
    grep -v "ClosedChannelException" | \
    # grep -v "CancelledKeyException" 
    # grep -v "Exception while sending packets in LearnHandler"
    # grep -v "InstanceAlreadyExistsException" | \
    grep -v "NoRouteToHostException" )

    if [ "$exceptions" != "" ];then
        echo $exceptions >> result.txt
        printf "uncommon exception!\n" >> result.txt
        # java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 5
        # printf "$failedNum\n" >> result.txt
        # failedNum=$[failedNum+1]
        exit 0
    fi
}
# hdfs --daemon start journalnode
# hdfs zkfc -formatZK
# hdfs --daemon start zkfc
# 
# conduct trial
function experiment {
    printf "Start experiment!\n"
    for((i=0;i<$count;i++));
    do
        printf "The $i trial!\n"
        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 1 $id
        # hdfs namenode -format
        # hdfs --daemon start namenode
        # hdfs --daemon start datanode
        ./hdfsworkload.sh
        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 4 $id
        # check log
        assertNodeNoBadFailure
        assertExceptionFailure
        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 3 $id
        ./cleanHDFS.sh
        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 2 $id
    done
    # check hang
    java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 0 $id
}
experiment