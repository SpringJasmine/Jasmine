package utils;

import soot.*;
import soot.jimple.*;

import java.util.List;

/**
 * @ClassName   BaseJimpleUtils
 * @Description Encapsulate the original jimple statement according to
 * the function (basic statement) for convenient development
 **/
public class BaseJimpleUtils {
    /**
     * New local variable
     *
     * @param localName local variable name
     * @param vtype     local variable type
     * @return local variable
     */
    public Local newLocalVar(String localName, String vtype) {
        return Jimple.v().newLocal(localName, RefType.v(vtype));
    }

    /**
     * New local variable
     *
     * @param localName local variable name
     * @param vtype     local variable type
     * @return local variable
     */
    public Local newLocalVar(String localName, Type vtype) {
        return Jimple.v().newLocal(localName, vtype);
    }

    /**
     * Add the local variable to the method body
     *
     * @param localName local variable name
     * @param vtype     local variable type
     * @param body      the body need to add local variable
     * @return local variable
     */
    public Local addLocalVar(String localName, String vtype, Body body) {
        Local local = newLocalVar(localName, vtype);
        body.getLocals().add(local);
        return local;
    }

    /**
     * Add the local variable to the method body
     *
     * @param localName local variable name
     * @param vtype     local variable type
     * @param body      the body need to add local variable
     * @return local variable
     */
    public Local addLocalVar(String localName, Type vtype, Body body) {
        Local local = newLocalVar(localName, vtype);
        body.getLocals().add(local);
        return local;
    }

    /**
     * Instantiate local variables
     * create an allocation statement：tmpRef = new com.demo.service.impl.ModelThreeServiceImpl;
     *
     * @param local    local variable
     * @param realType the type of allocation
     * @return unit
     */
    public Unit createAssignStmt(Local local, String realType) {
        return Jimple.v().newAssignStmt(local, Jimple.v().newNewExpr(RefType.v(realType)));
    }

    /**
     * Instantiate local variables
     * create an allocation statement：tmpRef = new com.demo.service.impl.ModelThreeServiceImpl;
     *
     * @param local    local variable
     * @param realType the type of allocation
     * @param units    the body need to add statement
     */
    public void createAssignStmt(Local local, String realType, PatchingChain<Unit> units) {
        units.add(Jimple.v().newAssignStmt(local, Jimple.v().newNewExpr(RefType.v(realType))));
    }

    /**
     * Instantiate variables
     *
     * @param var     local variable
     * @param realvar the type of allocation
     * @return unit
     */
    public Unit createAssignStmt(Value var, Value realvar) {
        return Jimple.v().newAssignStmt(var, realvar);
    }

    /**
     * Instantiate variables
     *
     * @param left  variable
     * @param right statement
     * @param units units
     */
    public void createAssignStmt(Value left, Value right, PatchingChain<Unit> units) {
        units.add(Jimple.v().newAssignStmt(left, right));
    }

    /**
     * Create new statement
     *
     * @param declType The specific type that allocation
     * @return new statement
     */
    public NewExpr createNewExpr(String declType) {
        return Jimple.v().newNewExpr(RefType.v(declType));
    }

    /**
     * Create new statement
     *
     * @param declType The specific type that allocation
     * @return new statement
     */
    public NewExpr createNewExpr(RefType declType) {
        return Jimple.v().newNewExpr(declType);
    }

    /**
     * Create new type[] statement
     *
     * @param type      array type
     * @param paramSize array dimension
     * @return array statement
     */
    public NewArrayExpr createNewArrayExpr(String type, int paramSize) {
        return Jimple.v().newNewArrayExpr(RefType.v(type), IntConstant.v(paramSize));
    }

    /**
     * Create special invoke statement
     *
     * @param localModel   variable points to the instantiated object
     * @param calleeMethod callee Method
     * @return invoke statement
     */
    public SpecialInvokeExpr createSpecialInvokeExpr(Local localModel, SootMethod calleeMethod) {
        return Jimple.v().newSpecialInvokeExpr(localModel, calleeMethod.makeRef());
    }

    /**
     * Create special invoke statement
     *
     * @param localModel   variable points to the instantiated object
     * @param calleeMethod callee Method
     * @param values       the actual params of invoke statement
     * @return invoke statement
     */
    public SpecialInvokeExpr createSpecialInvokeExpr(Local localModel, SootMethod calleeMethod, List<? extends Value> values) {
        return Jimple.v().newSpecialInvokeExpr(localModel, calleeMethod.makeRef(), values);
    }

