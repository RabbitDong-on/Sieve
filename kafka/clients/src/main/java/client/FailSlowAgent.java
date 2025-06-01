package client;
import java.io.FileNotFoundException;
import java.io.SyncFailedException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.io.IOException;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLPeerUnverifiedException;


import utils.*;
import scheduler.*;

public class FailSlowAgent{
    public static ReentrantLock lock=new ReentrantLock();
    public static Condition aCondition=lock.newCondition();
    public static boolean traceTimeOut=false;
    public static void main(String[] args){
        try{
            /*
             * 0 id report progress
             * 1 id start trial
             * 2 id end trial
             * 3 id isRestart
             * 4 id isCheck
             */
            // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            if(args.length==2){
                int option=Integer.parseInt(args[0]);
                int id=Integer.parseInt(args[1]);
                if(option==0){
                    access.isGoingOn(id);
                }else if(option==1){
                    access.startTrial(id);
                }else if(option==2){
                    access.endTrial(id);
                }else if(option==3){
                    access.isRestart(id);
                }else if(option==4){
                    access.isCheck(id);
                }else if(option==4){
                    access.isRecord();
                }
            }else {
                turnOffTrace();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        // int type=Integer.parseInt(args[0]);
        // report(type);
    }
    public static void turnOffTrace(){
        FailSlowAgent.traceTimeOut=false;
    }
    public static void reportProgress(int id){
        try{
            // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            access.isGoingOn(id);
        }catch (Exception e){
            e.printStackTrace();
        } 
    }
    /*
     * Exception type:
     * java.io.IOException
     * java.net.SocketException
     * java.nio.channels.ClosedChannelException
     * java.io.FileNotFoundException
     * java.net.UnknownHostException
     * java.io.SyncFailedException
     * javax.net.ssl.SSLPeerUnverifiedException
     */
    public static void exception() throws SocketException, ClosedChannelException, FileNotFoundException, UnknownHostException, SyncFailedException, SSLPeerUnverifiedException,IOException{
        try{
            // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
            ScheduleInfo info=FailSlowAgent.getInfo();
            ScheduleRes res=access.isInjectException(info);
            if(res.isInject){
                throwException(res.exceptionName);
            }

        }catch (RemoteException|NotBoundException|MalformedURLException rmie){
            rmie.printStackTrace();
        }catch (SocketException|ClosedChannelException|FileNotFoundException|UnknownHostException|SyncFailedException|SSLPeerUnverifiedException e){
            throw e;
        }catch (IOException ioe){
            throw ioe;
        }
    }
    public static void throwException(String exceptionName) throws SocketException, ClosedChannelException, FileNotFoundException, UnknownHostException, SyncFailedException, SSLPeerUnverifiedException,IOException{
        if(exceptionName.equals("java.nio.channels.ClosedChannelException")){
            throw new java.nio.channels.ClosedChannelException();
        }else if(exceptionName.equals("java.net.SocketException")){
            throw new java.net.SocketException();
        }else if(exceptionName.equals("java.io.FileNotFoundException")){
            throw new java.io.FileNotFoundException();
        }else if(exceptionName.equals("java.net.UnknownHostException")){
            throw new java.net.UnknownHostException();
        }else if(exceptionName.equals("java.io.SyncFailedException")){
            throw new java.io.SyncFailedException(exceptionName);
        }else if(exceptionName.equals("java.net.ssl.SSLPeerUnverifiedException")){
            throw new javax.net.ssl.SSLPeerUnverifiedException(exceptionName);
        }else if(exceptionName.equals("java.io.IOException")){
            throw new java.io.IOException();
        }
    }
    public static void report(Integer type,int id){
        try{
            // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
            ScheduleInfo info=FailSlowAgent.getInfo();
            info.nodeId=id;
            info.type=type;
            ScheduleRes res=access.isInjectDelay(info);
            if(res.isInject){
                System.out.println("FailSlowAgent.java | isInject : "+res.isInject+" delayTime : "+res.delayTime);
                delay(res.delayTime);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public static ScheduleInfo getInfo(){
        long tId=Thread.currentThread().getId();
        String className=Thread.currentThread().getStackTrace()[3].getClassName();
        String funcName=Thread.currentThread().getStackTrace()[3].getMethodName();
        int lineNum=Thread.currentThread().getStackTrace()[3].getLineNumber();
        ScheduleInfo info=new ScheduleInfo();
        info.className=className;
        info.funcName=funcName;
        info.tId=tId;
        info.lineNum=lineNum;
        info.lastSTE=Thread.currentThread().getStackTrace();
        System.out.println("FailSlowAgent.java | Thread ID : "+tId+" className : "+className+" funcName : "+funcName+" lineNum : "+lineNum);
        return info;
    }

    /*
     * traceR/WAccess()
     * report memory access to Sieve server which stores them into json.
     */
    public static void traceReadAccess(String className, String fieldName,String funcName,long lineNum,int hashcode,long tId,int nodeId){
        try{
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            access.reportReadAccess(className, fieldName,funcName,lineNum, hashcode, tId, nodeId);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public static void traceWriteAccess(String className, String fieldName,String funcName,long lineNum,int hashcode,long tId,int nodeId){
        try{
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            access.reportWriteAccess(className, fieldName,funcName,lineNum, hashcode, tId, nodeId);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /*
     * traceTimeOutHandler
     */
    public static void traceTimeOutHandler(){
        try{
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            String className=Thread.currentThread().getStackTrace()[2].getClassName();
            String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
            int lineNum=Thread.currentThread().getStackTrace()[2].getLineNumber();
            access.reportTimeOutHandler(className, funcName, lineNum);
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static void delay(long time){
        try{
            // Thread.sleep(time);
            // Sieve control sleep time
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            access.reportSleep(time);
            
        // }catch(InterruptedException ie){
        //     System.out.println("FailSlowAgent.java | delay error!");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /*
     * schedule for concurrency bug
     */
    public static void await(long seq){
        try{
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            access.Await(seq);
        }catch(Exception e){
        }
    }
    /*
     * stop delay
     * 
     */
    public static void signal(long seq){
        try{
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            access.Signal(seq);
        }catch(Exception e){
        }
    }

    public static void traceConnect(int time){
        if(FailSlowAgent.traceTimeOut){
            try{
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                access.reportConnect(time,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static void traceWait(long time){
        if(FailSlowAgent.traceTimeOut){
            try{
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                access.reportWait(time,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static long traceWait(long time,int nanos){
        long res=-1;
        if(FailSlowAgent.traceTimeOut){
            try{
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                res=access.reportWait(time,nanos,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return res;
    }
    public static void traceAwait(long time){
        if(FailSlowAgent.traceTimeOut){
            try{
            // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                access.reportAwait(time,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static long traceAwait(long time,java.util.concurrent.TimeUnit arg){
        long res=-1;
        if(FailSlowAgent.traceTimeOut){
            try{
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                res=access.reportAwait(time,arg,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return res;
    }
    public static long traceTryAcquire(long time,java.util.concurrent.TimeUnit arg){
        long res=-1;
        if(FailSlowAgent.traceTimeOut){
            try{
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                res=access.reportTryAcquire(time,arg,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return res;
    }
    public static void traceJoin(long time){
        if(FailSlowAgent.traceTimeOut){
            try{
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                access.reportJoin(time,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static long traceJoin(long time,int nanos){
        long res=-1;
        if(FailSlowAgent.traceTimeOut){
            try{
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                res=access.reportJoin(time,nanos,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return res;
    }
    public static long traceAddSub(long lValue,long rValue,int lr){
        long res=-1;
        if(FailSlowAgent.traceTimeOut){
            try{
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                long toValue=lr==1?lValue:rValue;
                res=access.reportAddSub(toValue,className,funcName);
                res=lValue;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return res;
    }
    public static long traceCMP(long lValue,long rValue,int lr){
        long res=-1;
        if(FailSlowAgent.traceTimeOut){
            try{
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                long toValue=lr==1?lValue:rValue;
                res=access.reportCMP(toValue,className,funcName);
                res=lValue;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return res;
    }
    public static void traceCMP(int lValue,int rValue,int lr){
        if(FailSlowAgent.traceTimeOut){
            try{
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                int toValue=lr==1?lValue:rValue;
                access.reportCMP(toValue,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    
}