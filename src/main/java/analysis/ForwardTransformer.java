package analysis;

import com.alibaba.fastjson.JSONObject;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ForwardTransformer extends SceneTransformer {
    public static String jarLoc = null;
    public static CallGraph cg = null;

    public ForwardTransformer(String jarLoc) {
        ForwardTransformer.jarLoc = jarLoc;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> map) {

        cg = Scene.v().getCallGraph();
        int count = 0;
        int clintnum = 0;
        Iterator<Edge> iterator = cg.listener();
        while (iterator.hasNext()) {
            Edge edge = iterator.next();
            JSONObject jsonObject = new JSONObject();

            try {
                if (edge.getSrc().method().toString().contains("com.example")
                        || edge.getTgt().method().toString().contains("com.example")
                        || edge.getTgt().method().toString().contains("synthetic.method")
                        || edge.getSrc().method().toString().contains("synthetic.method")) {
                    jsonObject.put("srcMethod", edge.getSrc().method().toString());
                    jsonObject.put("tgtMethod", edge.getTgt().method().toString());
                    System.out.println(jsonObject);
                    count++;
                    if (edge.getSrc().method().toString().contains("<clinit>")
                            || edge.getTgt().method().toString().contains("<clinit>")) {
                        clintnum++;
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();

            }
        }

        for (SootMethod modelFourServiceImpl : Scene.v().getSootClass("FieldFlow").getMethods()) {

            for (Local local : modelFourServiceImpl.retrieveActiveBody().getLocals()) {
                System.out.println(local);
                PointsToAnalysis points = Scene.v().getPointsToAnalysis();
                System.out.println(points.reachingObjects(local));
            }
        }

        JSONObject jsonObject = new JSONObject();
        ReachableMethods reachableMethods = Scene.v().getReachableMethods();
        System.out.println("total reachableMethods : " + reachableMethods.size());
        Iterator<MethodOrMethodContext> methodIterator = reachableMethods.listener();
        Set<String> methods = new HashSet<>();
        while (methodIterator.hasNext()) {
            MethodOrMethodContext methodOrMethodContext = methodIterator.next();
            if (methodOrMethodContext.method().toString().contains("com.example") && !methodOrMethodContext.method().getName().contains("synthetic")) {
                methods.add(methodOrMethodContext.method().toString());
            }
        }
        System.out.println("real CG edge: " + count);
        System.out.println("clint edge: " + clintnum);
        jsonObject.put("ReachableMethods", methods);
        System.out.println("reachable Method : " + methods.size());
        System.out.println("cg size:" + cg.size());
    }
}