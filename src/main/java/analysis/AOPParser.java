package analysis;

import bean.AOPTargetModel;
import bean.AspectModel;
import bean.InsertMethod;
import enums.AdviceEnum;
import mock.GenerateSyntheticClass;
import mock.GenerateSyntheticClassImpl;
import org.aspectj.weaver.UnresolvedType;
import org.aspectj.weaver.patterns.*;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.NullConstant;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.tagkit.AnnotationElem;
import soot.tagkit.AnnotationIntElem;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.Chain;
import utils.JimpleUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**notification
 * @ClassName AOPParser
 * @Description Analysis of AOP function
 */
public class AOPParser {
    public static Map<String, AOPTargetModel> modelMap = new HashMap<>();
    public static Map<String, SootClass> proxyMap = new HashMap<>();

    public static Map<String, HashSet<String>> TargetToAdv = new HashMap<>();
    private final JimpleUtils jimpleUtils = new JimpleUtils();
    private GenerateSyntheticClass gsc = new GenerateSyntheticClassImpl();

    /**
     * Get all the aspect classes annotated with @Aspect
     *
     * @param sootClasses Collection of all Application classes in Soot
     * @return List of aspect
     */
    public List<AspectModel> getAllAspects(Set<SootClass> sootClasses) {
        List<AspectModel> allAspects = new LinkedList<>();
        for (SootClass sootClass : sootClasses) {
            SootClass aspectClass = null;
            int order = Integer.MAX_VALUE;
            VisibilityAnnotationTag annotationTags = (VisibilityAnnotationTag) sootClass.getTag("VisibilityAnnotationTag");
            if (annotationTags != null && annotationTags.getAnnotations() != null) {
                for (AnnotationTag annotation : annotationTags.getAnnotations()) {
                    if (annotation.getType().equals("Lorg/aspectj/lang/annotation/Aspect;")) {
                        aspectClass = sootClass;
                        continue;
                    }
                    if (annotation.getType().equals("Lorg/springframework/core/annotation/Order;")) {
                        for (AnnotationElem elem : annotation.getElems()) {
                            if (elem instanceof AnnotationIntElem) {
                                order = ((AnnotationIntElem) elem).getValue();
                            }
                        }
                    }
                }
            }

            if (aspectClass != null) {
                allAspects.add(new AspectModel(aspectClass, order));
            }
        }
        return allAspects;
    }

    /**
     * Process different pointcut expressions
     *
     * @param expression   pointcut expressions
     * @param aspectMethod aspect method
     */
    public void processDiffAopExp(String expression, SootMethod aspectMethod) {
        PatternParser parser = new PatternParser(expression);
        Pointcut pointcut = parser.parsePointcut();
        AOPParser aopParser = new AOPParser();
        for (SootClass sootClass : CreateEdge.allBeansAndInterfaces) {
            for (SootMethod targetMethod : sootClass.getMethods()) {
                if (aopParser.switchPoint(pointcut, aspectMethod, targetMethod)) {
                    savePointMethod(aspectMethod, sootClass, targetMethod);
                }
            }
        }
    }

    /**
     * Process different pointcut expressions
     *
     * @param pointcut     pointcut expressions
     * @param aspectMethod aspect method
     */
    public boolean switchPoint(Pointcut pointcut, SootMethod aspectMethod, SootMethod targetMethod) {
        boolean matched = false;
        switch (pointcut.getClass().getSimpleName()) {
            case "WithinPointcut":
                WithinPointcut withinPointcut = (WithinPointcut) pointcut;
                matched = withInProcess(withinPointcut, aspectMethod, targetMethod);
                break;
            case "KindedPointcut":
                KindedPointcut execPointcut = (KindedPointcut) pointcut;
                matched = executionProcess(execPointcut, aspectMethod, targetMethod);
                break;
            case "ArgsPointcut":
                ArgsPointcut argsPointcut = (ArgsPointcut) pointcut;
                matched = ArgsProcess(argsPointcut, aspectMethod, targetMethod);
                break;
            case "AndPointcut":
                AndPointcut andPointcut = (AndPointcut) pointcut;
                matched = andProcess(andPointcut, aspectMethod, targetMethod);
                break;
            case "OrPointcut":
                OrPointcut orPointcut = (OrPointcut) pointcut;
                matched = orProcess(orPointcut, aspectMethod, targetMethod);
                break;
            case "AnnotationPointcut":
                AnnotationPointcut annotationPointcut = (AnnotationPointcut) pointcut;
                matched = AnnoProcess(annotationPointcut, aspectMethod, targetMethod);
                break;
            case "WithinAnnotationPointcut":
                WithinAnnotationPointcut withinAnnotationPointcut = (WithinAnnotationPointcut) pointcut;
                matched = withinAnnoProcess(withinAnnotationPointcut, aspectMethod, targetMethod);
                break;
        }
        return matched;
    }

