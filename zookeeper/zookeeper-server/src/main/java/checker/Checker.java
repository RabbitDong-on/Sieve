package checker;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.*;
/*
 * 0. data corruption
 * 1. hang
 * 2. abnormal exception : log scan
 * 3. approximate Panorama
 *    3-1. part of client fail to finish their workloads
 *    3-2. internal checker indicates a node is active, but the node`s clients report errors
 *    3-3. a fault is injected in one node, but only another node`s clients report errors
 */
public class Checker{
    private static final CountDownLatch connectedSignal=new CountDownLatch(1);
    public static void main(String[] args) throws Exception {
        String connectString = "localhost:2181"; 
        int sessionTimeout = 3000; 
        Watcher watcher = event -> {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connectedSignal.countDown();
            }
        };

        ZooKeeper zooKeeper = new ZooKeeper(connectString, sessionTimeout, watcher);

        connectedSignal.await();
        List<String> res=new ArrayList<>();
        for(int i=1;i<=30;i++){
            String path="/"+i;
            String temp=getDataExample(zooKeeper, path);
            if(temp!=null){
                res.add(temp);
            }
        }
        zooKeeper.close();
    }
    private static String getDataExample(ZooKeeper zooKeeper, String path)throws Exception{
        String dataStr=null;
        try{
            byte[] data=zooKeeper.getData(path,false,null);
            dataStr=new String(data);
            System.out.println("Data at path:"+path+": "+dataStr);
        }catch(Exception e){
        }
        return dataStr;
    }
}