    /**
     * Create special call statement
     * for example: specialinvoke tmpRef.<com.demo.service.impl.ModelThreeServiceImpl: void <init>()>();
     *
     * @param localModel variable points to the instantiated object
     * @param methodSign method signature
     * @return unit
     */
    public Unit specialCallStatement(Local localModel, String methodSign) {
        SootMethod toCall = Scene.v().getMethod(methodSign);
        return Jimple.v().newInvokeStmt(createSpecialInvokeExpr(localModel, toCall));
    }

    /**
     * Create special call statement
     * for example: specialinvoke tmpRef.<com.demo.service.impl.ModelThreeServiceImpl: void <init>()>();
     *
     * @param localModel   variable points to the instantiated object
     * @param calleeMethod method signature
     * @return unit
     */
    public Unit specialCallStatement(Local localModel, SootMethod calleeMethod) {
        return Jimple.v().newInvokeStmt(createSpecialInvokeExpr(localModel, calleeMethod));
    }

    /**
     * Create special call statement
     * for example: specialinvoke tmpRef.<com.demo.service.impl.ModelThreeServiceImpl: void <init>()>();
     *
     * @param localModel variable points to the instantiated object
     * @param values     the actual params of invoke statement
     * @return unit
     */
    public Unit specialCallStatement(Local localModel, SootMethod calleeMethod, List<Value> values) {
        return Jimple.v().newInvokeStmt(createSpecialInvokeExpr(localModel, calleeMethod, values));
    }

    /**
     * Create special call statement
     * for example: specialinvoke tmpRef.<com.demo.service.impl.ModelThreeServiceImpl: void <init>()>();
     *
     * @param localModel variable points to the instantiated object
     * @param methodSign method signature
     * @param values     the actual params of invoke statement
     * @return unit
     */

    public Unit specialCallStatement(Local localModel, String methodSign, List<Value> values) {
        SootMethod toCall = Scene.v().getMethod(methodSign);
        return Jimple.v().newInvokeStmt(createSpecialInvokeExpr(localModel, toCall, values));
    }

    /**
     * Create virtual call statement
     *
     * @param localModel   variable points to the instantiated object
     * @param calleeMethod callee Method
     * @return invoke statement
     */
    public VirtualInvokeExpr createVirtualInvokeExpr(Local localModel, SootMethod calleeMethod) {
        return Jimple.v().newVirtualInvokeExpr(localModel, calleeMethod.makeRef());
    }

    /**
     * Create virtual call statement
     *
     * @param localModel   variable points to the instantiated object
     * @param calleeMethod callee Method
     * @param values       the actual params of invoke statement
     * @return invoke statement
     */
    public VirtualInvokeExpr createVirtualInvokeExpr(Local localModel, SootMethod calleeMethod, List<? extends Value> values) {
        return Jimple.v().newVirtualInvokeExpr(localModel, calleeMethod.makeRef(), values);
    }

    /**
     * Create virtual call statement
     *
     * @param localModel variable points to the instantiated object
     * @param methodSign method signature
     * @return unit
     */
    public Unit virtualCallStatement(Local localModel, String methodSign) {
        SootMethod toCall = Scene.v().getMethod(methodSign);
        return Jimple.v().newInvokeStmt(createVirtualInvokeExpr(localModel, toCall));
    }

    /**
     * Create virtual call statement
     *
     * @param localModel variable points to the instantiated object
     * @param methodSign method signature
     * @param values     the actual params of invoke statement
     * @return unit
     */
    public Unit virtualCallStatement(Local localModel, String methodSign, List<? extends Value> values) {
        SootMethod toCall = Scene.v().getMethod(methodSign);
        return Jimple.v().newInvokeStmt(createVirtualInvokeExpr(localModel, toCall, values));
    }

    /**
     * Create virtual call statement
     *
     * @param localModel variable points to the instantiated object
     * @param calleeMethod callee Method
     * @return unit
     */
    public Unit virtualCallStatement(Local localModel, SootMethod calleeMethod) {
        return Jimple.v().newInvokeStmt(createVirtualInvokeExpr(localModel, calleeMethod));
    }

