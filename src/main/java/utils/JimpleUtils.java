package utils;

import bean.ConstructorArgBean;
import mock.MockObject;
import mock.MockObjectImpl;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JimpleLocal;

import java.util.*;

/**
 * @ClassName JimpleUtils
 * @Description Encapsulate the original jimple statement according to
 * the function for convenient development
 */
public class JimpleUtils extends BaseJimpleUtils {

    public Map<Type, Type> getImplType() {
        Map<Type, Type> map = new HashMap<>();
        map.put(RefType.v("java.util.List"), RefType.v("java.util.ArrayList"));
        map.put(RefType.v("java.util.Map"), RefType.v("java.util.HashMap"));
        map.put(RefType.v("java.util.Set"), RefType.v("java.util.HashSet"));
        return map;
    }

    /**
     * Initialize a default constructor
     *
     * @param customImplClass the class of need to add the constructor
     * @return a default constructor
     */
    public SootMethod genDefaultConstructor(SootClass customImplClass) {
        return genDefaultConstructor(customImplClass, null, false);
    }

    /**
     * Initialize a default constructor
     *
     * @param customImplClass the class of need to add the constructor
     * @param field           the field of need to init
     * @return a default constructor
     */
    public SootMethod genDefaultConstructor(SootClass customImplClass, SootField field, boolean singleton) {
        SootMethod initMethod = new SootMethod("<init>",
                null,
                VoidType.v(), Modifier.PUBLIC);
        SootMethod signature = null;
        try {
            signature = customImplClass.getSuperclass().getMethod("void <init>()");
        } catch (Exception e) {
            for (SootMethod method : customImplClass.getSuperclass().getMethods()) {
                if (method.getName().contains("<init>")) {
                    signature = method;
                    break;
                }
            }
        }

        JimpleBody initBody = createInitJimpleBody(initMethod, signature, customImplClass.getName(), field, singleton);
        initMethod.setActiveBody(initBody);
        return initMethod;
    }

    /**
     * Initialize a default <clinit> method
     *
     * @param customImplClass the class of need to add the <clinit> method
     * @param fields          the field of need to init
     * @param collect
     * @return <clinit> method
     */
    public SootMethod genDefaultClinit(SootClass customImplClass, Set<SootField> fields, Map<String, List<ConstructorArgBean>> collect) {
        SootMethod initMethod = new SootMethod("<clinit>",
                null,
                VoidType.v(), Modifier.STATIC);
        JimpleBody initBody = createClinitJimpleBody(initMethod, customImplClass.getName(), fields, collect);
        initMethod.setActiveBody(initBody);
        return initMethod;
    }

    /**
     * Implementation of static methods
     *
     * @param customImplClass the class of need to add the method
     * @param methodName      method name
     * @param parameterTypes  parameter types
     * @param returnType      return type
     * @param field           field
     * @return static methods
     */
    public SootMethod genStaticCustomMethod(SootClass customImplClass, String methodName, List<Type> parameterTypes, Type returnType, SootField field) {
        SootMethod implMethod = new SootMethod(methodName,
                parameterTypes,
                returnType,
                Modifier.PUBLIC + Modifier.STATIC);
        JimpleBody body = createNewJimpleStaticBody(implMethod, customImplClass.getName(), field);
        implMethod.setActiveBody(body);
        return implMethod;
    }


    /**
     * Implementation of interface methods
     *
     * @param customImplClass the class of need to add the method
     * @param methodName      method name
     * @param parameterTypes  parameter types
     * @param returnType      return type
     * @return methods
     */
    public SootMethod genCustomMethod(SootClass customImplClass, String methodName, List<Type> parameterTypes, Type returnType) {
        SootMethod implMethod = new SootMethod(methodName,
                parameterTypes,
                returnType,
                Modifier.PUBLIC);
        JimpleBody body = createNewJimpleBody(implMethod, new ArrayList<>(), customImplClass.getName());
        implMethod.setActiveBody(body);
        return implMethod;
    }

