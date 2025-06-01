# Reproduce ZK-4
## Step1:Replace SchedulerServiceImpl in Sieve server
```
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl.java /src/main/java/scheduler/
```
## Step2:Run test
```
# In Sieve server (failslow4)
./startEngine.sh zoo 1
# In failslow1,2,3
./workloadDriver_zoo.sh 1 1|2|3
```
## Result
The follower cannot join the cluster.
The system logs are attached.

## Fault point
writeLongToFile