#!/bin/bash

option=$1


# ./start-build-env.sh
function cpin {
# docker cp failslow1:/failslow/target ./
docker cp /home/xx/failslow/target_ST xx_hdfs:/home/xx
docker cp instrument.sh xx_hdfs:/home/xx
docker cp buildhdfs.sh xx_hdfs:/home/xx
}

# instrument
function instrument {
# pwd /failslow
cd hadoop
mvn compile
cd ..
cat myid|xargs ./instrument.sh hdfs
cd hadoop
mvn package -Pdist -Dtar -Dmaven.javadoc.skip=true -DskipTests -Dmaven.main.skip=true
sudo mv hadoop-dist/target/hadoop-3.3.6/share/hadoop/hdfs /
cd ..
sudo mv *.json /
}

function cpout {
docker cp xx_hdfs:/hdfs.json ./
docker cp xx_hdfs:/hdfsTOP2IO.json ./
docker cp xx_hdfs:/hdfsTimeOutSync.json ./
}

function cpuintofailslow {
    wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz
    docker cp hadoop-3.3.6.tar.gz failslow1:/
    docker cp hadoop-3.3.6.tar.gz failslow2:/
    docker cp hadoop-3.3.6.tar.gz failslow3:/
    docker cp hadoop-3.3.6.tar.gz failslow4:/
    docker cp hdfs failslow1:/
    docker cp hdfs failslow2:/
    docker cp hdfs failslow3:/
    docker cp hdfs failslow4:/
    # failslow4 0.6
    # failslow5 0.5
    docker cp hdfs.json failslow5:/failslow
    docker cp hdfsTOP2IO.json failslow5:/failslow
}

function startZK {
    cd /
    wget https://archive.apache.org/dist/zookeeper/zookeeper-3.7.2/apache-zookeeper-3.7.2-bin.tar.gz
    tar zxvf apache-zookeeper-3.7.2-bin.tar.gz 
    cd /apache-zookeeper-3.7.2-bin 
    cp /failslow/HBase_Config/zoo.cfg conf/zoo.cfg
    mkdir data
    cp /failslow/myid data/
    ./bin/zkServer.sh start
}

function configEnv {
cd /
mkdir hadoop
mv hadoop-3.3.6.tar.gz hadoop
cd hadoop
tar zxvf hadoop-3.3.6.tar.gz
mkdir name
mkdir data
mkdir hdfstemp
rm -rf hadoop-3.3.6/share/hadoop/hdfs
mv /hdfs hadoop-3.3.6/share/hadoop/
cd /failslow
cp HDFS_Config/hosts /etc/
cp HDFS_Config/profile /etc/

cd /hadoop/hadoop-3.3.6
rm -rf etc/hadoop/*
cp -r /failslow/HDFS_Config/config/* etc/hadoop/

# source /etc/profile
# hdfs namenode -format
# hdfs --daemon start namenode
# hdfs --daemon start datanode
# hdfs --daemon stop datanode
# hdfs --daemon stop datanode
# start-dfs.sh
}

function genWorkload {
    cat myid | xargs python genWorkload.py hdfs >> hdfsworkload.sh
    chmod +x hdfsworkload.sh
}

function clean {
    rm -rf /hadoop/data/*
    rm -rf /hadoop/name/*
    rm -rf /hadoop/hdfstemp/*
}
function main {
    if [ $option -eq 0 ]; then
        cpin
    elif [ $option -eq 1 ]; then
        instrument
    elif [ $option -eq 2 ]; then
        cpout
    elif [ $option -eq 3 ]; then
        cpuintofailslow
    elif [ $option -eq 4 ]; then
        configEnv
    elif [ $option -eq 5 ]; then
        genWorkload
    elif [ $option -eq 6 ]; then
        clean
    fi
}

main

