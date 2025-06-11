# Reproduce MR-7369
## Build the cluster
- MapReduce version : 3.3.1
- Build the cluster as the guideline in MapReduce.
## Step2:Replace SchedulerServiceImpl in Sieve server
```
# In Sieve server
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl.java /src/main/java/scheduler/
```
## Step3:Run test
```
# In Sieve server (failslow4)
./startEngine.sh mapred 1
# In failslow1,2,3
./workloadDriver_mapred.sh 1 1|2|3
```
## Fault point
MultipleOutputs#close