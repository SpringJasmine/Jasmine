package analysis;

import bean.AOPTargetModel;
import bean.AspectModel;
import bean.InsertMethod;
import enums.AdviceEnum;
import mock.MockObject;
import mock.MockObjectImpl;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JIdentityStmt;
import utils.JimpleUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName AOPAnalysis
 * @Description Processing of AOP annotations
 **/
public class AOPAnalysis {

    private final JimpleUtils jimpleUtils = new JimpleUtils();
    public static Map<String, InsertMethod> insertMethodMap = new HashMap<>();
    public static boolean newVersion = false;

    /**
     * Processing advice method and target method
     *
     * @param targetModel advice method and target method
     */
    public void processWeave(AOPTargetModel targetModel) {
        AOPParser ap = new AOPParser();
        // The target method is still used as the actual business logic
        SootMethod targetMethod = targetModel.getSootMethod();
        SootMethod proxyMethod = targetModel.getProxyMethod();
        modifyJimpleBody(proxyMethod);
        SootMethod currentMethod = proxyMethod;
        SootMethod preMethod = proxyMethod;
        AdviceEnum currentEnum = null;



        // Process advice method set of target method
        for (AspectModel aspectModel : targetModel.getAdvices()) {
            switch (aspectModel.getAnnotation()) {
                case AOP_AROUND:
                    preMethod = currentMethod;
                    SootMethod aroundMethod = ap.aroundParser(aspectModel, targetMethod);
                    ap.insertAOPAround(currentMethod, aroundMethod);
                    currentMethod = aroundMethod;
                    currentEnum = AdviceEnum.AOP_AROUND;
                    break;
                case AOP_BEFORE:
                    ap.insertAOPBefore(currentMethod, aspectModel.getSootMethod());
                    break;
                case AOP_AFTER:
                    SootMethod insertTargetMethod;
                    if (newVersion) {
                        insertTargetMethod = currentMethod;
                    } else {
                        insertTargetMethod = preMethod;
                    }
                    ap.insertAOPAfter(insertTargetMethod, aspectModel.getSootMethod());
                    break;
                case AOP_AFTER_RETURNING:
                    if (newVersion) {
                        insertTargetMethod = currentMethod;
                    } else {
                        insertTargetMethod = preMethod;
                    }
                    ap.insertAOPAfterReturning(insertTargetMethod, aspectModel.getSootMethod(), aspectModel.getPointcutExpressions());
                    break;
                case AOP_AFTER_THROWING:
                    break;
            }
        }
        ap.insertAOPTarget(currentMethod, targetMethod, currentEnum);
    }

    /**
     * construct function for proxy method
     *
     * @param method proxy method
     */
    public void modifyJimpleBody(SootMethod method) {
        MockObject mockObject = new MockObjectImpl();
        JimpleBody body = (JimpleBody) method.retrieveActiveBody();
        List<Integer> returnList = new ArrayList<>();
        List<Integer> insertPointList = new ArrayList<>();
        PatchingChain<Unit> units = body.getUnits();
        units.removeIf(unit -> !(unit instanceof JIdentityStmt || unit.toString().contains("localTarget = this.")));
        mockObject.mockJoinPoint(body, units);

        Type returnType = method.getReturnType();
        if (returnType instanceof VoidType) {
            jimpleUtils.addVoidReturnStmt(units);
        } else {
            Local returnRef = null;
            for (Local local : body.getLocals()) {
                if(local.getName().equals("returnRef")){
                    returnRef = local;
                    break;
                }
            }
            if(returnRef == null){
                returnRef = jimpleUtils.addLocalVar("returnRef", returnType, body);
            }
            jimpleUtils.addCommonReturnStmt(returnRef, units);
        }
        returnList.add(units.size() - 1);
        insertPointList.add(units.size() - 1);
        insertMethodMap.put(method.toString(), new InsertMethod(method, returnList, insertPointList));
    }
}
