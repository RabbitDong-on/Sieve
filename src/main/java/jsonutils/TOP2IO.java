package jsonutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.SootMethod;


public class TOP2IO{
    public String sysName;
    public List<MapTO2IO> map;
    public TOP2IO(String sysName){
        this.sysName=sysName;
        this.map=new ArrayList<>();
    }
    public void writeTOP2IO(SootMethod timeoutMethod,List<SootMethod> ioMethodList){
        MapTO2IO mapTO2IO=new MapTO2IO();
        mapTO2IO.toClassName=timeoutMethod.getDeclaringClass().getName();
        mapTO2IO.toFuncName=timeoutMethod.getSubSignature();
        Iterator<SootMethod> iter=ioMethodList.iterator();
        while(iter.hasNext()){
            SootMethod ioMethod=iter.next();
            DelayPoint dp=new DelayPoint();
            dp.className=ioMethod.getDeclaringClass().getName();
            dp.funcName=ioMethod.getSubSignature();
            mapTO2IO.dpSet.add(dp);
        }
        this.map.add(mapTO2IO);
    }
    public boolean check(List<DelayPoint> dpList, DelayPoint dp){
        Iterator<DelayPoint> dpIter=dpList.iterator();
        while(dpIter.hasNext()){
            DelayPoint existedDP=dpIter.next();
            if(existedDP.className.equals(dp.className)&&existedDP.funcName.equals(dp.funcName)){
                return true;
            }
        }
        return false;
    }

    public Map<DelayPoint,List<TimeOutPoint>> parser(){
        Map<DelayPoint,List<TimeOutPoint>> res=new LinkedHashMap<>();
        List<DelayPoint> dpList=new ArrayList<>();
        Iterator<MapTO2IO> iter=map.iterator();
        System.out.println("TOP2IO | map:"+map.size());
        while(iter.hasNext()){
            MapTO2IO mapTO2IO=iter.next();
            Iterator<DelayPoint> dpIter=mapTO2IO.dpSet.iterator();
            while(dpIter.hasNext()){
                DelayPoint dp=dpIter.next();
                if(!check(dpList, dp)){
                    dpList.add(dp);
                }
            }
        }
        System.out.println("TOP2IO | io size:"+dpList.size());
        Iterator<DelayPoint> dpListIter=dpList.iterator();
        while(dpListIter.hasNext()){
            List<TimeOutPoint> toList=new ArrayList<>();
            DelayPoint dp=dpListIter.next();
            Iterator<MapTO2IO> to2ioIter=map.iterator();
            while(to2ioIter.hasNext()){
                MapTO2IO mapTO2IO=to2ioIter.next();
                // if(mapTO2IO.dpSet.contains(dp)){
                if(checkSet(mapTO2IO.dpSet, dp)){
                    TimeOutPoint toP=new TimeOutPoint();
                    toP.toClassName=mapTO2IO.toClassName;
                    toP.toFuncName=mapTO2IO.toFuncName;
                    toList.add(toP);
                }
            }
            if(toList.size()==0){
                System.out.println("TOP2IO | "+dp.className+" "+dp.funcName);
            }
            res.put(dp,toList);
        }
        System.out.println("TOP2IO | res:"+res.size());
        return res;
    }
    public boolean checkSet(Set<DelayPoint> dpSet,DelayPoint dp){
        Iterator<DelayPoint> dpIter=dpSet.iterator();
        while(dpIter.hasNext()){
            DelayPoint existedDP=dpIter.next();
            if(existedDP.className.equals(dp.className)&&existedDP.funcName.equals(dp.funcName)){
                return true;
            }
        }
        return false;
    }

    public List<TimeOutPoint> getTOP(){
        List<TimeOutPoint> res=new ArrayList<>();
        Iterator<MapTO2IO>iter=map.iterator();
        while(iter.hasNext()){
            MapTO2IO mapTO2IO=iter.next();
            TimeOutPoint toP=new TimeOutPoint();
            toP.toClassName=mapTO2IO.toClassName;
            toP.toFuncName=mapTO2IO.toFuncName;
            res.add(toP);
        }
        return res;
    }
}
class MapTO2IO{
    public String toClassName;
    public String toFuncName;
    public Set<DelayPoint> dpSet;
    public MapTO2IO(){
        this.dpSet=new HashSet<>();
    }
}