package jsonutils;

import java.util.ArrayList;
import java.util.List;

public class TimeOutPoint{
    
    public int timeoutMethodType;
    
    public String toClassName;
    public String toFuncName;
    public int toPos;

    // customized timeout value
    // left:1  right:2
    public int lr=0;
    // 1:add 2:cmp
    public int traceType=0;
    // compare position cmp|if
    public int cmpPos=0;

    // timeout handler
    // type 1: Timeout handler is in if {block} or else {block} or next block of If_Stmt
    // type 2: General timeout -> dependency analysis of flag
    // type 3: Record some information (not our target)
    // type 4: Do something at special time (not our target)
    // public int handlerType=0;

    // if timeout handler in if true statements : true
    // otherwise : false
    // public boolean handlerInIftrue=false;

    // public long timeoutValue=-1;
    public List<Long> toVList=new ArrayList<>();
}