    /**
     * Construct <init> method
     *
     * @param method    method name
     * @param signature functions that need to be actively called in the sink point
     * @param cName     class name of the field instance
     * @param field     field that need to be initialized
     * @param singleton  the field that needs to be initialized is singleton
     * @return method body
     */
    public JimpleBody createInitJimpleBody(SootMethod method, SootMethod signature, String cName, SootField field, boolean singleton) {

        JimpleBody body = newMethodBody(method);
        Local thisRef = addLocalVar("this", cName, body);

        PatchingChain<Unit> units = body.getUnits();
        createIdentityStmt(thisRef, createThisRef(cName), units);

        SootMethod toCall = Scene.v().getMethod(signature.getSignature());
        units.add(specialCallStatement(thisRef, toCall.toString(), Collections.emptyList()));

        if (field != null) {
            String vtype = field.getType().toString();
            Local tmpRef = addLocalVar(vtype.substring(vtype.lastIndexOf(".") + 1).toLowerCase(), vtype, body);
            if (!singleton) {
                createAssignStmt(tmpRef, createNewExpr(field.getType().toString()), units);
                units.add(specialCallStatement(tmpRef, Scene.v().getSootClass(field.getType().toString()).getMethodByName("<init>").toString()));
            } else {
                SootClass singletonFactory = Scene.v().getSootClass("synthetic.method.SingletonFactory");
                Value returnValue = createStaticInvokeExpr(singletonFactory.getMethod(((RefType) field.getType()).getSootClass().getName()
                        + " get" + ((RefType) field.getType()).getSootClass().getShortName() + "()"));
                createAssignStmt(tmpRef, returnValue, units);
            }
            createAssignStmt(createInstanceFieldRef(thisRef, field.makeRef()), tmpRef, units);
        }

        addVoidReturnStmt(units);
        return body;
    }

    /**
     * Construct <clinit> method
     *
     * @param method  method name
     * @param cName   class name of the field instance
     * @param collect
     * @return method body
     */
    public JimpleBody createClinitJimpleBody(SootMethod method, String cName, Set<SootField> fields, Map<String, List<ConstructorArgBean>> collect) {
        boolean hasargFlag = false;
        if (collect != null && collect.size() > 0) {
            hasargFlag = true;
        }
        JimpleBody body = newMethodBody(method);
        PatchingChain<Unit> units = body.getUnits();
        for (SootField field : fields) {
            String vtype = field.getType().toString();
            Local tmpRef = addLocalVar(vtype.substring(vtype.lastIndexOf(".") + 1).toLowerCase(), vtype, body);
            createAssignStmt(tmpRef, createNewExpr(field.getType().toString()), units);
            if (hasargFlag && collect.containsKey(vtype)) {
                SootClass argClazz = Scene.v().getSootClass(vtype);
                List<ConstructorArgBean> constructorArgBeans = collect.get(vtype);
                for (SootMethod argClazzMethod : argClazz.getMethods()) {
                    if (argClazzMethod.getName().contains("<init>") && argClazzMethod.getParameterCount() == constructorArgBeans.size()) {
                        UnitPatchingChain argmethodunit = argClazzMethod.retrieveActiveBody().getUnits();
                        List<Unit> argunitlist = new LinkedList<>(argmethodunit);
                        LinkedHashMap<String, String> parammap = new LinkedHashMap<>();
                        for (int i = 1; i <= argClazzMethod.getParameterCount(); i++) {
                            if (argunitlist.get(i) instanceof JIdentityStmt) {
                                JIdentityStmt stmt = (JIdentityStmt) argunitlist.get(i);
                                JimpleLocal leftval = (JimpleLocal) stmt.leftBox.getValue();
                                String key = leftval.getName();
                                ParameterRef rightref = (ParameterRef) stmt.rightBox.getValue();
                                String val = rightref.getType().toString();
                                parammap.put(key, val);
                            }
                        }
                        int index = 0;
                        boolean flag = true;
                        List<Value> parmas = new ArrayList<>();
                        while (index < constructorArgBeans.size()) {
                            ConstructorArgBean argBean = constructorArgBeans.get(index);
                            if (argBean.getArgType() != null) {
                                Type parameterType = argClazzMethod.getParameterType(index);
                                if (!parameterType.toString().equals(argBean.getArgType())) {
                                    flag = false;
                                    break;
                                } else {
                                    parmas.add(getConstant(argBean.getArgType(), argBean.getArgValue(), false));
                                }
                            } else if (argBean.getArgName() != null) {
                                if (!parammap.containsKey(argBean.getArgName())) {
                                    flag = false;
                                    break;
                                } else {
                                    parmas.add(getConstant(parammap.get(argBean.getArgName()), argBean.getArgValue(), false));
                                }
                            } else if (argBean.getRefType() != null) {
                                if (!parammap.containsKey(argBean.getRefType())) {
                                    flag = false;
                                    break;
                                } else {
                                    parmas.add(getConstant("", "", true));
                                }
                            }
                            index++;
                        }
                        if (flag) {
                            units.add(specialCallStatement(tmpRef, argClazzMethod.toString(), parmas));
                            break;
                        }
                    }
                }
            } else {
                for (SootMethod sootMethod : Scene.v().getSootClass(field.getType().toString()).getMethods()) {
                    if (sootMethod.getName().contains("<init>")) {
                        units.add(specialCallStatement(tmpRef, sootMethod.toString()));
                        break;
                    }
                }
            }
            createAssignStmt(createStaticFieldRef(field.makeRef()), tmpRef, units);
        }
        addVoidReturnStmt(units);
        return body;
    }

