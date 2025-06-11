package failslow;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSON;

import scheduler.*;
import jsonutils.*;
import utils.*;

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

public class EngineDC {
    public static final int FS_ENGINE_PORT=2359;

    // cur delay point
    public static String curClzName;
    public static String curFuncName;
    public static int curLineNum;

    public static Map<DelayPoint,List<TimeOutPoint>> map=new LinkedHashMap<>();
    // dp timeoutvaluelist
    public static Map<DelayPoint,List<Long>> map2TOV=new LinkedHashMap<>();
    public static List<TimeOutPoint> toPList=new ArrayList<>();

    public volatile static int count=0;
    public static String sysName;

    // for timeoout handler
    public static TimeOutHandlerInfo timeoutHandlerInfo=new TimeOutHandlerInfo();
    
    public static ReentrantLock lock=new ReentrantLock();
    
    // for mem access
    // classify by nodeId
    public static MemAccesses memAccesses=new MemAccesses();
    public static Map<MemAccess,Integer> mA2ID=new LinkedHashMap<>();
    // additional memAccesses
    public static MemAccesses additionalMA=new MemAccesses();
    public static Map<MemAccess,Integer> aMA2ID=new LinkedHashMap<>();
    // existing conflicting pairs 
    // total conflicting pairs
    // timeout handler memory accesses to normal memory accesses
    public static List<ConflictPair> conflictedPairs=new ArrayList<>();
    // store delay point to conflicting pairs
    public static DP2ConflictPairs dp2ConflictPairs=new DP2ConflictPairs();
    // true: no delay ; false: inject delay
    public volatile static boolean isCollect=true;
    public volatile static boolean isOver=false;
    public volatile static int totalCount=0;

    public volatile static int collectCount=0;
    public volatile static int collectThreshold=10;
    public static void main(String[] args){
        if(args.length!=1){
            System.out.println("EngineDC | please specify system name!");
            System.exit(0);
        }
        sysName=args[0];
        // parserTOP2IO();
        System.out.println("EngineDC | dp -> timeout point:"+map.size());
        Thread thread1=new Thread(new Runnable(){
           @Override
           public void run() {
                int testNum=0;
                while(true){
                    if(!Engine.isStarted){
                        while(!(Engine.isReport[0]&&Engine.isReport[1]&&Engine.isReport[2])){                            
                            if(isOver){
                                // finish test
                                return ;
                            }
                        }
                        testNum++;
                        System.out.println("EngineDC | start trial:"+testNum);
                        Engine.isStarted=true;
                        // if(EngineDC.collectCount%2!=0){
                            EngineDC.isCollect=false;
                        // }

                    }else{
                        while(Engine.isReport[0]||Engine.isReport[1]||Engine.isReport[2]){
                            if(isOver){
                                // finish test
                                return ;
                            }
                        }
                        System.out.println("EngineDC | end trial:"+testNum);
                        // stop delay
                        SchedulerServiceImpl.signal();

                        Engine.isInjected=false;
                        Engine.injectNum=0;
                        Engine.isStarted=false;
                        // EngineDC.collectCount++;
                    }
                }
           }
        });
        long startTime=System.currentTimeMillis();
        thread1.start();
        try{
            SchedulerServiceImpl schedulerService=new SchedulerServiceImpl();
            Registry registry=LocateRegistry.createRegistry(FS_ENGINE_PORT);
            Naming.rebind("rmi://localhost:2359/failslow",schedulerService);
            System.out.println("EngineDC | start server, port is 2359");
            // check hang
            // <=30s
            boolean isGood=false;
            while(true){
                if(count>20000){
                    break;
                }
                Thread.sleep(3000);
                count++;
                if(Engine.isFinish[0]&&Engine.isFinish[1]&&Engine.isFinish[2]){
                    System.out.println("EngineDC | Finish tests");
                    isOver=true;
                    isGood=true;
                    break;
                }
            }
            thread1.join();
            schedulerService.printDP();
            Iterator<TimeOutPoint> iter=toPList.iterator();
            while(iter.hasNext()){
                TimeOutPoint toP=iter.next();
                System.out.println("EngineDC | toclassName :"+toP.toClassName+" toFuncName:"+toP.toFuncName+" timeoutValue:"+toP.toVList.size());
            }
            // add end code
            registry.unbind("failslow");
            UnicastRemoteObject.unexportObject(registry, true);
            if(!isGood){
                if(Engine.isFinish[0]){
                    System.out.println("EngineDC | client 1 finished!");
                }
                if(Engine.isFinish[1]){
                    System.out.println("EngineDC | client 2 finished!");
                }
                if(Engine.isFinish[2]){
                    System.out.println("EngineDC | client 3 finished!");
                }
                System.out.println("EngineDC | client hang!");
            }
            long endTime=System.currentTimeMillis();
            System.out.println("EngineDC | Execution Time:"+(endTime-startTime)+" ms");
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    

    public static int isContain(Map<MemAccess,Integer>memA2ID,MemAccess mA){
        for(Map.Entry<MemAccess,Integer>entry:memA2ID.entrySet()){
            MemAccess memA=entry.getKey();
            int id=entry.getValue();
            if(memA.className.equals(mA.className)&&
            memA.fieldName.equals(mA.fieldName)&&
            // memA.hashCode==mA.hashCode&&
            memA.funcName.equals(mA.funcName)&&
            memA.lineNum==mA.lineNum){
                return id;
            }
        }
        return -1;
    }


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
        System.out.println("EngineDC | timeoutMethod size:"+toPList.size());
        List<TimeOutPoint> checkList=new ArrayList<>();
        for(Map.Entry<DelayPoint,List<TimeOutPoint>>entry:map.entrySet()){
            List<TimeOutPoint> tempList=entry.getValue();
            // System.out.println("Engine size:"+tempList.size());
            if(tempList.size()==0){
                System.out.println("EngineDC | "+entry.getKey().className+" "+entry.getKey().funcName);
            }
            Iterator<TimeOutPoint> iter=tempList.iterator();
            while(iter.hasNext()){
                TimeOutPoint toP=iter.next();
                if(!check(checkList,toP)){
                    checkList.add(toP);
                }
            }
        }
        System.out.println("EngineDC | checkList size:"+checkList.size());
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
}