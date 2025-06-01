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

import jsonutils.DelayPoint;
import jsonutils.JsonUtil;
import jsonutils.SystemInfo;
import scheduler.*;

import com.alibaba.fastjson2.JSON;

public class EngineNB {
    public static final int FS_ENGINE_PORT=2359;

    public static String sysName;


    public volatile static long curStartTime;
    public volatile static long curEndTime;

    // only test current fault point
    public static String curClassName;
    public static String curFuncName;
    public static int curLineNum;
    public static int curNodeId=1;
    // fault point -> <startTime,endTime>
    public static Map<DelayPoint,List<Long>> dp2ET=new LinkedHashMap<>();

    public static int count=0;
    public static void main(String[] args){
        if(args.length!=1){
            System.out.println("EngineNB | please specify system name!");
            System.exit(0);
        }
        sysName=args[0];
        parseDP2ET();
        try{
            SchedulerServiceImpl schedulerService=new SchedulerServiceImpl();
            Registry registry=LocateRegistry.createRegistry(FS_ENGINE_PORT);
            Naming.rebind("rmi://localhost:2359/failslow",schedulerService);
            System.out.println("EngineNB | start server, port is 2359");
            while(true){
                if(count>20000){
                    break;
                }
                Thread.sleep(3000);
                count++;
                if(Engine.isFinish[0]&&Engine.isFinish[1]&&Engine.isFinish[2]){
                    System.out.println("EngineNB | Finish tests");
                    break;
                }
            }
            System.out.println("dp2ET size:"+dp2ET.size());
            int seq=0;
            for(Map.Entry<DelayPoint,List<Long>>entry:dp2ET.entrySet()){
                DelayPoint dp=entry.getKey();
                curClassName=dp.className;
                curFuncName=dp.funcName;
                curLineNum=dp.lineNum;
                List<Long> timeList=entry.getValue();
                if(timeList.size()!=2){
                    System.out.println("------------------------------------timeList:"+timeList.size());
                    continue;
                }
                System.out.println("seq:"+seq++);
                System.out.println("className:"+curClassName);
                System.out.println("funcName:"+curFuncName);
                System.out.println("lineNum:"+curLineNum);
                System.out.println("elapsedTime:"+(timeList.get(1)-timeList.get(0))+" ms");
            }
            // add end code
            registry.unbind("failslow");
            UnicastRemoteObject.unexportObject(registry, true);
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void parseDP2ET(){
        JsonUtil jsonUtil=new JsonUtil();
        String path=System.getProperty("user.dir");
        String fileName=sysName+"TimeOutSync";
        String context=jsonUtil.readJson(path, fileName);
        SystemInfo systemInfo=JSON.parseObject(context, SystemInfo.class);
        Iterator<DelayPoint> iter=systemInfo.dpSet.iterator();
        while(iter.hasNext()){
            DelayPoint dp=iter.next();
            DelayPoint dp2=new DelayPoint();
            dp2.className=dp.className;
            dp2.funcName=dp.funcName;
            dp2.lineNum=dp.lineNum;
            List<Long> timeList=new ArrayList<>();
            dp2ET.put(dp2, timeList);
        }
    }
}