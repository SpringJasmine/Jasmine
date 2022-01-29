package mock;

import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.StringConstant;
import utils.JimpleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @ClassName MockObjectImpl
 * @Description Simulate the initialization behavior of the class
 **/
public class MockObjectImpl implements MockObject {
    private final JimpleUtils jimpleUtils = new JimpleUtils();
    GenerateSyntheticClass gsc = new GenerateSyntheticClassImpl();

    /**
     * Simulate the behavior of JoinPoint in the program
     *
     * @param body  the method body which contains to JoinPoint initialized
     * @param units the method body which contains to JoinPoint initialized
     */
    @Override
    public void mockJoinPoint(JimpleBody body, PatchingChain<Unit> units) {
        SootClass abstractClass = Scene.v().getSootClass("org.aspectj.lang.ProceedingJoinPoint");
        SootClass joinPointImpl = gsc.generateJoinPointImpl(abstractClass);
        Local joinPointLocal = jimpleUtils.addLocalVar(joinPointImpl.getShortName(), joinPointImpl.getName(), body);
        Local paramArray = jimpleUtils.addLocalVar("paramArray",
                ArrayType.v(RefType.v("java.lang.Object"), 1),
                body);

        int paramSize = body.getParameterLocals().size();
        jimpleUtils.createAssignStmt(joinPointLocal, joinPointImpl.getName(), units);
        Unit specialInit = jimpleUtils.specialCallStatement(joinPointLocal,
                joinPointImpl.getMethodByName("<init>").toString());
        units.add(specialInit);
        jimpleUtils.createAssignStmt(paramArray,
                jimpleUtils.createNewArrayExpr("java.lang.Object", paramSize), units);
        for (int i = 0; i < paramSize; i++) {
            jimpleUtils.createAssignStmt(jimpleUtils.createArrayRef(paramArray, i), body.getParameterLocal(i), units);
        }
        units.add(jimpleUtils.virtualCallStatement(joinPointLocal,
                joinPointImpl.getMethodByName("setArgs_synthetic").toString(), Collections.singletonList(paramArray)));
    }

    /**
     * Simulate the initialization of the entity class in the program
     *
     * @param body      the method body which contains to entity class initialized
     * @param units     the method body which contains to entity class initialized
     * @param sootClass controller class
     * @param toCall    controller method
     * @return Local variables of entity classes
     */
    @Override
    public Local mockBean(JimpleBody body, PatchingChain<Unit> units, SootClass sootClass, SootMethod toCall) {
        Local paramRef = jimpleUtils.addLocalVar(toCall.getName() + sootClass.getShortName(), sootClass.getName(), body);
        jimpleUtils.createAssignStmt(paramRef, sootClass.getName(), units);
        String initStr = "";
        try {
            initStr = sootClass.getMethod("void <init>()").toString();
        } catch (RuntimeException runtimeException) {
            for (SootMethod method : sootClass.getMethods()) {
                if (method.getName().equals("<init>")) {
                    initStr = method.toString();
                    break;
                }
            }
        }
        Unit specialInit = jimpleUtils.specialCallStatement(paramRef, initStr);
        units.add(specialInit);
        for (SootMethod beanMethod : sootClass.getMethods()) {
            if (beanMethod.getName().startsWith("set") && beanMethod.getParameterTypes().size() > 0) {
                List<Value> valueList = new ArrayList<>();
                valueList.add(StringConstant.v(""));
                units.add(jimpleUtils.virtualCallStatement(paramRef, beanMethod.toString(), valueList));
            }
        }
        return paramRef;
    }

    @Override
    public Local mockHttpServlet(JimpleBody body, PatchingChain<Unit> units, SootClass sootClass, SootMethod toCall) {
        Local paramRef = jimpleUtils.addLocalVar(toCall.getName() + sootClass.getShortName(), sootClass.getName(), body);
        SootClass HttpServletImpl = gsc.generateHttpServlet(sootClass);
        jimpleUtils.createAssignStmt(paramRef, HttpServletImpl.getName(), units);
        Unit specialInit = jimpleUtils.specialCallStatement(paramRef,
                HttpServletImpl.getMethod("void <init>()").toString());
        units.add(specialInit);
        return paramRef;
    }
}
