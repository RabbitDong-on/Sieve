package scheduler;
import utils.*;
import java.rmi.Remote;
import java.rmi.RemoteException;


public interface SchedulerService extends Remote{

    ScheduleRes isInjectDelay(ScheduleInfo info) throws RemoteException;
    ScheduleRes isInjectException(ScheduleInfo info) throws RemoteException;


    void isGoingOn(int id) throws RemoteException;

    void startTrial(int id) throws RemoteException;

    void endTrial(int id) throws RemoteException;

    void isRestart(int id) throws RemoteException;
 
    void isCheck(int id) throws RemoteException;

    void isRecord() throws RemoteException;


    void reportConnect(int time,String toClassName,String toFuncName)throws RemoteException;
    void reportWait(long time,String toClassName,String toFuncName)throws RemoteException;
    long reportWait(long time,int nanos,String toClassName,String toFuncName)throws RemoteException;
    void reportAwait(long time,String toClassName,String toFuncName)throws RemoteException;
    long reportAwait(long time,java.util.concurrent.TimeUnit arg,String toClassName,String toFuncName)throws RemoteException;
    long reportTryAcquire(long time,java.util.concurrent.TimeUnit arg,String toClassName,String toFuncName)throws RemoteException;
    void reportJoin(long time,String toClassName,String toFuncName)throws RemoteException;
    long reportJoin(long time,int nanos,String toClassName,String toFuncName)throws RemoteException;
    long reportAddSub(long time,String toClassName,String toFuncName)throws RemoteException;
    long reportCMP(long time,String toClassName,String toFuncName)throws RemoteException;
    void reportCMP(int time,String toClassName,String toFuncName)throws RemoteException;
    void reportTimeOutHandler(String className,String funcName,int lineNum)throws RemoteException;
    void reportReadAccess(String className,String fieldName,String funcName,long lineNum,int hashCode,long tId,int nodeId)throws RemoteException;
    void reportWriteAccess(String className,String fieldName,String funcName,long lineNum,int hashCode,long tId,int nodeId)throws RemoteException;
    void reportSleep(long time)throws RemoteException;
    void Await(long seq)throws RemoteException;
    void Signal(long seq)throws RemoteException;
}