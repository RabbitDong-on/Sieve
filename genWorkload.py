import sys
import random

# zookeeper instruction
def lscmd_zoo(path):
    # type 0
    # ls path
    sys.stdout.write("ls "+path+"\n")
        
def createcmd_zoo(path,context):
    # type 1
    # create -mode path context acl
    type=random.randint(0,3)
    if type==0:
        mode="-s"
    elif type==1:
        mode="-e"
    elif type==2:
        mode="-s -e"
    else:
        mode=""
    mode=""
    sys.stdout.write("create "+mode+" "+path+" "+context+"\n")

def getcmd_zoo(path):
    # type 2
    # get path
    sys.stdout.write("get "+path+"\n")

def setcmd_zoo(path,context):
    # type 3
    # set path context
    type=random.randint(1,2)
    if type==0:
        sys.stdout.write("set "+path+" "+context+" "+str(type)+"\n")
    else:
        sys.stdout.write("set "+path+" "+context+"\n")
        
def statcmd_zoo(path):
    # type 4
    # stat path
    sys.stdout.write("stat "+path+"\n")

def watchcmd_zoo(path):
    # type 5
    # get path watch
    sys.stdout.write("get -w "+path+"\n")

def deletecmd_zoo(path):
    # type 6
    # delete path
    type=random.randint(0,2)
    if type==0:
        sys.stdout.write("delete -v "+str(type)+" "+path+"\n")
    else:
        sys.stdout.write("delete "+path+"\n")

def getaclcmd_zoo(path):
    # type 7
    # getacl path
    sys.stdout.write("getAcl "+path+"\n")

def setaclcmd_zoo(path):
    # type 8
    # setAcl path world:anyone:cdrwa
    type=random.randint(0,3)
    if type==0:
        sys.stdout.write("setAcl "+path+" world:anyone:drwa\n")
    elif type==1:
        sys.stdout.write("setAcl "+path+" world:anyone:crwa\n")
    elif type==2:
        sys.stdout.write("setAcl "+path+" world:anyone:cdwa\n")
    else:
        sys.stdout.write("setAcl "+path+" world:anyone:cdra\n")

def genWorkload_zoo_random(num):
    sys.stdout.write("$CMD<<EOF\n")
    context="XX"
    count=0
    while count<num:
        count+=1
        type=random.randint(0,8)
        seed=random.randint(0,8)
        # gen path
        if seed<2:
            dir2=random.randint(0,1)
            path="/"+str(type)+"/"+str(dir2)
        else:
            path="/"+str(type)
        # gen op
        if type==0:
            lscmd_zoo(path)
        elif type==1:
            createcmd_zoo(path,context)
        elif type==2:
            getcmd_zoo(path)
        elif type==3:
            setcmd_zoo(path,context)
        elif type==4:
            statcmd_zoo(path)
        elif type==5:
            watchcmd_zoo(path)
        elif type==6:
            deletecmd_zoo(path)
        elif type==7:
            getaclcmd_zoo(path)
        else:
            setaclcmd_zoo(path)
    sys.stdout.write("EOF\n")

def genWorkload_zoo_py(num,id):
    sys.stdout.write("$CMD<<EOF\n")
    context1="123"
    context2="456"
    count=0
    base=0
    if id=='1':
        base=0
    elif id=='2':
        base=num
    elif id=='3':
        base=num*2
    while count<num:
        count+=1
        path="/"+str(count+base)
        createcmd_zoo(path,"0")
        getcmd_zoo(path)
        watchcmd_zoo(path)

    count=0
    while count<num:
        count+=1
        path="/"+str(count+base)
        setcmd_zoo(path,context1)
        setcmd_zoo(path,context2)
    count=0 
    while count<num:
        count+=1
        path="/"+str(count+base)
        createcmd_zoo(path,"0")
        getcmd_zoo(path)
        statcmd_zoo(path)
        getaclcmd_zoo(path)
    sys.stdout.write("EOF\n")

