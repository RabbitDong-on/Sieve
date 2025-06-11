package jsonutils;

public class ConflictPair {
    public int toId;
    public int noId;
    public MemAccess toMemAccess=new MemAccess();
    public MemAccess normalMemAccess=new MemAccess();
    @Override
    public boolean equals(Object obj){
        if(obj instanceof ConflictPair){
            ConflictPair conflictPair=(ConflictPair)obj;
            // for object id
            if(this.toMemAccess.className.equals(conflictPair.toMemAccess.className)&&
            this.toMemAccess.fieldName.equals(conflictPair.toMemAccess.fieldName)&&
            // this.toMemAccess.hashCode==conflictPair.toMemAccess.hashCode&&
            // for pos
            this.toMemAccess.funcName.equals(conflictPair.toMemAccess.funcName)&&
            this.toMemAccess.lineNum==conflictPair.toMemAccess.lineNum&&

            this.normalMemAccess.className.equals(conflictPair.normalMemAccess.className)&&
            this.normalMemAccess.fieldName.equals(conflictPair.normalMemAccess.fieldName)&&
            // this.normalMemAccess.hashCode==conflictPair.normalMemAccess.hashCode&&
            this.normalMemAccess.funcName.equals(conflictPair.normalMemAccess.funcName)&&
            this.normalMemAccess.lineNum==conflictPair.normalMemAccess.lineNum){
                return true;
            }
        }
        return false;
    }
}
