# Reproduce ZK-2
The bug need two test runs. One for delay less than 20s, the other for delay more than 20s.
## Step1:Replace SchedulerServiceImpl in Sieve server
```
# for the first test run
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl_20s.java /src/main/java/scheduler/SchedulerServiceImpl.java
# for the second test run
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl_20plus.java /src/main/java/scheduler/SchedulerServiceImpl.java
```
## Step2:Run test
```
# In Sieve server (failslow4)
./startEngine.sh zoo 1
# In failslow1,2,3
./workloadDriver_zoo.sh 1 1|2|3
```
## Result
The system log of the node injected does not contain client disconnection messages when the injected delay lasts more than 20s.
The system logs are attached.

## Fault point
NIOServerCnxn#doIO