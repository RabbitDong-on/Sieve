# Reproduce KA-1
## Step2:Prepare workloads
```
cp workloadDriver_kafka_1|2|3 /apache-zookeeper-3.10.0-SNAPSHOT-bin/
```
## Step1:Replace SchedulerServiceImpl in Sieve server
```
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl.java /src/main/java/scheduler/SchedulerServiceImpl.java
```
## Step2:Run test
```
# In Sieve server (failslow4)
./startEngine.sh kafka 1
# In failslow1,2,3
./workloadDriver_kafka_1|2|3.sh 1 1|2|3
```
## Result
The uncreated topic state return a wrong message, which is the existent of the topic, to the sequential topic creations.
The system logs are attached.

## Fault point
createLogDirectory