package failslow;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson.JSON;

import scheduler.*;
import jsonutils.*;

/*
 * class : Engine
 * failslow controller
 * - how many delays will be injected?
 *   - one/two for each trial
 * - which delay points will be injected delay?
 *   - the last delay point within the basic block -- done by analyzer
 *   - the delay point with different call stacks
 * - how long the delay lasts?
 *   - synchronized related : 1 min
 *   - timeout protected : 2*timeout
 */

public class Engine {
    public static final int FS_ENGINE_PORT=2359;
    public volatile static boolean[] isFinish=new boolean[3];
    public volatile static boolean[] isReport=new boolean[3];
    public volatile static boolean[] isRestart=new boolean[3];
    public volatile static boolean[] isCheck=new boolean[3];
    public volatile static boolean isStarted=false;

    public volatile static int injectNum=0;
    public volatile static boolean isInjected=false;

    public static String className=null;
    public static String funcName=null;
    public static int lineNum=-1;
    public static long timeout=-1;
    public static Map<DelayPoint,List<TimeOutPoint>> map=new LinkedHashMap<>();
    // dp timeoutvaluelist
    public static Map<DelayPoint,List<Long>> map2TOV=new LinkedHashMap<>();
    public static List<TimeOutPoint> toPList=new ArrayList<>();
    // for exception
    public static List<DelayPoint> exceptionMap=new ArrayList<>();
    public volatile static int count=0;
    public static int slowNum=0;
    public static String sysName;
    public static int totalTestNum=0;

