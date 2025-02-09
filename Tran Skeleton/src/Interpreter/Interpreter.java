package Interpreter;

import AST.*;
import com.sun.jdi.Value;

import java.lang.reflect.Member;
import java.util.*;

public class Interpreter {
    private TranNode top;

    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        this.top = top;
        ClassNode console = new ClassNode();
        console.name = "console";
        ConsoleWrite print = new ConsoleWrite();
        print.name = "write";
        print.isPrivate = false;
        print.isVariadic = true;
        print.isShared = true;
        console.methods.add(print);
        top.Classes.add(console);

    }

    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */
    public void start() {
        List<InterpreterDataType> locals = new ArrayList<>();
        for(int i = 0; i < top.Classes.size(); i++) {
            for(int j = 0; j < top.Classes.get(i).methods.size(); j++) {
                if (top.Classes.get(i).methods.get(j).name.equals("start") && top.Classes.get(i).methods.get(j).isShared) {
                    interpretMethodCall(Optional.empty(), top.Classes.get(i).methods.get(j), locals);
                }
            }
        }
    }

    //              Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
        List<InterpreterDataType> result = null;
        result =  getParameters(object, locals, mc);

        if(mc.objectName.isPresent()){
            for(int i = 0; i < top.Classes.size(); i++) {
                if(top.Classes.get(i).name.equals(mc.objectName.get())) {
                    for(int j = 0; j < top.Classes.get(i).methods.size(); j++) {
                        if(top.Classes.get(i).methods.get(j).name.equals(mc.methodName)) {
                            return interpretMethodCall(object, top.Classes.get(i).methods.get(j), result);
                        }
                    }
                }
            }
        }
        Optional <String> optional = mc.objectName;

        if(optional.isPresent()) {

            InterpreterDataType type = locals.get(mc.objectName.get());
            ObjectIDT newObject = ((ReferenceIDT) type).refersTo.get();
            for(int i = 0; i < ((ReferenceIDT) type).refersTo.get().astNode.methods.size(); i++) {
                if(doesMatch(((ReferenceIDT) type).refersTo.get().astNode.methods.get(i), mc, result)){
                    return interpretMethodCall(Optional.of(newObject), ((ReferenceIDT) type).refersTo.get().astNode.methods.get(i), result);
                }
            }
        } else{
            for(int i = 0; i < object.get().astNode.methods.size(); i++) {
                if (doesMatch(object.get().astNode.methods.get(i), mc, result)){
                    return interpretMethodCall(object, object.get().astNode.methods.get(i), result);
                }
            }
        }

        return result;
    }

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
        var retVal = new LinkedList<InterpreterDataType>();
        if(m instanceof BuiltInMethodDeclarationNode){
            ((BuiltInMethodDeclarationNode) m).Execute(values);
        } else {
            HashMap<String, InterpreterDataType> variables = new HashMap<String, InterpreterDataType>();
            for(int i = 0; i < m.locals.size(); i++){
                variables.put(m.locals.get(i).name, instantiate(m.locals.get(i).type));
            }
            for(int i = 0; i < m.parameters.size(); i++) {
                variables.put(m.parameters.get(i).name, instantiate(m.parameters.get(i).type));
            }
            for(int i = 0; i < m.returns.size(); i++) {
                variables.put(m.returns.get(i).name, instantiate(m.returns.get(i).type));
            }

            if(m.parameters.size() != values.size()){
                throw new RuntimeException("Method call parameters don't match");
            }
                interpretStatementBlock(object, m.statements, variables);

                for(int j = 0; j < m.returns.size(); j++){
                    if(variables.containsKey(m.returns.get(j).name)){
                        retVal.add(variables.get(m.returns.get(j).name));
                    }
                }
        }
        return retVal;
    }

    //              Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
        var retVal = new LinkedList<InterpreterDataType>();
        retVal.addAll(getParameters(callerObj, locals, mc));
        
        var newClass = getClassByName(mc.methodName);

        if(newClass.isPresent()){
            for(int i = 0; i < newClass.get().constructors.size(); i++) {
                if(doesConstructorMatch(newClass.get().constructors.get(i), mc, retVal)){
                    interpretConstructorCall(newOne, newClass.get().constructors.get(i), retVal);
                }
            }
        }else {
            throw new RuntimeException("Class name not found");
        }
    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
        var retVal = new LinkedList<InterpreterDataType>();
        for(int i = 0; i < c.locals.size(); i++) {
            retVal.add(instantiate(c.locals.get(i).type));
        }

        if(!(values.size() == c.parameters.size())) {
            throw new RuntimeException("Size mismatch");
        }else {
            HashMap<String, InterpreterDataType> locals = new HashMap<>();
            for(int i = 0; i < values.size(); i++) {
                locals.put(c.parameters.get(i).name, values.get(i));
            }
            interpretStatementBlock(Optional.of(object), c.statements, locals);
        }
    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block, run each statement.
     * Blocks, by definition, do every statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call doMethodCall(). Loop over the returned values and copy them into our local variables.
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {
        for(int i = 0; i < statements.size(); i++) {
            StatementNode statementsNode = statements.get(i);
            if(statements.get(i) instanceof AssignmentNode){
                var target = findVariable(((AssignmentNode) statementsNode).target.name, locals, object);
                target.Assign(evaluate(locals,object,((AssignmentNode) statementsNode).expression));
            } else if(statements.get(i) instanceof MethodCallStatementNode){
                List<InterpreterDataType> parameterList = new LinkedList<InterpreterDataType>();
                for(int j =0; j < ((MethodCallStatementNode) statements.get(i)).parameters.size(); j++) {
                    parameterList.add(evaluate(locals,object,((MethodCallStatementNode) statements.get(i)).parameters.get(j)));
                }

                LinkedList<InterpreterDataType> returnList = new LinkedList<InterpreterDataType>();
                returnList.addAll(findMethodForMethodCallAndRunIt(object, locals, ((MethodCallStatementNode) statements.get(i))));

                for(int k = 0; k < returnList.size(); k++) {
                    if(locals.containsKey(((MethodCallStatementNode) statements).returnValues.get(k).name)){
                        var target = locals.get(k);
                        target.Assign(returnList.get(k));
                    }
                }
            } else if(statements.get(i) instanceof LoopNode){
                if(((LoopNode) statements.get(i)).assignment.isPresent()){
                    InterpreterDataType evaluate = evaluate(locals,object,((LoopNode) statements.get(i)).assignment.get());
                } else {
                    InterpreterDataType evaluateVariable = evaluate(locals, object, (((LoopNode) statements.get(i)).expression));
                    if (evaluateVariable instanceof BooleanIDT) {
                        while (((BooleanIDT) evaluateVariable).Value) {
                            interpretStatementBlock(object, ((LoopNode) statements.get(i)).statements, locals);
                            evaluateVariable = evaluate(locals, object, ((LoopNode) statements.get(i)).expression);
                        }
                    } else if (evaluateVariable instanceof ObjectIDT) {
                        ObjectIDT newObject = (ObjectIDT) evaluateVariable;
                        for(int j = 0; j < newObject.astNode.methods.size(); j++){
                            if(newObject.astNode.methods.get(j).name.equals("getNext")){
                                var value = (NumberIDT) locals.get(newObject.astNode.methods.get(j).locals.getFirst());
                                float max = value.Value;
                                int min = 0;
                                while(min <= max){
                                    interpretStatementBlock(object, ((LoopNode) statements.get(i)).statements, locals);
                                    min++;
                                }
                            }
                        }
                    }
                }
            } else if(statements.get(i) instanceof IfNode){
                var evaluateVariable = evaluate(locals, object, (((IfNode) statements.get(i)).condition));
                if(evaluateVariable instanceof BooleanIDT){
                    if(((BooleanIDT) evaluateVariable).Value){
                        interpretStatementBlock(object,((IfNode) statements.get(i)).statements,locals);
                    } else {
                        interpretStatementBlock(object,((IfNode) statements.get(i)).elseStatement.get().statements, locals);
                    }
                }

            }
        }
    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all of the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call doMethodCall() findMethodForMethodCallAndRunIt() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {

        if(expression instanceof BooleanLiteralNode){
            var booleanIDT = new BooleanIDT(((BooleanLiteralNode) expression).value);
            return booleanIDT;

        } else if(expression instanceof CharLiteralNode){
            var charIDT = new CharIDT(((CharLiteralNode) expression).value);
            return charIDT;
        } else if(expression instanceof StringLiteralNode){
            var stringIDT = new StringIDT(((StringLiteralNode) expression).value);
            return stringIDT;
        } else if(expression instanceof NumericLiteralNode){
            var numberIDT = new NumberIDT(((NumericLiteralNode) expression).value);
            return numberIDT;
        } else if(expression instanceof NewNode){
            NewNode newNode = (NewNode) expression;
            ObjectIDT obj = null;
            String string = newNode.className;

            for(int i = 0; i < top.Classes.size(); i++) {
                if(top.Classes.get(i).name.equals(string)){
                    obj = new ObjectIDT(top.Classes.get(i));
                    for(int j = 0; j < top.Classes.get(i).members.size(); j++) {
                        obj.members.put(top.Classes.get(i).members.get(j).declaration.name, instantiate(top.Classes.get(i).members.get(j).declaration.type));
                    }
                }
            }
            MethodCallStatementNode methodCall = new MethodCallStatementNode();

            methodCall.methodName = string;
            methodCall.parameters = newNode.parameters;

            ReferenceIDT reference = new ReferenceIDT();
            reference.refersTo = Optional.of(obj);
            findConstructorAndRunIt(object, locals, methodCall, obj);
            return reference;
        } else if(expression instanceof BooleanOpNode){
            var booleanOpLeft = evaluate(locals, object, ((BooleanOpNode) expression).left);
            var booleanOpRight = evaluate(locals, object, ((BooleanOpNode) expression).right);
            if(((BooleanOpNode) expression).op.equals(BooleanOpNode.BooleanOperations.and)) {
                if (((BooleanIDT) booleanOpLeft).Value && ((BooleanIDT) booleanOpRight).Value) {
                    return new BooleanIDT(true);
                }
            }
            if(((BooleanOpNode) expression).op.equals(BooleanOpNode.BooleanOperations.or)) {
                if (((BooleanIDT) booleanOpLeft).Value || ((BooleanIDT) booleanOpRight).Value) {
                    return new BooleanIDT(true);
                } else {
                    return new BooleanIDT(false);
                }
            }
        } else if(expression instanceof  CompareNode){
            var compareNodeLeft = evaluate(locals, object, ((CompareNode) expression).left);
            var compareNodeRight = evaluate(locals, object, ((CompareNode) expression).right);
            if(((CompareNode) expression).op.equals(CompareNode.CompareOperations.lt)){
                if(((NumberIDT) compareNodeLeft).Value < ((NumberIDT) compareNodeRight).Value){
                    return new BooleanIDT(true);
                } else {
                    return new BooleanIDT(false);
                }
            } else if(((CompareNode) expression).op.equals(CompareNode.CompareOperations.le)){
                if(((NumberIDT) compareNodeLeft).Value <= ((NumberIDT) compareNodeRight).Value){
                    return new BooleanIDT(true);
                } else {
                    return new BooleanIDT(false);
                }
            } else if(((CompareNode) expression).op.equals(CompareNode.CompareOperations.gt)){
                if(((NumberIDT) compareNodeLeft).Value > ((NumberIDT) compareNodeRight).Value){
                    return new BooleanIDT(true);
                } else {
                    return new BooleanIDT(false);
                }
            } else if(((CompareNode) expression).op.equals(CompareNode.CompareOperations.ge)){
                if(((NumberIDT) compareNodeLeft).Value >= ((NumberIDT) compareNodeRight).Value){
                    return new BooleanIDT(true);
                } else {
                    return new BooleanIDT(false);
                }
            } else if(((CompareNode) expression).op.equals(CompareNode.CompareOperations.eq)){
                if(((NumberIDT) compareNodeLeft).Value == ((NumberIDT) compareNodeRight).Value){
                    return new BooleanIDT(true);
                } else {
                    return new BooleanIDT(false);
                }
            } else if(((CompareNode) expression).op.equals(CompareNode.CompareOperations.ne)){
                if(((NumberIDT) compareNodeLeft).Value != ((NumberIDT) compareNodeRight).Value){
                    return new BooleanIDT(true);
                } else {
                    return new BooleanIDT(false);
                }
            }
        } else if(expression instanceof MathOpNode){
             var mathOpNodeLeft = evaluate(locals, object, ((MathOpNode) expression).left);
             var mathOpNodeRight = evaluate(locals, object, ((MathOpNode) expression).right);

             if(((MathOpNode) expression).op.equals(MathOpNode.MathOperations.add)){
                 return new NumberIDT(((NumberIDT) mathOpNodeLeft).Value + ((NumberIDT) mathOpNodeRight).Value);
             } else if(((MathOpNode) expression).op.equals(MathOpNode.MathOperations.subtract)){
                 return new NumberIDT(((NumberIDT) mathOpNodeLeft).Value - ((NumberIDT) mathOpNodeRight).Value);
             } else if(((MathOpNode) expression).op.equals(MathOpNode.MathOperations.multiply)){
                 return new NumberIDT(((NumberIDT) mathOpNodeLeft).Value * ((NumberIDT) mathOpNodeRight).Value);
             } else if(((MathOpNode) expression).op.equals(MathOpNode.MathOperations.divide)){
                 return new NumberIDT(((NumberIDT) mathOpNodeLeft).Value / ((NumberIDT) mathOpNodeRight).Value);
             } else if(((MathOpNode) expression).op.equals(MathOpNode.MathOperations.modulo)){
                 return new NumberIDT(((NumberIDT) mathOpNodeLeft).Value % ((NumberIDT) mathOpNodeRight).Value);
             }
             //TODO ask if it works this way
        } else if(expression instanceof  MethodCallExpressionNode){
            MethodCallStatementNode newNode = new MethodCallStatementNode((MethodCallExpressionNode) expression);
            return findMethodForMethodCallAndRunIt(object, locals, newNode).getFirst();
        } else if(expression instanceof  VariableReferenceNode){
            return findVariable(((VariableReferenceNode) expression).name, locals,object);
        }
        throw new IllegalArgumentException();
    }

    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method declaration, does it match this method call?
     * We double check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
     *
     * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
     * If all of those match, consider the types (use TypeMatchToIDT).
     * If everything is OK, return true, else return false.
     * Note - if m is a built-in and isVariadic is true, skip all of the parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {

        if(!(m.name.equals(mc.methodName))) {
            return false;
        }
        if(m instanceof BuiltInMethodDeclarationNode){
            if((((BuiltInMethodDeclarationNode) m).isVariadic)){
                return true;
            }
        }
        if(parameters.size() == mc.parameters.size() && (m.returns.size() == mc.returnValues.size() || mc.returnValues.isEmpty())){
            for(int i = 0; i < parameters.size(); i++){
                if(!(typeMatchToIDT(m.parameters.get(i).type, parameters.get(i)))){
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        if(c.parameters.size() == parameters.size() && mc.parameters.size() == parameters.size()){
            for(int i = 0; i < c.parameters.size(); i++){
                if(!(typeMatchToIDT(c.parameters.get(i).type, parameters.get(i)))){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallStatementNode mc) {
        var retVal = new LinkedList<InterpreterDataType>();

        for(int i = 0; i < mc.parameters.size(); i++) {
            retVal.add(evaluate(locals, object, mc.parameters.get(i)));
        }
        return retVal;
    }

    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc) - does the string type match the name of that IDT ("boolean", etc)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (refered to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {

        if(idt instanceof StringIDT && type.equals("string")) {
            return true;
        }  else if (idt instanceof NumberIDT && type.equals("number")) {
            return true;
        } else if (idt instanceof BooleanIDT && type.equals("boolean")) {
            return true;
        } else if (idt instanceof CharIDT && type.equals("character")) {
            return true;
        } else if(idt instanceof ObjectIDT){
            ClassNode classNode = new ClassNode();
            classNode = ((ObjectIDT) idt).astNode;

            if(classNode.name.equals(type)){
                return true;
            }
            for(int i = 0; i < classNode.interfaces.size(); i++){
                if(classNode.interfaces.get(i).equals(type)){
                    return true;
                }
            }
        } else if (idt instanceof ReferenceIDT){
            if(((ReferenceIDT) idt).refersTo.equals(type)){
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
        throw new RuntimeException("Unable to resolve type " + type);
    }

    /**
     * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
     *
     * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        MethodDeclarationNode methodDeclarationNode = new MethodDeclarationNode();
        ClassNode classNode = new ClassNode();
        classNode = object.astNode;

        for(int i = 0; i < classNode.methods.size(); i++) {
            if(classNode.methods.get(i).equals(object)) {
                methodDeclarationNode = classNode.methods.get(i);
                if(doesMatch(methodDeclarationNode,mc,parameters)){
                    return methodDeclarationNode;
                }
            }
        }
        throw new RuntimeException("Unable to resolve method call " + mc);
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {
        if(top != null) {
            for(int i = 0; i < top.Classes.size(); i++) {
                if(top.Classes.get(i).name.equals(name)){
                    return Optional.of(top.Classes.get(i));
                }
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * Given an execution environment (the current object, the current local variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String,InterpreterDataType> locals, Optional<ObjectIDT> object) {
        if(locals.containsKey(name)) {
            return locals.get(name);
        } else if(object.isPresent()) {
            ObjectIDT obj = object.get();
            if(obj.members.containsKey(name)) {
                return obj.members.get(name);
            }
        }
        throw new RuntimeException("Unable to find variable " + name);
    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {
        if(type.equals("string")){
            StringIDT stringIDT = new StringIDT("");
            return stringIDT;
        } else if(type.equals("number")){
            NumberIDT numberIDT = new NumberIDT(0);
            return numberIDT;
        } else if (type.equals("boolean")){
            BooleanIDT booleanIDT = new BooleanIDT(false);
            return booleanIDT;
        } else if (type.equals("character")){
            CharIDT charIDT = new CharIDT(' ');
            return charIDT;
        } else {
            ReferenceIDT referenceIDT = new ReferenceIDT();
            return referenceIDT;
        }
    }
}
