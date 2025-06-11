package config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;
import soot.util.Chain;

/*
 * SootConfig
 * Set up soot configuration parameter
 */
public class SootConfig{
    public static List<String> excludeClassList;

    public static void setupSoot(String sysClassPath){
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_output_format(Options.output_format_jimple);
        // TODOEXTEND
        Options.v().set_process_dir(Collections.singletonList(sysClassPath));
        Options.v().set_whole_program(true);
        Options.v().set_verbose(true);
        // Options.v().setPhaseOption("cg", "off");
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Scene.v().addBasicClass("io.netty.channel.ChannelFutureListener",SootClass.HIERARCHY);
        // process for scala
        Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcB$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcC$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcF$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcS$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcV$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcZ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcDD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcDF$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcDI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcDJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcFD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcFF$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcFI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcFJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcID$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcIF$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcII$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcIJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcJD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcJF$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcJI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcJJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcVD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcVF$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcVI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcVJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcZD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcZF$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcZI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcZJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcDDD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcDDI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcDDJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcDID$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcDII$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcDIJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcDJD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcDJI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcDJJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcFDD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcFDI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcFDJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcFID$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcFII$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcFIJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcFJD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcFJI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcFJJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcIDD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcIDI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcIDJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcIID$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcIII$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcIIJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcIJD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcIJI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcIJJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcJDD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcJDI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcJDJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcJID$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcJII$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcJIJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcJJD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcJJI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcJJJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcVDD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcVDI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcVDJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcVID$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcVII$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcVIJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcVJD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcVJI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcVJJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcZDD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcZDI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcZDJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcZID$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcZII$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcZIJ$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcZJD$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcZJI$sp",SootClass.HIERARCHY);
        Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcZJJ$sp",SootClass.HIERARCHY);
        Scene.v().loadNecessaryClasses();
        // SootClass sootClass=Scene.v().loadClassAndSupport("faultpointanalysis.DelayParser");
        // sootClass.setApplicationClass();

        //add to-exclude classes
        // Options.v().set_exclude(addExcludeClasses());
        PackManager.v().runPacks();
        //enable spark call-graph construction
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().setPhaseOption("cg.spark", "enabled:true");
        Options.v().setPhaseOption("cg.spark", "verbose:true");
        Options.v().setPhaseOption("cg.spark", "on-fly-cg:true");
        Options.v().setPhaseOption("cg.spark","cs-demand:true");
        // append only significantly speed up analysis
        Options.v().setPhaseOption("cg.spark","apponly:true");
    }

    public static void getBasicInfo(){
        //obtain class contains main method
        SootClass maincClass=Scene.v().getMainClass();
        //obtain main method
        SootMethod mainMethod=Scene.v().getMainMethod();
        //obtain libclass,applicationclass,basicclass,allclass
        Chain<SootClass> libClasses=Scene.v().getLibraryClasses();
        Chain<SootClass> applicationClasses=Scene.v().getApplicationClasses();
        Set<String>basicclass=Scene.v().getBasicClasses();
        Chain<SootClass>classes=Scene.v().getClasses();
        //obtain soot class path
        String sootClassPath=Scene.v().getSootClassPath();
        // obtain jvm class path
        String jvmClassPath=Scene.v().defaultClassPath();
    }
    //add excluded classes
    public static List<String> addExcludeClasses(){
        if(excludeClassList==null){
            excludeClassList=new ArrayList<String>();
        }
        excludeClassList.add("java.");
        excludeClassList.add("javax.");
        excludeClassList.add("sun.");
        excludeClassList.add("sunw.");
        excludeClassList.add("com.sun.");
        excludeClassList.add("com.ibm.");
        return excludeClassList;
    }
}