def genWorkload_zoo_dc1(num,id):
    sys.stdout.write("$CMD<<EOF\n")
    context1="123"
    context2="456"
    count=0
    base=0
    if id=='1':
        base=0
    elif id=='2':
        base=num
    elif id=='3':
        base=num*2
    while count<num:
        count+=1
        path="/"+str(count+base)
        createcmd_zoo(path,"0")
    count=0
    while count<num:
        count+=1
        path="/"+str(count+base)
        setcmd_zoo(path,context1)
    sys.stdout.write("EOF\n")

def genWorkload_zoo_dc2(num,id):
    sys.stdout.write("$CMD<<EOF\n")
    context2="456"
    count=0
    base=0
    if id=='1':
        base=0
    elif id=='2':
        base=num
    elif id=='3':
        base=num*2
    while count<num:
        count+=1
        path="/"+str(count+base)
        setcmd_zoo(path,context2)
    sys.stdout.write("EOF\n")

# hdfs instruction
def lscmd_hdfs(path):
    # type=0
    type=random.randint(0,1)
    if type==0:
        sys.stdout.write("hdfs dfs -ls "+path+"\n")
    else:
        sys.stdout.write("hdfs dfs -ls -R "+path+"\n")

def ducmd_hdfs(path):
    # type=1
    type=random.randint(0,1)
    if type==0:
        sys.stdout.write("hdfs dfs -du "+path+"\n")
    else:
        sys.stdout.write("hdfs dfs -du -s "+path+"\n")

def countcmd_hdfs(path):
    # type=2
    sys.stdout.write("hdfs dfs -count "+path+"\n")

def mvcmd_hdfs(path1,path2):
    # type=3
    sys.stdout.write("hdfs dfs -mv "+path1+" "+path2+"\n")

def cpcmd_hdfs(path1,path2):
    # type=4
    sys.stdout.write("hdfs dfs -cp "+path1+" "+path2+"\n")

def rmcmd_hdfs(path):
    # type=5
    type=random.randint(0,1)
    if type==0:
        sys.stdout.write("hdfs dfs -rm "+path+"\n")
    else:
        sys.stdout.write("hdfs dfs -rm -r "+path+"\n")

def putcmd_hdfs(path1,path2):
    # type=6
    # put file hdfspath
    sys.stdout.write("hdfs dfs -put "+path1+" "+path2+"\n")

def getcmd_hdfs(path1,path2):
    # type=7
    # get hdfspath localpath
    sys.stdout.write("hdfs dfs -get "+path1+" "+path2+"\n")

def lookupcmd_hdfs(path):
    # type=8
    sys.stdout.write("hdfs dfs -cat "+path+"\n")

def mkdir_hdfs(path):
    # type=9
    sys.stdout.write("hdfs dfs -mkdir "+path+"\n")

def genWorkload_hdfs(id):
    dir="/"+id
    subdir=dir+"/XX"
    localfile="zoo.cfg"
    mkdir_hdfs(dir)
    mkdir_hdfs(subdir)
    putcmd_hdfs(localfile,dir)
    cpcmd_hdfs(dir+"/"+localfile,subdir)
    lookupcmd_hdfs(subdir+"/"+localfile)
    getcmd_hdfs(dir+"/"+localfile,"./")
    ducmd_hdfs(dir+"/"+localfile)
    rmcmd_hdfs(subdir+"/"+localfile)

if __name__ =="__main__":
    sysName=sys.argv[1]
    id=sys.argv[2]
    kind=sys.argv[3]
    if sysName=='zoo':
        if kind=='0':
            genWorkload_zoo_py(50,id)
            # genWorkload_zoo_random(30)
        elif kind=='1':
            genWorkload_zoo_dc1(10,id)
        elif kind=='2':
            genWorkload_zoo_dc2(10,id)
    elif sysName=='hdfs':
        genWorkload_hdfs(id)
    else:
        print("todo")