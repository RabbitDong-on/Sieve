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
    logContent="$(grep -r "$1" system.log)"
    logCount="$(grep -r "$1" system.log|wc -l)"
    if [ "$logCount" -ne "0" ]; then
        /apache-zookeeper-3.7.2-bin/bin/zkCli.sh ls /brokers/ids|grep "\[1, 2, 3\]" > status.log
        internalContent="$(grep -r "follower\|leader" status.log)"
        internalCount="$(grep -r "follower\|leader" status.log|wc -l)"
        if [ "$internalCount" -ne "0" ]; then
            error "has log level at $1"
            echo "$logContent" >> result.txt
            error "test has failed:"
            error "differential observability"
            printf "$2\n" >> result.txt
            # exit 0
        fi
    fi
}

function assertNodeNoBadFailure {
    cat kafka/logs/*.out >> system.log
    assertNodeNoLogAtLevel ERROR $1
    assertNodeNoLogAtLevel FATAL $1
    # assertNodeNoLogAtLevel WARN
}

function assertExceptionFailure {
    exceptions=$( grep -nr "Exception" $(find ./kafka/logs/ -name "*") | \
    grep -v "ConnectException" | \
    grep -v "ConnectionClosedException" | \
    grep -v "ConnectTimeoutException" | \
    grep -v "Failed get of master address: java.io.IOException" | \
    grep -v "EOFException" | \
    grep -v "Exception.<init>" | \
    grep -v "CallTimeoutException" | \
    grep -v "IOException.<init>" | \
    grep -v "SocketException" | \
    grep -v "InterruptedException" | \
    grep -v "Exception when using channel" | \
    grep -v "IOException" | \
    grep -v "Exception when following the leader" | \
    grep -v "SocketTimeoutException" | \
    # grep -v "Exception while shutting down" | \
    # grep -v "reading or writing challenge" | \
    grep -v "ClosedChannelException" | \
    grep -v "ControllerMovedException" | \
    grep -v "MemberIdRequiredException" | \
    grep -v "InvalidReplicationFactorException" | \
    # grep -v "CancelledKeyException" 
    # grep -v "Exception while sending packets in LearnHandler"
    # grep -v "InstanceAlreadyExistsException" | \
    grep -v "NoRouteToHostException" )

    if [ "$exceptions" != "" ];then
        echo $exceptions >> result.txt
        printf "uncommon exception!\n" >> result.txt
        printf "$1\n" >> result.txt
        # exit 0
    fi
}

# conduct trial
function experiment {
    printf "Start experiment!\n"
    for((i=0;i<$count;i++));
    do
        printf "The $i trial!\n"
        cd /apache-zookeeper-3.7.2-bin
        ./bin/zkServer.sh start
        cd /failslow
        sleep 2
        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 1 $id
        ./kafka/bin/kafka-server-start.sh -daemon kafka/config/server.properties
        sleep 2
        # create topic
        # ./kafka/bin/kafka-topics.sh --create --bootstrap-server 172.30.0.2:9092,172.30.0.3:9092,172.30.0.4:9092 --replication-factor 2 --partitions 1 --topic test
        # lookup
        # ./kafka/bin/kafka-topics.sh --list --bootstrap-server 172.30.0.2:9092,172.30.0.3:9092,172.30.0.4:9092
        # delete
        # ./kafka/bin/kafka-topics.sh --delete --bootstrap-server 172.30.0.2:9092,172.30.0.3:9092,172.30.0.4:9092 --topic test
        
        
        ./kafka/bin/kafka-producer-perf-test.sh --topic test --num-records 500000 --throughput -1 --record-size 1000 --producer-props bootstrap.servers=172.30.0.2:9092,172.30.0.3:9092,172.30.0.4:9092 acks=1
        ./kafka/bin/kafka-consumer-perf-test.sh --broker-list 172.30.0.2:9092,172.30.0.3:9092,172.30.0.4:9092 --topic test --fetch-size 1048576 --messages 500000 

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
        
        mv system.log system_$i.log
        mv result.txt result_$i.txt
        mv status.log status_$i.log
        ./cleanKafka.sh


        # wait for clean kafka
        c3=$(jps|grep "Kafka"|wc -l)
        while [ $c3 != 0 ]
        do
            c3=$(jps|grep "Kafka"|wc -l)
        done
        java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 2 $id
    done
    # check hang
    java -cp .:/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar client.FailSlowAgent 0 $id
}
experiment
# printf "$count $2\n"