    private Constant getConstant(String typesign, String value, boolean isclazz) {
        String s = typesign.toLowerCase();
        if (isclazz) {
            return NullConstant.v();
        }
        if (s.contains("string")) {
            return StringConstant.v(value);
        } else if (s.contains("int")) {
            return IntConstant.v(Integer.parseInt(value));
        } else if (s.contains("double")) {
            return DoubleConstant.v(Double.parseDouble(value));
        } else if (s.contains("float")) {
            return FloatConstant.v(Float.parseFloat(value));
        } else if (s.contains("long")) {
            return LongConstant.v(Long.parseLong(value));
        }
        return null;
    }

    /**
     * create new method
     *
     * @param method     method name
     * @param signatures functions that need to be actively called in the sink point
     * @param cName      class name of the field instance
     * @return method body
     */
    public JimpleBody createNewJimpleBody(SootMethod method, List<SootMethod> signatures, String cName) {
        List<Value> parameterValues = new ArrayList<>();
        JimpleBody body = newMethodBody(method);
        Local thisRef = addLocalVar("this", cName, body);

        PatchingChain<Unit> units = body.getUnits();
        createIdentityStmt(thisRef, createThisRef(cName), units);
        // initialize the type parameters of the method
        for (int i = 0; i < method.getParameterCount(); i++) {
            Type parameterType = method.getParameterType(i);
            Local param = addLocalVar("param" + i, parameterType, body);
            createIdentityStmt(param, createParamRef(parameterType, i), units);
            parameterValues.add(param);
        }

        if (method.getName().equals("setArgs_synthetic")) {
            SootField sootField = Scene.v().getSootClass(cName).getFieldByName("args");
            Local param = body.getParameterLocal(0);
            createAssignStmt(createInstanceFieldRef(thisRef, sootField.makeRef()), param, units);
        }

        // call all methods in the signatures list
        for (int i = 0; i < signatures.size(); i++) {
            SootMethod toCall = Scene.v().getMethod(signatures.get(i).toString());
            int parameterCount = toCall.getParameterCount();
            List<Value> paramList = new ArrayList<>();
            for (int j = 0; j < parameterCount - parameterValues.size(); j++) {
                paramList.add(NullConstant.v());
            }
            paramList.addAll(parameterValues);

            String declaringClassName = signatures.get(i).getDeclaringClass().getName();
            if (!declaringClassName.equals(cName)
                    && !declaringClassName.equals("java.lang.Object")
                    && signatures.get(i).getReturnType() != null) {
                Local virtualRef = addLocalVar("virtual" + i, declaringClassName, body);
                createAssignStmt(virtualRef, declaringClassName, units);
                units.add(specialCallStatement(virtualRef,
                        signatures.get(i).getDeclaringClass().getMethod("void <init>()").toString()));
            } else {
                if (signatures.get(i).getReturnType() instanceof VoidType || method.getName().equals("callEntry_synthetic")) {
                    units.add(specialCallStatement(thisRef, toCall.toString(), paramList));
                }
            }
        }

        Type returnType = method.getReturnType();
        if (returnType instanceof RefType) {
            addCommonReturnStmt(NullConstant.v(), units);
        } else if (returnType instanceof VoidType) {
            addVoidReturnStmt(units);
        } else if (returnType instanceof IntType) {
            addCommonReturnStmt(IntConstant.v(0), units);
        } else if (returnType instanceof LongType) {
            addCommonReturnStmt(LongConstant.v(0), units);
        } else if (returnType instanceof DoubleType) {
            addCommonReturnStmt(DoubleConstant.v(0), units);
        } else if (returnType instanceof ArrayType) {
            if (Scene.v().getSootClass(cName).getFields().size() > 0) {
                SootField sootField = Scene.v().getSootClass(cName).getFieldByName("args");
                if (sootField != null && sootField.getType().equals(returnType) && method.getName().contains("getArgs")) {
                    Local returnRef = addLocalVar("returnRef", returnType, body);
                    createAssignStmt(returnRef, createInstanceFieldRef(thisRef, sootField.makeRef()), units);
                    addCommonReturnStmt(returnRef, units);
                }
            } else {
                addCommonReturnStmt(NullConstant.v(), units);
            }
        } else if (returnType instanceof BooleanType) {
            addCommonReturnStmt(IntConstant.v(0) , units);
        }
        return body;
    }

