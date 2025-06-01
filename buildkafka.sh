#!/bin/bash

#!/bin/bash
option=$1
testOption=$2

function configEnv {
# /failslow
cp Kafka_Config/profile /etc
id="$(cat myid)"
if [ "$id" -eq "1" ]; then
    rm -rf kafka/config/server.properties
    cp Kafka_Config/server1.properties kafka/config/server.properties
elif [ "$id" -eq "2" ]; then
    rm -rf kafka/config/server.properties
    cp Kafka_Config/server2.properties kafka/config/server.properties
elif [ "$id" -eq "3" ]; then
    rm -rf kafka/config/server.properties
    cp Kafka_Config/server3.properties kafka/config/server.properties
fi
}
# instrument
function instrument {
# pwd /failslow
cd /
wget https://services.gradle.org/distributions/gradle-8.2.1-bin.zip
apt-get -y install unzip
unzip gradle-8.2.1-bin.zip
cd /failslow/kafka
gradle compileJava
cd /failslow
# pwd /failslow
cat myid|xargs ./instrument.sh kafka $testOption
cd /failslow/kafka
gradle assemble -x compileJava -x compileScala
./bin/kafka-server-start.sh -daemon config/server.properties
}

# build zookeeper
function buildzoo {
# /failslow
cd /
wget https://archive.apache.org/dist/zookeeper/zookeeper-3.7.2/apache-zookeeper-3.7.2-bin.tar.gz
tar zxvf apache-zookeeper-3.7.2-bin.tar.gz 
cd /apache-zookeeper-3.7.2-bin 
cp /failslow/Kafka_Config/zoo.cfg conf/zoo.cfg
mkdir data
cp /failslow/myid data/
./bin/zkServer.sh start
}

function mvjson2engine {
    docker cp failslow1:/failslow/kafka1.json ./
    docker cp kafka1.json failslow4:/failslow
    docker cp failslow1:/failslow/kafka1TOP2IO.json ./
    docker cp kafka1TOP2IO.json failslow4:/failslow

    docker cp failslow1:/failslow/kafka2.json ./
    docker cp kafka2.json failslow4:/failslow
    docker cp failslow1:/failslow/kafka2TOP2IO.json ./
    docker cp kafka2TOP2IO.json failslow4:/failslow

    # docker cp failslow1:/failslow/kafka3.json ./
    # docker cp kafka3.json failslow4:/failslow
    # docker cp failslow1:/failslow/kafka3TOP2IO.json ./
    # docker cp kafka3TOP2IO.json failslow4:/failslow
}

# ./bin/kafka-topics.sh --create --bootstrap-server 172.30.0.2:9092,172.30.0.3:9092,172.30.0.4:9092 --replication-factor 2 --partitions 1 --topic test
# ./bin/kafka-console-consumer.sh --bootstrap-server 172.30.0.3:9092 --topic test --from-beginning
# ./bin/kafka-console-producer.sh --broker-list 172.30.0.2:9092 --topic test
function main {
    if [ $option -eq 0 ]; then
        buildzoo
        # ./bin/zkServer.sh start
    elif [ $option -eq 1 ]; then
        configEnv
        # source /etc/profile
    elif [ $option -eq 2 ]; then
        instrument
    elif [ $option -eq 3 ]; then
        mvjson2engine
    fi
}

main