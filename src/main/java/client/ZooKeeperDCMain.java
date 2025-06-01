package client;

import java.rmi.Naming;
import scheduler.SchedulerService;

public class ZooKeeperDCMain {
    // option + id
    public static void main(String[] args){
        int retryConnect=100;
        int continueCount=0;
        // try to connect with zoo server
        int id=Integer.parseInt(args[0]);
        int testNum=Integer.parseInt(args[1]);
        int threshold=5;
        int curI=0;
        boolean isExecuted=false;
        while(retryConnect>0){
            try{
                ZooKeeperClient zkClient=new ZooKeeperClient("localhost:2181", 25000);
                System.out.println("ZooKeeperDCMain | connected!");
                // option + id
                SchedulerService access=(SchedulerService)Naming.lookup("rmi://172.30.0.5:2359/failslow");
                while(curI<testNum){
                    if(curI>=threshold){
                        if(!isExecuted){
                            System.out.println("ZooKeeperDCMain | startTrial+");
                            access.startTrial(id);
                            isExecuted=true;
                            System.out.println("ZooKeeperDCMain | startTrial-");
                        }
                    }
                    System.out.println("ZooKeeperDCMain | trial:"+curI);
                    zkClient.operate(args[0], 1,1);
                    zkClient.operate(args[0], 2,-1);
                    // sleep for server mem collection if curI<threshold
                    if(curI<threshold){
                        Thread.sleep(2*1000);
                    }
                    if(curI>=threshold){
                        if(isExecuted){
                            System.out.println("ZooKeeperDCMain | endTrial+");
                            access.endTrial(id);
                            curI++;
                            isExecuted=false;
                            System.out.println("ZooKeeperDCMain | endTrial-");
                        }
                    }else{
                        curI++;
                    }
                    continueCount=0;
                }
                // finish the whole experiment
                zkClient.close();
                access.isGoingOn(id);
                break;
            }catch(Exception e){
                System.out.println("ZooKeeperDCMain | exception!");
                e.printStackTrace();
                String message=e.getMessage();
                if(message!=null){
                    System.out.println("ZooKeeperDCMain | "+message);
                }
                if(message!=null&&message.equals("ZooKeeper client timeout in starting")){
                    retryConnect--;
                    continueCount++;
                }
                try{
                    if(continueCount==5){
                            Thread.sleep(60*1000);
                        
                    }else if(continueCount>5){
                        Thread.sleep(120*1000);
                    }
                }catch(Exception se){

                }
                if(retryConnect==0){
                    // e.printStackTrace();
                    System.out.println("ZooKeeperDCMain | client connect failed!");
                    return ;
                }
            }
        }
        System.out.println("ZooKeeperDCMain | endTest!");
    }
}
