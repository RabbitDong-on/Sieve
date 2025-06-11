package jsonutils;

import java.util.ArrayList;
import java.util.List;


public class DP2ConflictPair {
    public String sysName;
    // delay point
    public String className;
    public String funcName;
    public int lineNum;
    // conflict pairs
    // public Map<MemAccess,MemAccess> conflictedPairs=new LinkedHashMap<>();
    public List<ConflictPair> conflictPairs=new ArrayList<>();
}