    private boolean withinAnnoProcess(WithinAnnotationPointcut withinAnnotationPointcut, SootMethod aspectMethod, SootMethod targetMethod) {
        AnnotationTypePattern typePattern = withinAnnotationPointcut.getAnnotationTypePattern();
        String str = "";
        if (typePattern instanceof ExactAnnotationTypePattern) {
            ExactAnnotationTypePattern ex = (ExactAnnotationTypePattern) typePattern;
            UnresolvedType annotationType = ex.getAnnotationType();
            str = annotationType.getSignature();
        }
        VisibilityAnnotationTag classAnnotationTag = (VisibilityAnnotationTag) targetMethod.getDeclaringClass().getTag("VisibilityAnnotationTag");
        if (classAnnotationTag != null && classAnnotationTag.getAnnotations() != null) {
            for (AnnotationTag annotation : classAnnotationTag.getAnnotations()) {
                if (annotation.getType().equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Process within expressions
     *
     * @param pointcut     pointcut expressions
     * @param aspectMethod aspect method
     */
    public boolean withInProcess(WithinPointcut pointcut, SootMethod aspectMethod, SootMethod targetMethod) {
        TypePattern typePattern = pointcut.getTypePattern();
        Map<String, Object> dealWithinPointcut = dealWithinPointcut(typePattern);
        Integer type = (Integer) dealWithinPointcut.get("type");
        if (type == 1) {
        } else if (type == 3) {
            NamePattern[] namePatterns1 = (NamePattern[]) dealWithinPointcut.get("pattern");
            if (!clazzIsMatch(namePatterns1, targetMethod.getDeclaringClass().getName())) {
                return false;
            }
        } else if (type == 2) {
        }
        return true;
    }

    /**
     * Process args expressions
     *
     * @param pointcut     pointcut expressions
     * @param aspectMethod aspect method
     */
    public boolean ArgsProcess(ArgsPointcut pointcut, SootMethod aspectMethod, SootMethod targetMethod) {
        TypePattern[] typePatterns = pointcut.getArguments().getTypePatterns();
        return isMethodParamMatches(typePatterns, targetMethod.getParameterTypes());

    }

    /**
     * Process execution expressions
     *
     * @param pointcut     pointcut expressions
     * @param aspectMethod aspect method
     */
    public boolean executionProcess(KindedPointcut pointcut, SootMethod aspectMethod, SootMethod targetMethod) {
        SignaturePattern pattern = pointcut.getSignature();
        String modifier = pattern.getModifiers().toString();
        TypePattern declaringType = pattern.getDeclaringType();
        TypePattern returnType = pattern.getReturnType();
        NamePattern methodName = pattern.getName();
        TypePattern[] typePatterns = pattern.getParameterTypes().getTypePatterns();
        if (declaringType instanceof WildTypePattern) {
            WildTypePattern wildType = (WildTypePattern) declaringType;
            NamePattern[] namePatterns = wildType.getNamePatterns();
            if (!clazzIsMatch(namePatterns, targetMethod.getDeclaringClass().getName())) {
                return false;
            }
        }

        int methodModifier = targetMethod.getModifiers();
        boolean flag;
        switch (modifier) {
            case "public":
                flag = Modifier.isPublic(methodModifier);
                break;
            case "protected":
                flag = Modifier.isProtected(methodModifier);
                break;
            case "private":
                flag = Modifier.isPrivate(methodModifier);
                break;
            default:
                flag = true;
                break;
        }

        if (!flag || !methodName.matches(targetMethod.getName()) || targetMethod.getName().equals("<init>")
                || targetMethod.getName().equals("callEntry_synthetic") || targetMethod.getName().equals("<clinit>")) {
            return false;
        }

        if (returnType instanceof WildTypePattern) {
            WildTypePattern wildType = (WildTypePattern) returnType;
            NamePattern[] namePatterns = wildType.getNamePatterns();
            if (clazzIsMatch(namePatterns, targetMethod.getReturnType().toString())) {
                return false;
            }
        }

        return isMethodParamMatches(typePatterns, targetMethod.getParameterTypes());
    }

    /**
     * Process annotation expressions
     *
     * @param pointcut     pointcut expressions
     * @param aspectMethod aspect method
     */
    public boolean AnnoProcess(AnnotationPointcut pointcut, SootMethod aspectMethod, SootMethod targetMethod) {
        String s = pointcut.getAnnotationTypePattern().getAnnotationType().toString();
        String annot = "";
        for (Local local : aspectMethod.retrieveActiveBody().getLocals()) {
            if (local.getName().equals(s)) {
                String a = local.getType().toString();
                String[] array = a.split("\\.");
                annot = array[array.length-1];
                break;
            }
        }
        s = s.substring(s.lastIndexOf(".") + 1);
        boolean isclazzAnnoed = false;
        VisibilityAnnotationTag classAnnotationTag = (VisibilityAnnotationTag) targetMethod.getDeclaringClass().getTag("VisibilityAnnotationTag");
        if (classAnnotationTag != null && classAnnotationTag.getAnnotations() != null) {
            for (AnnotationTag annotation : classAnnotationTag.getAnnotations()) {
                String c = annotation.getType().substring(annotation.getType().lastIndexOf("/") + 1, annotation.getType().length() - 1);
                if (c.equals(s)) {
                    isclazzAnnoed = true;
                    break;
                }
            }
        }
        if (isclazzAnnoed) {
            return true;
        } else {
            VisibilityAnnotationTag annotationTags = (VisibilityAnnotationTag) targetMethod.getTag("VisibilityAnnotationTag");
            if (annotationTags != null && annotationTags.getAnnotations() != null) {
                for (AnnotationTag annotation : annotationTags.getAnnotations()) {
                    String c = annotation.getType().substring(annotation.getType().lastIndexOf("/") + 1, annotation.getType().length() - 1);
                    if (c.equals(s) || c.equals(annot)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Process or expressions
     *
     * @param pointcut     pointcut expressions
     * @param aspectMethod aspect method
     */
    public boolean orProcess(OrPointcut pointcut, SootMethod aspectMethod, SootMethod targetMethod) {
        Pointcut leftPoint = pointcut.getLeft();
        Pointcut rightPoint = pointcut.getRight();
        return switchPoint(leftPoint, aspectMethod, targetMethod) || switchPoint(rightPoint, aspectMethod, targetMethod);
    }

    public boolean andProcess(AndPointcut pointcut, SootMethod aspectMethod, SootMethod targetMethod) {
        Pointcut leftPoint = pointcut.getLeft();
        Pointcut rightPoint = pointcut.getRight();
        return switchPoint(leftPoint, aspectMethod, targetMethod) && switchPoint(rightPoint, aspectMethod, targetMethod);
    }

    /**
     * Add the matching advice to the list corresponding to the target object
     *
     * @param allAdvices all advices
     */
    public void addAdviceToTarget(List<AspectModel> allAdvices) {
        Map<String, AOPTargetModel> tmp = new HashMap<>();
        for (AspectModel adviceModel : allAdvices) {
            for (Map.Entry<String, AOPTargetModel> stringAOPTargetModelEntry : AOPParser.modelMap.entrySet()) {
                AOPTargetModel aopTargetModel = stringAOPTargetModelEntry.getValue();
                String key = stringAOPTargetModelEntry.getKey();
                SootClass proxyClass;
                if (proxyMap.containsKey(aopTargetModel.getClassName())) {
                    proxyClass = proxyMap.get(aopTargetModel.getClassName());
                } else {
                    if (aopTargetModel.getSootClass().isAbstract()) {
                        return;
                    }
                    proxyClass = gsc.generateProxy(aopTargetModel.getSootClass());
                    proxyMap.put(aopTargetModel.getClassName(), proxyClass);
                }
                SootMethod superMethod = aopTargetModel.getSootMethod();
                if (superMethod.isStatic() || superMethod.isPrivate()
                        || superMethod.isFinal() || superMethod.getName().contains("<init>")
                        || superMethod.getName().contains("<clinit>")) {
                    tmp.put(key, aopTargetModel);
                    continue;
                }
                SootMethod proxyMethod = proxyClass.getMethod(aopTargetModel.getSootMethod().getSubSignature());
                aopTargetModel.setProxyClass(proxyClass);
                aopTargetModel.setProxyClassName(proxyClass.getName());
                aopTargetModel.setProxyMethod(proxyMethod);
                aopTargetModel.setProxyMethodName(proxyMethod.getSignature());
                if (TargetToAdv.containsKey(key) && TargetToAdv.get(key).contains(adviceModel.getSootMethod().getSignature())) {
                    aopTargetModel.addAdvice(adviceModel);
                }
            }
        }
        for (String s : tmp.keySet()) {
            AOPParser.modelMap.remove(s);
        }
    }

    private AOPTargetModel getAopTargetInstance(SootClass sootClass, SootMethod sootMethod) {
        AOPTargetModel atm = new AOPTargetModel();
        atm.setSootClass(sootClass);
        atm.setClassName(sootClass.getName());
        atm.setSootMethod(sootMethod);
        atm.setMethodName(sootMethod.getSignature());
        return atm;
    }

    private void savePointMethod(SootMethod aspectMethod, SootClass sootClass, SootMethod method) {
        if (method.getName().equals("<init>")
                || method.getName().equals("callEntry_synthetic") || method.getName().equals("<clinit>")) {
            return;
        }
        if (sootClass.isInterface()) {
            sootClass = CreateEdge.interfaceToBeans.get(sootClass.getName());
            method = sootClass.getMethodUnsafe(method.getSubSignature());
        }
        if (method == null) {
            return;
        }
        String methodSign = method.getSignature();
        HashSet<String> tmp;
        if (TargetToAdv.containsKey(methodSign)) {
            tmp = TargetToAdv.get(methodSign);
        } else {
            tmp = new HashSet<>();
        }
        tmp.add(aspectMethod.getSignature());
        TargetToAdv.put(methodSign, tmp);

        if (!modelMap.containsKey(methodSign)) {
            AOPTargetModel atm = getAopTargetInstance(sootClass, method);
            modelMap.put(methodSign, atm);
        }
    }

    public boolean clazzIsMatch(NamePattern[] namePatterns, String path) {
        Pattern re1 = Pattern.compile("[a-z|A-Z|_]+[0-9]*");
        Pattern re2 = Pattern.compile("\\*");
        StringBuilder sb = new StringBuilder();
        sb.append("^");
        for (NamePattern namePattern : namePatterns) {
            Matcher m1 = re1.matcher(namePattern.toString());
            Matcher m2 = re2.matcher(namePattern.toString());
            if (m1.find()) {
                sb.append(namePattern.toString());
                sb.append("\\.");
            } else if (m2.find()) {
                sb.append("([a-z|A-Z|_]+[0-9]*)\\.");
            } else if (namePattern.toString().equals("")) {
                sb.append("(((\\D+)(\\w*)\\.)+)?");
            }
        }
        String res = sb.toString();
        if (res.lastIndexOf(".") == res.length() - 1) {
            res = res.substring(0, res.lastIndexOf("\\."));
        }
        res += "$";
        Pattern compile = Pattern.compile(res);
        Matcher matcher = compile.matcher(path);
        return matcher.find();
    }

    public boolean isMethodParamMatches(TypePattern[] typePatterns, List<Type> parameterTypes) {
        boolean ismatch = false;

        if (parameterTypes.size() >= typePatterns.length) {
            if (parameterTypes.size() == 0) {
                ismatch = true;
            } else {
                for (int i = 0; i < typePatterns.length; i++) {
                    String tmptype = typePatterns[i].toString();
                    if (i == (typePatterns.length - 1) && typePatterns.length == parameterTypes.size()) {
                        if ("..".equals(tmptype) || parameterTypes.get(i).toString().contains(tmptype)) {
                            ismatch = true;
                        }
                    }
                    if ("*".equals(tmptype)) {
                        continue;
                    }
                    if ("..".equals(tmptype)) {
                        ismatch = true;
                        break;
                    }
                    if (!parameterTypes.get(i).toString().contains(tmptype)) {
                        ismatch = false;
                        break;
                    }

                }
            }

        } else {
            int i;
            for (i = 0; i < parameterTypes.size(); i++) {
                String tmptype = typePatterns[i].toString();
                if ("*".equals(tmptype)) {
                    continue;
                }
                if ("..".equals(tmptype)) {
                    ismatch = true;
                    break;
                }
                if (!parameterTypes.get(i).toString().contains(tmptype)) {
                    break;
                }
            }
            if (typePatterns.length - i == 1 && "..".equals(typePatterns[typePatterns.length - 1].toString())) {
                ismatch = true;
            }

        }
        return ismatch;

    }

    public static Map<String, Object> dealWithinPointcut(TypePattern typePattern) {
        Map<String, Object> res = new HashMap<>();
        // 1 means with "+" matching subtype
        // 2 means with "@" matching annotation
        // 3 means normal matching class
        if (typePattern.isIncludeSubtypes()) {
            WildTypePattern wildTypePattern = (WildTypePattern) typePattern;
            NamePattern[] namePatterns = wildTypePattern.getNamePatterns();
            res.put("pattern", namePatterns);
            res.put("type", 1);
            return res;
        } else {
            if (typePattern instanceof AnyWithAnnotationTypePattern) {
                AnyWithAnnotationTypePattern awatp = (AnyWithAnnotationTypePattern) typePattern;
                WildAnnotationTypePattern wildAnnotationTypePattern = (WildAnnotationTypePattern) awatp.getAnnotationTypePattern();
                WildTypePattern wildTypePattern = (WildTypePattern) wildAnnotationTypePattern.getTypePattern();
                NamePattern[] namePatterns = wildTypePattern.getNamePatterns();
                res.put("pattern", namePatterns);
                res.put("type", 2);
                return res;
            } else {
                WildTypePattern wildTypePattern = (WildTypePattern) typePattern;
                NamePattern[] namePatterns = wildTypePattern.getNamePatterns();
                res.put("pattern", namePatterns);
                res.put("type", 3);
            }
        }
        return res;
    }

    /**
     * for the analysis of the @Around annotation, a new method is generated after the call statement of the target
     * function is inserted into the related call statement of the Around advice method.
     *
     * @param aspectModel  advice method
     * @return Newly generated method, the actual business method
     */
    protected SootMethod aroundParser(AspectModel aspectModel, SootMethod targetMethod) {
        List<Integer> returnList = new ArrayList<>();
        List<Integer> insertPointList = new ArrayList<>();
        List<Integer> pjpList = new ArrayList<>();
        List<Type> parameterTypes = new ArrayList<>(aspectModel.getSootMethod().getParameterTypes());
        parameterTypes.addAll(targetMethod.getParameterTypes());
        parameterTypes.add(targetMethod.getDeclaringClass().getType());
        SootMethod newAspectMethod = new SootMethod(aspectModel.getSootMethod().getName()
                + "_" + targetMethod.getName(),
                parameterTypes,
                aspectModel.getSootMethod().getReturnType(),
                Modifier.PUBLIC);
        aspectModel.getSootClass().addMethod(newAspectMethod);
        JimpleBody aspectBody = (JimpleBody) aspectModel.getSootMethod().retrieveActiveBody().clone();
        PatchingChain<Unit> aspectUnits = aspectBody.getUnits();
        Unit paramInsertPoint = null;
        for (Unit unit : aspectUnits) {
            if (unit.toString().contains("@parameter")) {
                paramInsertPoint = unit;
            } else if (paramInsertPoint != null) {
                break;
            }
        }

        for (int i = parameterTypes.size() - 1; i > 0; i--) {
            Local param = jimpleUtils.addLocalVar("param" + i, parameterTypes.get(i), aspectBody);
            if (paramInsertPoint != null) {
                aspectUnits.insertAfter(jimpleUtils.createIdentityStmt(param,
                        jimpleUtils.createParamRef(parameterTypes.get(i), i)), paramInsertPoint);
            } else {
                aspectUnits.addFirst(jimpleUtils.createIdentityStmt(param,
                        jimpleUtils.createParamRef(parameterTypes.get(i), i)));
            }
        }

        newAspectMethod.setActiveBody(aspectBody);
        int lineNumber = 0;
        for (Unit unit : aspectUnits) {
            if ((unit instanceof JReturnVoidStmt) || (unit instanceof JReturnStmt)) {
                returnList.add(lineNumber);
                insertPointList.add(lineNumber);
            } else if (unit.toString().contains("<org.aspectj.lang.ProceedingJoinPoint: java.lang.Object proceed()>")
                    || unit.toString().contains("<org.aspectj.lang.ProceedingJoinPoint: java.lang.Object proceed(java.lang.Object[])>")) {
                pjpList.add(lineNumber);
            }
            lineNumber++;
        }
        AOPAnalysis.insertMethodMap.put(newAspectMethod.toString(), new InsertMethod(newAspectMethod, returnList, insertPointList, pjpList));
        return newAspectMethod;
    }

    /**
     * Insert the call statement of the target business code
     *
     * @param currentMethod  Caller method
     * @param calleeMethod  The method that needs to be called
     */
    protected void insertAOPTarget(SootMethod currentMethod, SootMethod calleeMethod, AdviceEnum currentEnum) {
        int modifyLineNumber = 0;
        JimpleBody body = (JimpleBody) currentMethod.retrieveActiveBody();
        PatchingChain<Unit> units = body.getUnits();
        Local localModel = null;
        if (currentMethod.getDeclaringClass().getSuperclass().equals(calleeMethod.getDeclaringClass())) {
            for (Local local : body.getLocals()) {
                if (local.getName().equals("localTarget")) {
                    localModel = local;
                    break;
                }
            }
        } else {
            localModel = body.getParameterLocal(body.getParameterLocals().size() - 1);
        }

        int paramCount = currentMethod.getParameterCount() - calleeMethod.getParameterCount() - 1;
        List<Value> paramList = new ArrayList<>(body.getParameterLocals());
        if (currentEnum != null && currentEnum.name().equals("AOP_AROUND")) {
            if (currentMethod.getParameterCount() != 0)
                paramList.remove(currentMethod.getParameterCount() - 1);
            for (int i = 0; i < paramCount; i++) {
                paramList.remove(i);
            }
        }

        InsertMethod im = AOPAnalysis.insertMethodMap.get(currentMethod.toString());
        List<Integer> returnList = im.getReturnList();
        List<Integer> insertPointList = im.getPjpList() == null ? im.getInsertPointList() : im.getPjpList();
        List<Unit> unitList = new LinkedList<>(units);
        Local returnRef = null;
        for (int i = 0; i < insertPointList.size(); i++) {
            if (!(currentMethod.getReturnType() instanceof VoidType) && !(calleeMethod.getReturnType() instanceof VoidType)) {
                if (returnRef == null) {
                    String returnRefName = unitList.get(returnList.get(i) + modifyLineNumber).toString().replace("return ", "");
                    for (Local local : body.getLocals()) {
                        if (local.getName().equals(returnRefName)) {
                            returnRef = local;
                            break;
                        }
                    }
                    if (returnRef == null) {
                        returnRef = jimpleUtils.newLocalVar(returnRefName, RefType.v(currentMethod.getReturnType().toString()));
                    }
                }


                Value returnValue = jimpleUtils.createVirtualInvokeExpr(localModel, calleeMethod, paramList);
                if (im.getPjpList() != null) {
                    units.insertAfter(jimpleUtils.createAssignStmt(returnRef, returnValue), unitList.get(insertPointList.get(i) + modifyLineNumber));
                } else {
                    units.insertBefore(jimpleUtils.createAssignStmt(returnRef, returnValue), unitList.get(insertPointList.get(i) + modifyLineNumber));
                }
            } else {
                if (im.getPjpList() != null) {
                    units.insertAfter(jimpleUtils.virtualCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
                } else {
                    units.insertBefore(jimpleUtils.virtualCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
                }
            }
            modifyLineNumber += 1;
            for (int j = 0; j < returnList.size(); j++) {
                returnList.set(j, returnList.get(j) + modifyLineNumber);
            }
            insertPointList.set(i, insertPointList.get(i) + modifyLineNumber);
            unitList = new LinkedList<>(units);
        }
    }

    /**
     * Insert the Around advice method into the business logic
     *
     * @param currentMethod  The current method, the caller
     * @param calleeMethod  The method that needs to be instrumented, that is, the callee
     */
    protected void insertAOPAround(SootMethod currentMethod, SootMethod calleeMethod) {
        InsertMethod im = AOPAnalysis.insertMethodMap.get(currentMethod.getSignature());
        List<Integer> returnList = im.getReturnList();
        List<Integer> insertPointList = im.getPjpList() == null ? im.getInsertPointList() : im.getPjpList();

        int modifyLineNumber = 0;
        Local localModel = jimpleUtils.newLocalVar(calleeMethod.getDeclaringClass().getShortName().toLowerCase(), RefType.v(calleeMethod.getDeclaringClass().getName()));
        JimpleBody body = (JimpleBody) currentMethod.retrieveActiveBody();
        PatchingChain<Unit> units = body.getUnits();
        Local returnLocal = initLocalModel(currentMethod, calleeMethod, body, units, localModel);
        if (returnLocal == localModel) {
            modifyLineNumber += 2;
        } else {
            localModel = returnLocal;
        }

        List<Value> paramList = new ArrayList<>();
        int paramCount = calleeMethod.getParameterCount() - currentMethod.getParameterCount();
        if (im.getPjpList() == null) {
            paramCount--;
        }
        for (int j = 0; j < paramCount; j++) {
            if (!addJoinPointToParam(calleeMethod, j, body, paramList)) {
                paramList.add(NullConstant.v());
            }
        }
        paramList.addAll(body.getParameterLocals());
        if (im.getPjpList() == null) {
            for (Local local : body.getLocals()) {
                if (local.getName().equals("localTarget")) {
                    paramList.add(local);
                }
            }
        }
        Local returnRef = null;
        List<Unit> unitList = new LinkedList<>(units);
        for (int i = 0; i < insertPointList.size(); i++) {
            if (!(currentMethod.getReturnType() instanceof VoidType)) {
                if (returnRef == null) {
                    String returnRefName = unitList.get(returnList.get(i) + modifyLineNumber).toString().replace("return ", "");
                    for (Local local : body.getLocals()) {
                        if (local.getName().equals(returnRefName)) {
                            returnRef = local;
                            break;
                        }
                    }
                    if (returnRef == null) {
                        returnRef = jimpleUtils.newLocalVar(returnRefName, currentMethod.getReturnType());
                    }
                }

                Value returnValue = jimpleUtils.createVirtualInvokeExpr(localModel, calleeMethod, paramList);
                if (im.getPjpList() != null) {
                    units.insertAfter(jimpleUtils.createAssignStmt(returnRef, returnValue), unitList.get(insertPointList.get(i) + modifyLineNumber));
                } else {
                    units.insertBefore(jimpleUtils.createAssignStmt(returnRef, returnValue), unitList.get(insertPointList.get(i) + modifyLineNumber));
                }
            } else {
                if (im.getPjpList() != null) {
                    units.insertAfter(jimpleUtils.virtualCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
                } else {
                    units.insertBefore(jimpleUtils.virtualCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
                }
            }
            modifyLineNumber += 1;
            for (int j = 0; j < returnList.size(); j++) {
                returnList.set(j, returnList.get(j) + modifyLineNumber);
            }
            insertPointList.set(i, insertPointList.get(i) + modifyLineNumber);
            unitList = new LinkedList<>(units);
        }
    }

    /**
     * Insert the Before advice method into the business logic
     *
     * @param currentMethod  The current method, the caller
     * @param calleeMethod  The method that needs to be instrumented, that is, the callee
     */
    protected void insertAOPBefore(SootMethod currentMethod, SootMethod calleeMethod) {
        int modifyLineNumber = 0;
        Local localModel = jimpleUtils.newLocalVar(calleeMethod.getDeclaringClass().getShortName().toLowerCase(), RefType.v(calleeMethod.getDeclaringClass().getName()));
        JimpleBody body = (JimpleBody) currentMethod.retrieveActiveBody();
        PatchingChain<Unit> units = body.getUnits();
        Local returnLocal = initLocalModel(currentMethod, calleeMethod, body, units, localModel);
        if (returnLocal == localModel) {
            modifyLineNumber += 2;
        } else {
            localModel = returnLocal;
        }

        List<Value> paramList = new ArrayList<>();
        int paramCount = calleeMethod.getParameterCount();
        for (int j = 0; j < paramCount; j++) {
            if (!addJoinPointToParam(calleeMethod, j, body, paramList)) {
                paramList.add(NullConstant.v());
            }
        }

        InsertMethod im = AOPAnalysis.insertMethodMap.get(currentMethod.toString());
        List<Integer> returnList = im.getReturnList();
        List<Integer> insertPointList = im.getPjpList() == null ? im.getInsertPointList() : im.getPjpList();
        List<Unit> unitList = new LinkedList<>(units);

        for (int i = 0; i < insertPointList.size(); i++) {
            if (localModel != body.getThisLocal()) {
                units.insertBefore(jimpleUtils.virtualCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
            } else {
                units.insertBefore(jimpleUtils.specialCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
            }
            modifyLineNumber += 1;
            for (int j = 0; j < returnList.size(); j++) {
                returnList.set(j, returnList.get(j) + modifyLineNumber);
            }
            insertPointList.set(i, insertPointList.get(i) + modifyLineNumber);
            unitList = new LinkedList<>(units);
        }
    }

    /**
     * Insert the After advice method into the business logic
     *
     * @param currentMethod The current method, the caller
     * @param calleeMethod  The method that needs to be instrumented, that is, the callee
     */
    protected void insertAOPAfter(SootMethod currentMethod, SootMethod calleeMethod) {
        int modifyLineNumber = 0;
        Local localModel = jimpleUtils.newLocalVar(calleeMethod.getDeclaringClass().getShortName().toLowerCase(), RefType.v(calleeMethod.getDeclaringClass().getName()));
        JimpleBody body = (JimpleBody) currentMethod.retrieveActiveBody();
        PatchingChain<Unit> units = body.getUnits();
        Local returnLocal = initLocalModel(currentMethod, calleeMethod, body, units, localModel);
        if (returnLocal == localModel) {
            modifyLineNumber += 2;
        } else {
            localModel = returnLocal;
        }

        List<Value> paramList = new ArrayList<>();
        int paramCount = calleeMethod.getParameterCount();
        for (int j = 0; j < paramCount; j++) {
            if (!addJoinPointToParam(calleeMethod, j, body, paramList)) {
                paramList.add(NullConstant.v());
            }
        }

        InsertMethod im = AOPAnalysis.insertMethodMap.get(currentMethod.toString());
        List<Integer> returnList = im.getReturnList();
        List<Integer> insertPointList =
                (AOPAnalysis.newVersion && im.getPjpList() != null) ? im.getPjpList() : im.getInsertPointList();
        List<Unit> unitList = new LinkedList<>(units);
        for (int i = 0; i < insertPointList.size(); i++) {
            if (!AOPAnalysis.newVersion) {
                if (localModel != body.getThisLocal()) {
                    units.insertBefore(jimpleUtils.virtualCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
                } else {
                    units.insertBefore(jimpleUtils.specialCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
                }
            } else {
                units.insertAfter(jimpleUtils.specialCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
            }

            modifyLineNumber += 1;
            for (int j = 0; j < returnList.size(); j++) {
                returnList.set(j, returnList.get(j) + modifyLineNumber);
            }
            insertPointList.set(i, insertPointList.get(i) + modifyLineNumber - 1);
            unitList = new LinkedList<>(units);
        }
    }

    /**
     * Insert the AfterReturning advice method into the business logic
     *
     * @param currentMethod  The current method, the caller
     * @param calleeMethod  The method that needs to be instrumented, that is, the callee
     */
    protected void insertAOPAfterReturning(SootMethod currentMethod, SootMethod calleeMethod, List<String> expressionList) {
        int modifyLineNumber = 0;
        Local localModel = jimpleUtils.newLocalVar(calleeMethod.getDeclaringClass().getShortName().toLowerCase(), RefType.v(calleeMethod.getDeclaringClass().getName()));
        JimpleBody body = (JimpleBody) currentMethod.retrieveActiveBody();
        PatchingChain<Unit> units = body.getUnits();

        Local returnLocal = initLocalModel(currentMethod, calleeMethod, body, units, localModel);
        if (returnLocal == localModel) {
            modifyLineNumber += 2;
        } else {
            localModel = returnLocal;
        }

        List<Value> paramList = new ArrayList<>();
        int paramCount = calleeMethod.getParameterCount();
        int returnParamIndex = -1;
        for (int j = 0; j < paramCount; j++) {
            if (calleeMethod.getParameterTypes().get(j).toString().equals("java.lang.Object")
                    && expressionList.contains(calleeMethod.retrieveActiveBody().getParameterLocal(j).toString())) {
                returnParamIndex = j;
            }
            if (!addJoinPointToParam(calleeMethod, j, body, paramList)) {
                paramList.add(NullConstant.v());
            }
        }

        InsertMethod im = AOPAnalysis.insertMethodMap.get(currentMethod.toString());
        List<Integer> returnList = im.getReturnList();
        List<Integer> insertPointList =
                (AOPAnalysis.newVersion && im.getPjpList() != null) ? im.getPjpList() : im.getInsertPointList();
        List<Unit> unitList = new LinkedList<>(units);
        Local returnRef = null;
        for (int i = 0; i < insertPointList.size(); i++) {
            if (!(currentMethod.getReturnType() instanceof VoidType)) {
                if (returnRef == null) {
                    String returnRefName = unitList.get(returnList.get(i) + modifyLineNumber).toString().replace("return ", "");
                    if (returnParamIndex != -1) {
                        paramList.set(returnParamIndex, returnRef);
                    }
                    if (returnRef == null) {
                        returnRef = jimpleUtils.newLocalVar(returnRefName, currentMethod.getReturnType());
                    }
                }

            }

            if (!AOPAnalysis.newVersion) {
                if (localModel != body.getThisLocal()) {
                    units.insertBefore(jimpleUtils.virtualCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
                } else {
                    units.insertBefore(jimpleUtils.specialCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
                }
            } else {
                units.insertAfter(jimpleUtils.specialCallStatement(localModel, calleeMethod, paramList), unitList.get(insertPointList.get(i) + modifyLineNumber));
            }

            modifyLineNumber += 1;
            for (int j = 0; j < returnList.size(); j++) {
                returnList.set(j, returnList.get(j) + modifyLineNumber);
            }
            insertPointList.set(i, insertPointList.get(i) + modifyLineNumber - 1);
            unitList = new LinkedList<>(units);
        }
    }

    /**
     * Initialize the local variables of the current method
     *
     * @param currentMethod  The current method, the caller
     * @param calleeMethod  The method that needs to be instrumented, that is, the callee
     * @param body          The method body of the current method (that is, the caller)
     * @param units         The method body statement of the current method (that is, the caller)
     * @param localModel    The new local variables which need to be added
     * @return Local variable
     */
    private Local initLocalModel(SootMethod currentMethod, SootMethod calleeMethod, JimpleBody body, PatchingChain<Unit> units, Local localModel) {
        Local existLocal = isExistLocal(body.getLocals(), localModel);
        if (calleeMethod.getDeclaringClass().getName().equals(currentMethod.getDeclaringClass().getName())) {
            localModel = body.getThisLocal();
        } else if (existLocal == null) {
            body.getLocals().add(localModel);
            Unit localInitAssign = jimpleUtils.createAssignStmt(localModel, calleeMethod.getDeclaringClass().getName());
            units.addFirst(localInitAssign);
            units.insertAfter(jimpleUtils.specialCallStatement(localModel,
                    calleeMethod.getDeclaringClass().getMethodByName("<init>")),
                    localInitAssign);
        } else {
            localModel = existLocal;
        }
        return localModel;
    }

    /**
     * Add JoinPoint interface parameters to AOP related processing methods
     *
     * @param calleeMethod  The method that needs to be instrumented, that is, the callee
     * @param paramIndex   The index of formal parameter
     * @param body         The method body of the current method (that is, the caller)
     * @param paramList    The list of arguments
     * @return
     */
    private boolean addJoinPointToParam(SootMethod calleeMethod, int paramIndex, JimpleBody body, List<Value> paramList) {
        boolean continueFlag = false;
        if (calleeMethod.getParameterType(paramIndex).toString().contains("JoinPoint")) {
            for (Local local : body.getLocals()) {
                if (local.getType().toString().contains("JoinPoint")) {
                    paramList.add(local);
                    continueFlag = true;
                    break;
                }
            }
        }
        return continueFlag;
    }

    /**
     * Determine whether local variables already exist in the method body
     *
     * @param locals     The local variable table in the method body
     * @param localModel The new local variables need to be added
     * @return boolean value
     */
    private Local isExistLocal(Chain<Local> locals, Local localModel) {
        for (Local local : locals) {
            if (local.getName().equals(localModel.getName()) && local.getType().equals(localModel.getType())) {
                return local;
            }
        }
        return null;
    }

}
