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
import javax.net.ssl.SSLPeerUnverifiedException;
import utils.*;
import scheduler.*;

public class FailSlowAgent{
    public static boolean traceTimeOut=true;
    public static void main(String[] args){
        try{
            /*
             * 0 id report progress
             * 1 id start trial
             * 2 id end trial
             * 3 turnOffTrace
             */
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
                }
            }else {
                turnOffTrace();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        // report();
    }
    public static void turnOffTrace(){
        FailSlowAgent.traceTimeOut=false;
    }
    public static void reportProgress(int id){
        try{
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
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
    public static void report(int type){
        try{
            SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
            // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
            ScheduleInfo info=FailSlowAgent.getInfo();
            info.type=type;
            ScheduleRes res=access.isInjectDelay(info);
            if(res.isInject){
                System.out.println("FailSlowAgent.java | isInject : "+res.isInject+" delayTime : "+res.delayTime);
            }
            if(res.isInject){
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
        // System.out.println("Thread ID : "+tId+" className : "+className+" funcName : "+funcName+" lineNum : "+lineNum);
        return info;
    }


    public static void delay(long time){
        try{
            Thread.sleep(time);
        }catch(InterruptedException ie){
            System.out.println("FailSlowAgent.java | delay error!");
        }
    }

    public static void traceConnect(int time){
        System.out.println("FailSlowAgent.java | traceConnect");
        if(FailSlowAgent.traceTimeOut){
            try{
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                access.reportConnect(time,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static void traceWait(long time){
        System.out.println("FailSlowAgent.java | traceWait");
        if(FailSlowAgent.traceTimeOut){
            try{
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                access.reportWait(time,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static long traceWait(long time,int nanos){
        System.out.println("FailSlowAgent.java | traceWait");
        long res=-1;
        if(FailSlowAgent.traceTimeOut){
            try{
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
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
        System.out.println("FailSlowAgent.java | traceAwait");
        if(FailSlowAgent.traceTimeOut){
            try{
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                access.reportAwait(time,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static long traceAwait(long time,java.util.concurrent.TimeUnit arg){
        System.out.println("FailSlowAgent.java | traceAwait");
        long res=-1;
        if(FailSlowAgent.traceTimeOut){
            try{
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
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
        System.out.println("FailSlowAgent.java | traceTryAcquire");
        long res=-1;
        if(FailSlowAgent.traceTimeOut){
            try{
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
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
        System.out.println("FailSlowAgent.java | traceJoin");
        if(FailSlowAgent.traceTimeOut){
            try{
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
                String className=Thread.currentThread().getStackTrace()[2].getClassName();
                String funcName=Thread.currentThread().getStackTrace()[2].getMethodName();
                access.reportJoin(time,className,funcName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static long traceJoin(long time,int nanos){
        System.out.println("FailSlowAgent.java | traceJoin");
        long res=-1;
        if(FailSlowAgent.traceTimeOut){
            try{
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
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
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
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
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://localhost:2359/failslow");
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
}