    /**
     * Create virtual call statement
     *
     * @param localModel   variable points to the instantiated object
     * @param calleeMethod callee Method
     * @param values       the actual params of invoke statement
     * @return unit
     */
    public Unit virtualCallStatement(Local localModel, SootMethod calleeMethod, List<? extends Value> values) {
        return Jimple.v().newInvokeStmt(createVirtualInvokeExpr(localModel, calleeMethod, values));
    }

    /**
     * Create static call statement
     *
     * @param calleeMethod callee Method
     * @return static invoke statement
     */
    public StaticInvokeExpr createStaticInvokeExpr(SootMethod calleeMethod) {
        return Jimple.v().newStaticInvokeExpr(calleeMethod.makeRef());
    }

    /**
     * Initialize the parameters and this pointer
     * this := @this: com.demo.targetdemo.infoleak.service.impl.UserServiceCGLIB;
     *
     * @param var      the parameters and this pointer
     * @param identvar type
     * @return unit
     */
    public Unit createIdentityStmt(Value var, Value identvar) {
        return Jimple.v().newIdentityStmt(var, identvar);
    }

    /**
     * Initialize the parameters and this pointer
     * this := @this: com.demo.targetdemo.infoleak.service.impl.UserServiceCGLIB;
     *
     * @param var      the parameters and this pointer
     * @param identvar type
     * @param units    units
     */
    public void createIdentityStmt(Value var, Value identvar, PatchingChain<Unit> units) {
        units.add(createIdentityStmt(var, identvar));
    }

    /**
     * Create parameter references
     * // @parameter0: com.demo.targetdemo.infoleak.service.impl.UserServiceCGLIB
     *
     * @param type  The specific type of the parameter
     * @param index the index of params
     * @return ParameterRef
     */
    public ParameterRef createParamRef(Type type, int index) {
        return Jimple.v().newParameterRef(type, index);
    }

    /**
     * Create a reference to this pointer
     * // @this: com.demo.targetdemo.infoleak.service.impl.UserServiceCGLIB
     *
     * @param type The specific type of this
     * @return ThisRef
     */
    public ThisRef createThisRef(String type) {
        return createThisRef(RefType.v(type));
    }

    /**
     * Create a reference to this pointer
     * // @this: com.demo.targetdemo.infoleak.service.impl.UserServiceCGLIB
     *
     * @param type The specific type of this
     * @return ThisRef
     */
    public ThisRef createThisRef(RefType type) {
        return Jimple.v().newThisRef(type);
    }

    /**
     * Create array reference statement
     *
     * @param type  The specific type of array
     * @param index the index of params
     * @return ArrayRef
     */
    public ArrayRef createArrayRef(Value type, int index) {
        return Jimple.v().newArrayRef(type, IntConstant.v(index));
    }

    /**
     * add return; statement
     *
     * @param units unit
     */
    public void addVoidReturnStmt(PatchingChain<Unit> units) {
        units.add(Jimple.v().newReturnVoidStmt());
    }

    /**
     * add return local; statement
     *
     * @param returnRef return value
     * @param units     unit
     */
    public void addCommonReturnStmt(Value returnRef, PatchingChain<Unit> units) {
        units.add(Jimple.v().newReturnStmt(returnRef));
    }


    /**
     * Use the variable to call the field of the instance, for example:
     * this.<com.demo.modelcontroller.ModelThreeController: com.demo.service.ModelThreeService modelThreeService>;
     *
     * @param local        local variable
     * @param sootFieldRef Reference to instance field
     * @return InstanceFieldRef
     */
    public InstanceFieldRef createInstanceFieldRef(Value local, SootFieldRef sootFieldRef) {
        return Jimple.v().newInstanceFieldRef(local, sootFieldRef);
    }

    /**
     * Use static field, for example:
     * <com.demo.modelcontroller.ModelThreeController: com.demo.service.ModelThreeService modelThreeService>;
     *
     * @param sootFieldRef Reference to static field
     * @return StaticFieldRef
     */
    public StaticFieldRef createStaticFieldRef(SootFieldRef sootFieldRef) {
        return Jimple.v().newStaticFieldRef(sootFieldRef);
    }

    /**
     * Create a new body for the method
     *
     * @param sootMethod method
     * @return method body
     */
    public JimpleBody newMethodBody(SootMethod sootMethod) {
        return Jimple.v().newBody(sootMethod);
    }
}
