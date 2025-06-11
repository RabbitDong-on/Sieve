package jsonutils;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.SootClass;

import java.util.ArrayList;
import java.util.HashSet;


/*
 * SystemInfo
 */
public class SystemInfo{
    public String sysName;
    public Set<DelayPoint>dpSet;
    public Set<TimeOutPoint>toSet;
    public SystemInfo(){
        dpSet=new HashSet<>();
        toSet=new HashSet<>();
    }
    /*
     * writeSystemInfo
     */
    public void writeSystemInfo(String sysName,String className,String funcName,int lineNum,int type,List<SootClass>IOEXP){
        this.sysName=sysName;
        DelayPoint dp=new DelayPoint();
        dp.className=className;
        dp.funcName=funcName;
        dp.lineNum=lineNum;
        dp.type=type;
        if(IOEXP!=null){
            dp.exception=new ArrayList<>();
            Iterator<SootClass>ioeIterator=IOEXP.iterator();
            while(ioeIterator.hasNext()){
                SootClass exception=ioeIterator.next();
                dp.exception.add(exception.getName());
            }
        }
        this.dpSet.add(dp);
    }
    /*
     * writeTOPoint
     */
    public void writeTOPoint(String sysName,String className,String funcName,int lineNum,int type,int lr,int traceType){
        this.sysName=sysName;
        TimeOutPoint toP=new TimeOutPoint();
        toP.timeoutMethodType=type;
        toP.toClassName=className;
        toP.toFuncName=funcName;
        toP.toPos=lineNum;
        toP.lr=lr;
        toP.traceType=traceType;
        this.toSet.add(toP);
    }
    /*
     * print
     */
    public void print(){
        System.out.println(this.sysName);
        Iterator<DelayPoint>iter=dpSet.iterator();
        while(iter.hasNext()){
            DelayPoint dp=iter.next();
            System.out.println(dp.className);
            System.out.println(dp.funcName);
            System.out.println(dp.lineNum);
        }
    }
}