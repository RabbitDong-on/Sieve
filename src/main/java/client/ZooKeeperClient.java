package client;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;


public class ZooKeeperClient implements AutoCloseable {
    private final ZooKeeper zk;

    public ZooKeeperClient(final String addr, final int timeout) throws Exception {
        final CountDownLatch connectedSignal = new CountDownLatch(1);
        this.zk = new ZooKeeper(addr, timeout, watchedEvent -> {
            if (watchedEvent.getState() == KeeperState.SyncConnected) {
                connectedSignal.countDown();
            }
        });
        if (!connectedSignal.await(timeout, TimeUnit.MILLISECONDS)) {
            throw new Exception("ZooKeeper client timeout in starting");
        }
    }

    public final synchronized void create(final String path, final byte[] data,int acl) throws Exception {
        if(acl==1){
            zk.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }else{
            zk.create(path, data, Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT);
        }
    }

    public final synchronized boolean exists(final String path) throws Exception {
        return zk.exists(path, false) != null;
    }

    public final synchronized byte[] getData(final String path) throws Exception {
        return zk.getData(path, false, null);
    }

    public final synchronized void setData(final String path, final byte[] data) throws Exception {
        zk.setData(path, data, -1);
    }

    public final synchronized void delete(final String path) throws Exception {
        zk.delete(path, -1);
    }

    public final synchronized List<String> getChildren(final String path) throws Exception {
        return zk.getChildren(path, false);
    }

    public final synchronized void setACL(final String path,int type) throws Exception{
        Stat stat=new Stat();
        System.out.println("ZooKeeperClient | setACL");
        zk.getACL(path, stat);
        if(type==1){
            zk.setACL(path, ZooDefs.Ids.CREATOR_ALL_ACL, stat.getAversion());
        }else{
            zk.setACL(path, ZooDefs.Ids.OPEN_ACL_UNSAFE, stat.getAversion());
        }
    }

    public final synchronized void getACL(final String path,int type) throws Exception{
        Stat stat=new Stat();
        System.out.println("ZooKeeperClient | getACL");
        zk.getACL(path, stat);
    }

    public final synchronized void multiCreate(final String path) throws Exception{
        Stat stat=new Stat();
        System.out.println("ZooKeeperClient | multiCreate");
        zk.multi(Arrays.asList(Op.create(path, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)));
    }
    @Override
    public final synchronized void close() throws Exception {
        zk.close();
    }
    public void operate(String node,int type,int acl)throws Exception{
        int retryOp=3;
        byte[] content1=new byte[9000];
        content1[1]='1';
        byte[] content2=new byte[9000];
        content2[2]='2';
        while(retryOp>0){
            try{
                if(type==1){
                    this.create("/"+node, content1,acl);
                }else if(type==2){
                    this.delete("/"+node);
                }else if(type==3){
                    this.setData("/"+node, content2);
                }else if(type==4){
                    this.getData("/"+node);
                }else if(type==5){
                    this.setACL("/"+node, acl);
                }else if(type==6){
                    this.multiCreate(node);
                }
                System.out.println("ZooKeeperClient | success");
                return ;
            }catch(Exception re){
                System.out.println("ZooKeeperClient | exception!");
                re.printStackTrace();
                String message=re.getMessage();
                if(message!=null){
                    System.out.println("ZooKeeperClient | "+message);
                    if(message.contains("Session expired")){
                        System.out.println("ZooKeeperClient | contains Session expired");
                    }
                    if(message.contains("ConnectionLoss")){
                        System.out.println("ZooKeeperClient | contains ConnectionLoss");
                    }
                }
                if(message!=null&&(message.contains("ConnectionLoss")||message.contains("Session expired"))){
                    System.out.println("ZooKeeperClient | enter");
                    retryOp--;
                    if(retryOp==0){
                        System.out.println("ZooKeeperClient | reconnect!");
                        this.close();
                        throw new Exception("ZooKeeper client timeout in starting");
                    }
                }else{
                    if(message==null){
                        re.printStackTrace();
                    }
                    retryOp=0;
                }
            }
        }
        System.out.println("ZooKeeperClient | node existed or disappear!");
    }
}
