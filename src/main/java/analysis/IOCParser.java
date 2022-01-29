package analysis;

import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import utils.JimpleUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IOCParser {
    private final JimpleUtils jimpleUtils = new JimpleUtils();
    private final Map<SootClass, String> initMap = new HashMap<>();
    private final Map<SootClass, SootClass> potentialImpl = new HashMap<>();

    /**
     * classname All objects injected through annotations such as Autowired, and find their specific implementation classes,
     * then call the initIOCObject method to initialize these objects by new.
     *
     * @param sootClass  The name of the class to be detected, such as the full class name of ModelOneController
     */
    public String getIOCObject(SootClass sootClass, Set<SootMethod> allBeans) {
        AnnotationAnalysis annotationAnalysis = new AnnotationAnalysis();
        Hierarchy hierarchy = new Hierarchy();
        SootFieldRef sootFieldRef;
        String cName = null;
        String realType;
        String initStr = null;
        SootMethod initMethod = null;
        boolean ambiguous = false;
        try {
            initMethod = sootClass.getMethodByName("<init>");
        } catch (AmbiguousMethodException amx) {
            ambiguous = true;
        }
        for (SootMethod method : sootClass.getMethods()) {
            List<Type> paramOfAutoWiredMethod = annotationAnalysis.getParamOfAutoWiredMethod(method);
            if (paramOfAutoWiredMethod != null) {
                AnnotationAnalysis.autoMethodParams.addAll(paramOfAutoWiredMethod);
            }
            if (ambiguous && method.getName().equals("<init>")) {
                initMethod = method;
                break;
            }
        }

        for (SootField classField : sootClass.getFields()) {
            SootField field = annotationAnalysis.getFieldWithSpecialAnnos(classField, initMethod, ambiguous);
            if (field != null) {
                sootFieldRef = field.makeRef();
                cName = field.getDeclaringClass().getName();
                String vtype = field.getType().toString();
                SootClass aClass = CreateEdge.interfaceToBeans.getOrDefault(((RefType) field.getType()).getSootClass().getName(), null);
                if (aClass != null && !aClass.isInterface()) {
                    // Generate dynamic proxy
                    SootClass beanClass = AOPParser.proxyMap.getOrDefault(aClass.getName(), aClass);
                    // The type of member variable declaration has an implementation class in the Application program
                    realType = beanClass.getType().toString();
                    initStr = mapInitMethod(beanClass, initMap);
                    assert initMethod != null;
                    if (beanClass != aClass || CreateEdge.prototypeComponents.contains(aClass)) {
                        initIOCObjectByPrototype(sootFieldRef, initMethod, vtype, realType, initStr);
                        continue;
                    } else {
                        initIOCObjectBySingleton(sootFieldRef, initMethod, vtype, aClass);
                        continue;
                    }
                } else {
                    assert initMethod != null;
                    SootClass fieldClass = ((RefType) field.getType()).getSootClass();
                    if (fieldClass.isPhantom()) {
                        System.out.println("can't find this bean: " + fieldClass.getName() + " in " + sootClass);
                        continue;
                    }
                    if (filterBaseClass(fieldClass)) {
                        continue;
                    }
                    if (!fieldClass.isInterface() && !fieldClass.isAbstract()) {
                        initStr = mapInitMethod(fieldClass, initMap);
                        initIOCObjectByPrototype(sootFieldRef, initMethod, vtype, fieldClass.getType().toString(), initStr);
                        continue;
                    }
                    SootClass hierarchyClass = null;
                    if (potentialImpl.containsKey(fieldClass)) {
                        hierarchyClass = potentialImpl.get(fieldClass);
                    } else {
                        List<SootClass> hierarchyClasses = fieldClass.isInterface() ? hierarchy.getImplementersOf(fieldClass) : hierarchy.getSubclassesOf(fieldClass);
                        if (hierarchyClasses.size() > 0) {
                            hierarchyClass = hierarchyClasses.get(0);
                            potentialImpl.put(fieldClass, hierarchyClass);
                        }
                    }
                    if (hierarchyClass != null) {
                        initStr = mapInitMethod(hierarchyClass, initMap);
                        initIOCObjectByPrototype(sootFieldRef, initMethod, vtype, hierarchyClass.getType().toString(), initStr);
                        continue;
                    } else {
                        System.out.println("can't find this bean: " + fieldClass.getName() + " in " + sootClass);
                    }
                }

                // Dealing with methods annotated with @Bean
                for (SootMethod bean : allBeans) {
                    PatchingChain<Unit> units = bean.retrieveActiveBody().getUnits();
                    RefType returnType = null;
                    SootClass returnClass = null;
                    if (!(bean.getReturnType() instanceof VoidType) && (field.getType().equals(bean.getReturnType())
                            || ((RefType) bean.getReturnType()).getSootClass().getInterfaces()
                            .contains(((RefType) bean.getReturnType()).getSootClass()))) {
                        for (Unit unit : units) {
                            if (unit instanceof JReturnStmt) {
                                // Find the type of object actually returned by return, and use it as a member variable of the user to initialize the object
                                returnType = (RefType) ((JReturnStmt) unit).getOpBox().getValue().getType();
                                returnClass = returnType.getSootClass();
                                break;
                            }
                        }
                        assert initMethod != null;
                        if (returnClass == null
                                || returnClass.isInterface() || returnClass.isAbstract()
                                || returnClass.getMethodUnsafe("void <init>()") == null) {
                            initStr = bean.getDeclaringClass().getMethodByNameUnsafe("<init>").toString();
                            String callStr = bean.toString();
                            initIOCObjectByPrototype(sootFieldRef, initMethod, vtype, bean.getDeclaringClass().toString(), initStr, callStr);
                            break;
                        }
                        initStr = returnClass.getMethodUnsafe("void <init>()").toString();
                        realType = returnType.toString();
                        initIOCObjectByPrototype(sootFieldRef, initMethod, vtype, realType, initStr);
                        break;
                    }
                }
            }
        }
        return cName;
    }

    private boolean filterBaseClass(SootClass sc) {
        switch (sc.getName()) {
            // case "java.lang.Integer":
            // case "java.lang.Long":
            // case "java.lang.Float":
            // case "java.lang.Double":
            // case "java.lang.Boolean":
            // case "java.lang.Byte":
            case "java.lang.String":
                return true;
            default:
                return false;
        }
    }

    private String mapInitMethod(SootClass sootClass, Map<SootClass, String> initMap) {
        String initStr = null;
        if (initMap.containsKey(sootClass)) {
            initStr = initMap.get(sootClass);
        } else {
            try {
                initStr = sootClass.getMethod("void <init>()").toString();
            } catch (RuntimeException runtimeException) {
                try {
                    initStr = sootClass.getMethodByName("<init>").toString();
                } catch (AmbiguousMethodException ame) {
                    for (SootMethod method : sootClass.getMethods()) {
                        if (method.getName().equals("<init>")) {
                            initStr = method.toString();
                            break;
                        }
                    }
                }
            }
            initMap.put(sootClass, initStr);
        }
        return initStr;
    }

    private void initIOCObjectByPrototype(SootFieldRef sootFieldRef, SootMethod initMethod, String vtype, String declType, String initStr, String callStr) {
        Local tmpRef = jimpleUtils.newLocalVar(vtype.substring(vtype.lastIndexOf(".") + 1).toLowerCase(), RefType.v(vtype));
        Local declRef = jimpleUtils.newLocalVar(declType.substring(declType.lastIndexOf(".") + 1).toLowerCase(), RefType.v(declType));
        JimpleBody body = (JimpleBody) initMethod.retrieveActiveBody();
        Local thisRef = body.getThisLocal();
        if (!body.getLocals().contains(tmpRef)) {
            body.getLocals().add(tmpRef);
        }

        if (!body.getLocals().contains(declRef)) {
            body.getLocals().add(declRef);
        }
        PatchingChain<Unit> units = body.getUnits();
        units.removeIf(unit -> unit instanceof JReturnVoidStmt);

        jimpleUtils.createAssignStmt(declRef, jimpleUtils.createNewExpr(declType), units);
        units.add(jimpleUtils.specialCallStatement(declRef, initStr));
        SootMethod toCall2 = Scene.v().getMethod(callStr);
        jimpleUtils.createAssignStmt(tmpRef, jimpleUtils.createVirtualInvokeExpr(declRef, toCall2), units);
        if (!sootFieldRef.isStatic()) {
            jimpleUtils.createAssignStmt(jimpleUtils.createInstanceFieldRef(thisRef, sootFieldRef), tmpRef, units);
        }
        jimpleUtils.addVoidReturnStmt(units);
    }

    /**
     * It is used to use new in the <init> method of the detected class to instantiate those objects constructed by injection (corresponding to the prototype mode in Spring).
     *
     * @param sootFieldRef Form <com.demo.modelcontroller.ModelFourController: com.demo.service.ModelFourService modelFourService> the information of the constructed object
     * @param vtype        The declared type, that is, the full class name of the interface
     * @param realType     The real type, which is the full type name of the interface implementation class
     * @param initStr      The init method of the real type. To instantiate this object, you need to call its init method
     */
    public void initIOCObjectByPrototype(SootFieldRef sootFieldRef, SootMethod initMethod, String vtype, String realType, String initStr) {
        JimpleBody body = (JimpleBody) initMethod.retrieveActiveBody();
        Local thisRef = body.getThisLocal();
        Local tmpRef = jimpleUtils.addLocalVar(vtype.substring(vtype.lastIndexOf(".") + 1).toLowerCase(),
                vtype,
                body);
        PatchingChain<Unit> units = body.getUnits();
        units.removeIf(unit -> unit instanceof JReturnVoidStmt);
        jimpleUtils.createAssignStmt(tmpRef, jimpleUtils.createNewExpr(realType), units);

        units.add(jimpleUtils.specialCallStatement(tmpRef, initStr));

        if (!sootFieldRef.isStatic()) {
            jimpleUtils.createAssignStmt(jimpleUtils.createInstanceFieldRef(thisRef, sootFieldRef), tmpRef, units);
            jimpleUtils.addVoidReturnStmt(units);
        }

    }

    /**
     * Used in the <init> method of the detected class, using the global singleton factory to construct objects (singleton mode) by injection.
     *
     * @param sootFieldRef Form <com.demo.modelcontroller.ModelFourController: com.demo.service.ModelFourService modelFourService> the information of the constructed object
     * @param vtype        The declared type, that is, the full class name of the interface
     */
    public void initIOCObjectBySingleton(SootFieldRef sootFieldRef, SootMethod initMethod, String vtype, SootClass fieldClass) {
        JimpleBody body = (JimpleBody) initMethod.retrieveActiveBody();
        Local thisRef = body.getThisLocal();
        Local tmpRef = jimpleUtils.addLocalVar(vtype.substring(vtype.lastIndexOf(".") + 1).toLowerCase(),
                vtype,
                body);
        PatchingChain<Unit> units = body.getUnits();
        units.removeIf(unit -> unit instanceof JReturnVoidStmt);

        SootClass singletonFactory = Scene.v().getSootClass("synthetic.method.SingletonFactory");
        Value returnValue = jimpleUtils.createStaticInvokeExpr(singletonFactory.getMethod(fieldClass.getName() + " get" + fieldClass.getShortName() + "()"));
        jimpleUtils.createAssignStmt(tmpRef, returnValue, units);
        try {
            jimpleUtils.createAssignStmt(jimpleUtils.createInstanceFieldRef(thisRef, sootFieldRef), tmpRef, units);
        } catch (Exception e) {
        }
        jimpleUtils.addVoidReturnStmt(units);
    }
}
