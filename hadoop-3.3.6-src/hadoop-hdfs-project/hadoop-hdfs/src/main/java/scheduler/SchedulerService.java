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
}