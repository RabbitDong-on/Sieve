package client;

// import java.rmi.Naming;

// import scheduler.SchedulerService;

public class ZooKeeperDCTMain {
    // option + id
    public static void main(String[] args){
        int retryConnect=100;
        // try to connect with zoo server
        int id=Integer.parseInt(args[0]);
        int testNum=Integer.parseInt(args[1]);
        int curI=0;
        while(retryConnect>0){
            try{
                ZooKeeperClient zkClient=new ZooKeeperClient("localhost:2181", 25000);
                System.out.println("ZooKeeperDCTMain | connected!");
                // option + id
                // SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                while(curI<testNum){
                    System.out.println("ZooKeeperDCTMain | trial:"+curI);
                    zkClient.operate(args[0], 1,1);
                    zkClient.operate(args[0], 2,-1);
                    zkClient.operate(args[0], 1, 2);
                    curI++;
                }
                // finish the whole experiment
                zkClient.close();
                // access.isGoingOn(id);
                break;
            }catch(Exception e){
                e.printStackTrace();
                String message=e.getMessage();
                if(message!=null&&message.equals("ZooKeeper client timeout in starting")){
                    retryConnect--;
                }
                if(retryConnect==0){
                    // e.printStackTrace();
                    System.out.println("ZooKeeperDCTMain | client connect failed!");
                    return ;
                }
            }
        }
        System.out.println("ZooKeeperDCTMain | endTest!");
    }
}
