#!/bin/bash

function error {
    printf "[checker error] "
    printf "$@"
    printf "\n"
}

# search log 
function assertNodeNoLogAtLevel {
    logContent="$(grep -r "$1" system.log)"
    logCount="$(grep -r "$1" system.log|wc -l)"
    if [ "$logCount" -ne "0" ]; then
        error "has log level at $1"
        echo "$logContent"
        error "test has failed:"
        exit 0
    fi
}

function assertNodeNoBadFailure {
    assertNodeNoLogAtLevel ERROR
    assertNodeNoLogAtLevel FATAL
    assertNodeNoLogAtLevel WARN
}


function assertExceptionFailure {
    exceptions=$( grep -nr "Exception" $(find ./logs/ -name "*.out") | \
    grep -v "ConnectException" | grep -v "ConnectionClosedException" | \
    grep -v "ConnectTimeoutException" | \
    grep -v "Failed get of master address: java.io.IOException" | \
    grep -v "EOFException" | grep -v "Exception.<init>" | \
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
    grep -v "NoRouteToHostException")

    if [ "$exceptions" != "" ];then
        echo $exceptions
        printf "[checker error] uncommon exception!\n"
        exit 0
    fi
}

./bin/zkTxnLogToolkit.sh logs/version-2/* > system.log
assertNodeNoBadFailure 
assertExceptionFailure