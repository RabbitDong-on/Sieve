package failslow;
import config.SootConfig;
import faultinjection.*;
import faultpointanalysis.*;
import utils.*;
/*
 * Framework
 */
public class Framework{
    public static int nodeId=-1;
    public static int testOption=-1;
    public static void main(String[] args)throws Exception{
        if(args.length!=3){
            System.out.println("Framework | please specify system name ,nodeId and testOption");
            System.exit(0);
        }
        nodeId=Integer.parseInt(args[1]);
        testOption=Integer.parseInt(args[2]);
        String sysName=args[0];
        String sysClassPath;

        long startTime=System.currentTimeMillis();
        Runtime runtime=Runtime.getRuntime();
        runtime.gc();
        long startMem=runtime.totalMemory()-runtime.freeMemory();

        if(sysName.equals("zoo")){
            sysClassPath=ClassPath.zooSourceDirectory;
            ParserAndInjection(sysName, sysClassPath);
        }else if(sysName.equals("hdfs")){
            sysClassPath=ClassPath.hdfsSourceDirectory;
            ParserAndInjection(sysName, sysClassPath);
        }else if(sysName.equals("mapred")){
            ParserAndInjection("mapred1", ClassPath.mapreduceSourceDirectory1);
            ParserAndInjection("mapred2", ClassPath.mapreduceSourceDirectory2);
            ParserAndInjection("mapred3", ClassPath.mapreduceSourceDirectory3);
            ParserAndInjection("mapred4", ClassPath.mapreduceSourceDirectory4);
            ParserAndInjection("mapred5", ClassPath.mapreduceSourceDirectory5);
            ParserAndInjection("mapred6", ClassPath.mapreduceSourceDirectory6);
            ParserAndInjection("mapred7", ClassPath.mapreduceSourceDirectory7);
            ParserAndInjection("mapred8", ClassPath.mapreduceSourceDirectory8);
            ParserAndInjection("mapred9", ClassPath.mapreduceSourceDirectory9);
            ParserAndInjection("mapred10", ClassPath.mapreduceSourceDirectory10);
        }else if(sysName.equals("cassandra")){
            sysClassPath=ClassPath.cassandraSourceDirectory;
            ParserAndInjection(sysName, sysClassPath);
        }else if(sysName.equals("hbase")){
            sysClassPath=ClassPath.hbaseSourceDirectory;
            ParserAndInjection(sysName, sysClassPath);
        }else if(sysName.equals("kafka")){
            ParserAndInjection("kafka1", ClassPath.kafkaSourceDirectory1);
            ParserAndInjection("kafka2", ClassPath.kafkaSourceDirectory2);
            // ParserAndInjection("kafka3", ClassPath.kafkaSourceDirectory3);
        }

        long endTime=System.currentTimeMillis();
        long endMem=runtime.totalMemory()-runtime.freeMemory();
        System.out.println("Framework.java | Analysis and Instrumentation time :"+(endTime-startTime)+" ms");
        System.out.println("Framework.java | Memory consumption(s) :"+(startMem)/1024/1024+" MB");
        System.out.println("Framework.java | Memory consumption(e) :"+(endMem)/1024/1024+" MB");
    }
    public static void ParserAndInjection(String sysName,String sysClassPath){
        try{
            SootConfig.setupSoot(sysClassPath);
            SystemParser systemParser=new SystemParser();
            // long startTime=System.currentTimeMillis();
            // Runtime runtime=Runtime.getRuntime();
            // runtime.gc();
            // long startMem=runtime.totalMemory()-runtime.freeMemory();
            systemParser.ObtainCaller(sysName,sysClassPath,nodeId,testOption);

            // delay 
            String delayFileName=sysName+"TimeOutSync";
            DelayInjection delayInjection=new DelayInjection(delayFileName, sysName,sysClassPath);
            delayInjection.InjectDelay(nodeId);
            
            if(testOption==1){
                // trace memory access
                
            }else if(testOption==2){
                // exception
                // String exceptionFileName=sysName;
                // ExceptionInjection exceptionInjection=new ExceptionInjection(exceptionFileName, sysName,sysClassPath);
                // exceptionInjection.InjectException();
                // trace timeout
                String fileName=sysName+"TOPoint";
                TraceTimeOut traceTimeOut=new TraceTimeOut(fileName,sysName, sysClassPath);
                traceTimeOut.traceAllTimeOut();
            }else if(testOption==3){
                // reorder mem accesses
                String fileName=sysName+"DP2ConflictPairs";
                // ScheduleMemAccesses scheduleMemAccesses=new ScheduleMemAccesses(fileName, sysClassPath);
                // scheduleMemAccesses.schedule();
            }else{
                // process non-blocking io
                String nbFileName=sysName+"TimeOutSync";
                NonBlockingIOProcess nonBlockingIOProcess=new NonBlockingIOProcess(nbFileName, sysName, sysClassPath);
                nonBlockingIOProcess.InjectHook(nodeId);
            }
            // long endTime=System.currentTimeMillis();
            // long endMem=runtime.totalMemory()-runtime.freeMemory();
            // System.out.println("Framework.java | Analysis and Instrumentation time :"+(endTime-startTime)+" ms");
            // System.out.println("Framework.java | Memory consumption(s) :"+(startMem)/1024/1024+" MB");
            // System.out.println("Framework.java | Memory consumption(e) :"+(endMem)/1024/1024+" MB");
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Framework.java | ParserAndInjection failed!");
        }
    }
}