    /**
     * create static method
     *
     * @param method method name
     * @param cName  class name of the field instance
     * @return method body
     */
    public JimpleBody createNewJimpleStaticBody(SootMethod method, String cName, SootField field) {
        JimpleBody body = newMethodBody(method);
        PatchingChain<Unit> units = body.getUnits();
        Local localRef = null;
        if (method.getName().equals("get" + field.getName())) {
            localRef = addLocalVar(field.getName().toLowerCase(), field.getType(), body);
            createAssignStmt(localRef, createStaticFieldRef(field.makeRef()), units);
        }

        Type returnType = method.getReturnType();
        if (returnType instanceof RefType) {
            if (localRef != null) {
                addCommonReturnStmt(localRef, units);
            } else {
                addCommonReturnStmt(NullConstant.v(), units);
            }
        }
        return body;
    }

    /**
     * create new method
     *
     * @param method     method name
     * @param signatures functions that need to be actively called in the sink point
     * @param cName      class name of the field instance
     * @return method body
     */
    public JimpleBody createJimpleBody(SootMethod method, List<SootMethod> signatures, String cName) {
        Map<String, Local> hasMethods = new HashMap<>();
        MockObject mockObject = new MockObjectImpl();
        Value virtualCallWithReturn = null;
        JimpleBody body = newMethodBody(method);
        Local thisRef = addLocalVar("this", cName, body);

        PatchingChain<Unit> units = body.getUnits();
        createIdentityStmt(thisRef, createThisRef(cName), units);

        for (int i = 0; i < signatures.size(); i++) {
            SootMethod toCall = Scene.v().getMethod(signatures.get(i).toString());
            List<Local> paramsLocal = toCall.retrieveActiveBody().getParameterLocals();
            int parameterCount = toCall.getParameterCount();
            List<Value> paramList = new ArrayList<>();
            for (int j = 0; j < parameterCount; j++) {
                if (isBaseTypes(toCall.getParameterType(j))) {
                    paramList.add(NullConstant.v());
                    continue;
                }
                SootClass sootClass = Scene.v().getSootClass(toCall.getParameterType(j).toString());
                if (!sootClass.isInterface() && !sootClass.isJavaLibraryClass()
                        && !sootClass.isAbstract() && sootClass.getMethods().size() > 0) {
                    try {
                        Local paramRef = mockObject.mockBean(body, units, sootClass, toCall);
                        paramList.add(paramRef);
                    } catch (Exception e) {
                        e.printStackTrace();
                        paramList.add(NullConstant.v());
                    }
                    continue;
                } else if (sootClass.getName().contains("HttpServlet")) {
                    Local paramRef = mockObject.mockHttpServlet(body, units, sootClass, toCall);
                    paramList.add(paramRef);
                    continue;
                } else if (sootClass.getName().equals("java.lang.String")) {
                    String methodName = "get" + paramsLocal.get(j).getName();
                    if (hasMethods.containsKey(methodName)) {
                        paramList.add(hasMethods.get(methodName));
                    } else {
                        Local paramRef = addLocalVar(methodName, sootClass.getName(), body);
                        SootMethod stringMethod = genCustomMethod(method.getDeclaringClass(), methodName, new ArrayList<>(), sootClass.getType());
                        method.getDeclaringClass().addMethod(stringMethod);
                        Value invokeStringMethod = createVirtualInvokeExpr(thisRef, stringMethod);
                        units.add(createAssignStmt(paramRef, invokeStringMethod));
                        hasMethods.put(methodName, paramRef);
                        paramList.add(paramRef);
                    }
                    continue;
                }
                paramList.add(NullConstant.v());
            }

            String declaringClassName = signatures.get(i).getDeclaringClass().getName();
            if (!declaringClassName.equals(cName)
                    && !declaringClassName.equals("java.lang.Object")
                    && signatures.get(i).getReturnType() != null) {
                Local virtualRef = addLocalVar("virtual" + i, declaringClassName, body);
                createAssignStmt(virtualRef, declaringClassName, units);
                units.add(specialCallStatement(virtualRef,
                        signatures.get(i).getDeclaringClass().getMethod("void <init>()").toString()));
                virtualCallWithReturn = createVirtualInvokeExpr(virtualRef, toCall, paramList);
            } else {
                if (!(signatures.get(i).getReturnType() instanceof VoidType) && !(method.getName().equals("callEntry_synthetic"))) {
                    virtualCallWithReturn = createSpecialInvokeExpr(thisRef, toCall, paramList);
                } else {
                    units.add(specialCallStatement(thisRef, toCall.toString(), paramList));
                }
            }
        }

        Type returnType = method.getReturnType();
        if (returnType instanceof RefType) {
            Local returnRef = addLocalVar("returnRef", returnType, body);
            if (((RefType) returnType).getSootClass().isInterface()) {
                returnType = getImplType().get(returnType);
            }
            if (virtualCallWithReturn != null) {
                createAssignStmt(returnRef, virtualCallWithReturn, units);
            } else {
                createAssignStmt(returnRef, createNewExpr((RefType) returnType), units);
                units.add(specialCallStatement(returnRef,
                        ((RefType) returnType).getSootClass().getMethod("void <init>()")));
            }
            addCommonReturnStmt(returnRef, units);
        } else if (returnType instanceof VoidType) {
            addVoidReturnStmt(units);
        } else if (returnType instanceof IntType) {
            addCommonReturnStmt(LongConstant.v(0), units);
        } else if (returnType instanceof LongType) {
            addCommonReturnStmt(LongConstant.v(0), units);
        } else if (returnType instanceof DoubleType) {
            addCommonReturnStmt(DoubleConstant.v(0), units);
        }
        return body;
    }

    private boolean isBaseTypes(Type target) {
        return target instanceof LongType
                || target instanceof IntType
                || target instanceof DoubleType
                || target instanceof FloatType
                || target instanceof ArrayType
                || target instanceof BooleanType;
    }
}
