# Reproduce ZK-642
## Build the cluster
- Zookeeper version : 3.1.2
- Build the cluster as the guideline in zookeeper.
- Must use the c client.
## Step2:Replace SchedulerServiceImpl in Sieve server
```
# In Sieve server
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl.java /src/main/java/scheduler/
```
## Step3:Run test
```
# In Sieve server (failslow4)
./startEngine.sh zoo 1
# In failslow1,2,3
./workloadDriver_zoo_dc_1|2|3.sh 1 1|2|3
```
## Fault point
NIOServerCnxn{doIO}