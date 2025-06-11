package client;

import java.io.*;
import java.util.List;
import java.util.zip.CheckedOutputStream;

import org.apache.jute.BinaryInputArchive;
import org.apache.jute.BinaryOutputArchive;
import org.apache.jute.InputArchive;
import org.apache.jute.OutputArchive;
import org.apache.jute.Record;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.server.DataNode;
import org.apache.zookeeper.server.DataTree;
import org.apache.zookeeper.server.persistence.SnapStream;
import org.apache.zookeeper.txn.CreateTxn;
import org.apache.zookeeper.txn.DeleteTxn;
import org.apache.zookeeper.txn.TxnHeader;


public class UnitTest {
    public static void main(String[] args)throws Exception{
        UnitTest unitTest=new UnitTest();
        // ZK-4689 | 2234 | 3306
        unitTest.testACLCreatedDuringFuzzySnapshotSync();
        System.out.println("Seperate ---------------------------------");
        unitTest.testACLCreatedDuringFuzzySnapshotSync1();
    }
    public void testACLCreatedDuringFuzzySnapshotSync() throws IOException {
        System.out.println("Start test");
        byte[] content=new byte[9000];
        DataTree leaderDataTree = new DataTree();
        // Add couple of transaction in-between.
        TxnHeader hdr1 = new TxnHeader(1, 2, 2, 2, ZooDefs.OpCode.create);
        Record txn1 = new CreateTxn("/a1", content, ZooDefs.Ids.OPEN_ACL_UNSAFE, false, -1);
        leaderDataTree.processTxn(hdr1, txn1);  

        // Start the simulated snap-sync by serializing ACL cache.
        // File file = File.createTempFile("snapshot", "zk");
        File file =new File("/mnt/slow/ut.zk");
        // FileOutputStream os = new FileOutputStream(file);
        CheckedOutputStream snapOS=SnapStream.getOutputStream(file, true);
        OutputArchive oa = BinaryOutputArchive.getArchive(snapOS);
        long st1=System.currentTimeMillis();
        leaderDataTree.serializeAcls(oa);
        long et1=System.currentTimeMillis();
        System.out.println("elapsedTime1:"+(et1-st1));


        // Finish the snapshot.
        long st2=System.currentTimeMillis();
        leaderDataTree.serializeNodes(oa);
        long et2=System.currentTimeMillis();
        System.out.println("elapsedTime2:"+(et2-st2));
        snapOS.close();

        // Simulate restore on follower and replay.
        FileInputStream is = new FileInputStream(file);
        InputArchive ia = BinaryInputArchive.getArchive(is);
        DataTree followerDataTree = new DataTree();
        // execute shell command
        Process process=Runtime.getRuntime().exec("./drop.sh");
        try{
            process.waitFor();
            BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line=null;
            while((line=reader.readLine())!=null){
                System.out.println("Shell:"+line);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        long st3=System.currentTimeMillis();
        followerDataTree.deserialize(ia, "tree");
        long et3=System.currentTimeMillis();
        System.out.println("elapsedTime3:"+(et3-st3));
        // followerDataTree.processTxn(hdr1, txn1);

        DataNode a1 = leaderDataTree.getNode("/a1");
        List<ACL> leaderACLList=leaderDataTree.getACL(a1);
        List<ACL> followerACLList=followerDataTree.getACL(a1);
        System.out.println("CREATOR_ALL_ACL:"+ZooDefs.Ids.CREATOR_ALL_ACL);
        System.out.println("leader:"+leaderACLList);
        System.out.println("follower:"+followerACLList);
        System.out.println("End test");
        // assertNotNull(a1);
        // assertEquals(ZooDefs.Ids.CREATOR_ALL_ACL, leaderDataTree.getACL(a1));
        // assertEquals(ZooDefs.Ids.CREATOR_ALL_ACL, followerDataTree.getACL(a1));
    }
    public void testACLCreatedDuringFuzzySnapshotSync1() throws IOException {
        DataTree leaderDataTree = new DataTree();
        // Populate the initial ACL cache with some entries before snapshotting.
        TxnHeader hdr1 = new TxnHeader(1, 2, 2, 2, ZooDefs.OpCode.create);
        Record txn1 = new CreateTxn("/a1", "foo".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL, false, -1);
        leaderDataTree.processTxn(hdr1, txn1);

        TxnHeader hdr2 = new TxnHeader(1, 2, 3, 2, ZooDefs.OpCode.delete);
        Record txn2 = new DeleteTxn("/a1");
        leaderDataTree.processTxn(hdr2, txn2);

        // Start the simulated snap-sync by serializing ACL cache.
        File file = File.createTempFile("snapshot", "zk");
        FileOutputStream os = new FileOutputStream(file);
        OutputArchive oa = BinaryOutputArchive.getArchive(os);
        leaderDataTree.serializeAcls(oa);

        // Add couple of transaction in-between.
        TxnHeader hdr3 = new TxnHeader(1, 2, 4, 2, ZooDefs.OpCode.create);
        Record txn3 = new CreateTxn("/a1", "foo".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, false, -1);
        leaderDataTree.processTxn(hdr3, txn3);

        // Finish the snapshot.
        leaderDataTree.serializeNodes(oa);
        os.close();

        // Simulate restore on follower and replay.
        FileInputStream is = new FileInputStream(file);
        InputArchive ia = BinaryInputArchive.getArchive(is);
        DataTree followerDataTree = new DataTree();
        followerDataTree.deserialize(ia, "tree");
        is.close();
        followerDataTree.processTxn(hdr3, txn3);

        // check
        DataNode a1FromLeader = leaderDataTree.getNode("/a1");
        System.out.println("leaderACL:"+leaderDataTree.getACL(a1FromLeader));
        DataNode a1FromFollower = followerDataTree.getNode("/a1");
        System.out.println("followerACl:"+followerDataTree.getACL(a1FromFollower));
    }
}
