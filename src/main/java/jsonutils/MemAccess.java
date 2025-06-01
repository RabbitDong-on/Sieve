package jsonutils;

public class MemAccess {
    // type 1:Write  2:Read
    public int type;
    // information
    public String className;
    public String fieldName;
    public String funcName;
    // static:-1  non-static:this.hashCode
    public int hashCode;
    public long tId;
    public int nodeId;
    // pos: field access position
    public int lineNum;
    // time stamp

    @Override
    public boolean equals(Object obj){
        if(obj instanceof MemAccess){
            MemAccess memAccess=(MemAccess)obj;
            if(this.className.equals(memAccess.className)&&
                this.fieldName.equals(memAccess.fieldName)&&
                this.funcName.equals(memAccess.funcName)&&
                this.hashCode==memAccess.tId&&
                this.tId==memAccess.tId&&
                this.nodeId==memAccess.nodeId&&
                this.type==memAccess.type&&
                this.lineNum==memAccess.lineNum){
                return true;
            }
        }
        return false;
    }
}