    public static void main(String[] args){
        if(args.length!=2){
            System.out.println("Engine | please specify system name and testNum");
            System.exit(0);
        }
        sysName=args[0];
        totalTestNum=Integer.parseInt(args[1]);
        // parserTOP2IO();
        // parserException();
        // System.out.println("Engine | dp -> timeout point:"+map.size());
        Thread thread1=new Thread(new Runnable(){
           @Override
           public void run() {
                int testNum=0;
                while(testNum<totalTestNum){
                    if(!Engine.isStarted){
                        while(!(Engine.isReport[0]&&Engine.isReport[1]&&Engine.isReport[2])){}
                        testNum++;
                        Arrays.fill(Engine.isRestart, false);
                        Arrays.fill(Engine.isCheck, false);
                        System.out.println("Engine | start trial:"+testNum);
                        Engine.isStarted=true;
                    }else{
                        int elapsedTime=count;
                        boolean isSlow=false;
                        boolean isCount=false;
                        while(Engine.isReport[0]||Engine.isReport[1]||Engine.isReport[2]){
                            while((Engine.isCheck[0]||Engine.isCheck[1]||Engine.isCheck[2])&&!(Engine.isCheck[0]&&Engine.isCheck[1]&&Engine.isCheck[2])){
                                if(!isCount){
                                    elapsedTime=count;
                                    isCount=true;
                                }
                                if((count-elapsedTime>10)&&!isSlow&&isCount){
                                    slowNum++;
                                    isSlow=true;
                                    if(!Engine.isCheck[0]){
                                        System.out.println("Engine | the first client is so slow:"+slowNum);
                                    }
                                    if(!Engine.isCheck[1]){
                                        System.out.println("Engine | the second client is so slow:"+slowNum);
                                    }
                                    if(!Engine.isCheck[2]){
                                        System.out.println("Engine | the third client is so slow:"+slowNum);
                                    }
                                }
                            }
                        }
                        System.out.println("Engine | end trial:"+testNum);
                        SchedulerServiceImpl.signal();
                        Engine.isInjected=false;
                        Engine.injectNum=0;
                        Engine.isStarted=false;
                    }
                }
                while((Engine.isReport[0]||Engine.isReport[1]||Engine.isReport[2])){}
                System.out.println("Engine | end trial:"+testNum);
                SchedulerServiceImpl.signal();
                Engine.isInjected=false;
                Engine.injectNum=0;
                Engine.isStarted=false;
           }
        });
        long startTime=System.currentTimeMillis();
        thread1.start();
        try{
            SchedulerServiceImpl schedulerService=new SchedulerServiceImpl();
            Registry registry=LocateRegistry.createRegistry(FS_ENGINE_PORT);
            Naming.rebind("rmi://localhost:2359/failslow",schedulerService);
            System.out.println("Engine | start server, port is 2359");
            // check hang
            // <=30s
            boolean isGood=false;
            while(true){
                if(count>20000){
                    break;
                }
                Thread.sleep(3000);
                count++;
                if(isFinish[0]&&isFinish[1]&&isFinish[2]){
                    System.out.println("Engine | Finish tests");
                    isGood=true;
                    break;
                }
            }
            thread1.join();
            // schedulerService.printDP();
            Iterator<TimeOutPoint> iter=toPList.iterator();
            while(iter.hasNext()){
                TimeOutPoint toP=iter.next();
                System.out.println("Engine | toclassName :"+toP.toClassName+" toFuncName:"+toP.toFuncName+" timeoutValue:"+toP.toVList.size());
            }
            // add end code
            registry.unbind("failslow");
            UnicastRemoteObject.unexportObject(registry, true);
            if(!isGood){
                if(isFinish[0]){
                    System.out.println("Engine | client 1 finished!");
                }
                if(isFinish[1]){
                    System.out.println("Engine | client 2 finished!");
                }
                if(isFinish[2]){
                    System.out.println("Engine | client 3 finished!");
                }
                System.out.println("Engine | client hang!");
            }
            long endTime=System.currentTimeMillis();
            System.out.println("Engine | Execution Time:"+(endTime-startTime)+" ms");
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /*
     * parserTOP2IO
     */
    public static void parserTOP2IO(){
        JsonUtil jsonUtil=new JsonUtil();
        String path=System.getProperty("user.dir");
        if(sysName.equals("kafka")){
            for(int i=1;i<=2;i++){
                String fileName="kafka"+Integer.toString(i)+"TOP2IO";
                String context=jsonUtil.readJson(path, fileName);
                TOP2IO top2io=JSON.parseObject(context, TOP2IO.class);
                Map<DelayPoint,List<TimeOutPoint>> tmpMap=top2io.parser();
                List<TimeOutPoint> tmptoPList=top2io.getTOP();
                map.putAll(tmpMap);
                toPList.addAll(tmptoPList);
            }
        }else if(sysName.equals("mapred")){
            for(int i=1;i<=10;i++){
                String fileName="mapred"+Integer.toString(i)+"TOP2IO";
                String context=jsonUtil.readJson(path, fileName);
                TOP2IO top2io=JSON.parseObject(context, TOP2IO.class);
                Map<DelayPoint,List<TimeOutPoint>> tmpMap=top2io.parser();
                List<TimeOutPoint> tmptoPList=top2io.getTOP();
                map.putAll(tmpMap);
                toPList.addAll(tmptoPList);
            }
        }else{
            String fileName=sysName+"TOP2IO";
            String context=jsonUtil.readJson(path, fileName);
            TOP2IO top2io=JSON.parseObject(context, TOP2IO.class);
            map=top2io.parser();
            toPList=top2io.getTOP();
        }
        System.out.println("Engine | timeoutMethod size:"+toPList.size());
        List<TimeOutPoint> checkList=new ArrayList<>();
        for(Map.Entry<DelayPoint,List<TimeOutPoint>>entry:map.entrySet()){
            List<TimeOutPoint> tempList=entry.getValue();
            // System.out.println("Engine size:"+tempList.size());
            if(tempList.size()==0){
                System.out.println("Engine | "+entry.getKey().className+" "+entry.getKey().funcName);
            }
            Iterator<TimeOutPoint> iter=tempList.iterator();
            while(iter.hasNext()){
                TimeOutPoint toP=iter.next();
                if(!check(checkList,toP)){
                    checkList.add(toP);
                }
            }
        }
        System.out.println("Engine | checkList size:"+checkList.size());
    }
    public static boolean check(List<TimeOutPoint> toList, TimeOutPoint to){
        Iterator<TimeOutPoint> toIter=toList.iterator();
        while(toIter.hasNext()){
            TimeOutPoint existedTO=toIter.next();
            if(existedTO.toClassName.equals(to.toClassName)&&existedTO.toFuncName.equals(to.toFuncName)){
                return true;
            }
        }
        return false;
    }
    /*
     * parserException
     */
    public static void parserException(){
        JsonUtil jsonUtil=new JsonUtil();
        String path=System.getProperty("user.dir");
        if(sysName.equals("kafka")){
            for(int i=1;i<=2;i++){
                String fileName="kafka"+Integer.toString(i);
                String context=jsonUtil.readJson(path, fileName);
                SystemInfo systemInfo=JSON.parseObject(context,SystemInfo.class);
                Iterator<DelayPoint> dpIter=systemInfo.dpSet.iterator();
                while(dpIter.hasNext()){
                    DelayPoint dp=dpIter.next();
                    exceptionMap.add(dp);
                }
            }
        }else if(sysName.equals("mapred")){
            for(int i=1;i<=10;i++){
                String fileName="mapred"+Integer.toString(i);
                String context=jsonUtil.readJson(path, fileName);
                SystemInfo systemInfo=JSON.parseObject(context,SystemInfo.class);
                Iterator<DelayPoint> dpIter=systemInfo.dpSet.iterator();
                while(dpIter.hasNext()){
                    DelayPoint dp=dpIter.next();
                    exceptionMap.add(dp);
                }
            }
        }else{
            String context=jsonUtil.readJson(path, sysName);
            SystemInfo systemInfo=JSON.parseObject(context,SystemInfo.class);
            Iterator<DelayPoint> dpIter=systemInfo.dpSet.iterator();
            while(dpIter.hasNext()){
                DelayPoint dp=dpIter.next();
                exceptionMap.add(dp);
            }
        }
    }
}