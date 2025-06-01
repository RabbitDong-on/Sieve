# Reproduce ZK-2251
## Build the cluster
- Zookeeper version : 3.4.9, 3.5.2, 3.4.11
- Build the cluster as the guideline in zookeeper.
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
./workloadDriver_zoo.sh 1 1|2|3
```
## Fault point
ServerCnxn{sendResponse}