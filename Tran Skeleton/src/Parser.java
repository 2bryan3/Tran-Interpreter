import AST.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Parser {
    private TokenManager newManager;
    private TranNode tranNode;

    public Parser(TranNode top, List<Token> tokens) {
        newManager = new TokenManager(tokens);
        tranNode = top;
    }

    // Tran = { Class | Interface }
    public void Tran() throws SyntaxErrorException {
        while (!newManager.done() && newManager.peekCurrent().get().getType() == Token.TokenTypes.INTERFACE) {
            if (newManager.matchAndRemove(Token.TokenTypes.INTERFACE).isPresent()) {
                tranNode.Interfaces.add(interfaceParser());
                while(!newManager.done() && newManager.peekCurrent().get().getType() == Token.TokenTypes.NEWLINE) {
                    requireNewLine();
                }
            }
        }
        if (newManager.matchAndRemove(Token.TokenTypes.CLASS).isPresent()) {
            tranNode.Classes.add(classParser());
        }
    }

    public NumericLiteralNode numberLiteralParser() throws SyntaxErrorException {
        var numberLiteralNode = new NumericLiteralNode();
        var number = newManager.peekCurrent().get().getValue();

        if(newManager.matchAndRemove(Token.TokenTypes.NUMBER).isPresent()) {
            numberLiteralNode.value = Float.parseFloat(number.toString());
        }
        return numberLiteralNode;
    }

    public StringLiteralNode stringLiteralParser() throws SyntaxErrorException {
        var stringLiteralNode = new StringLiteralNode();
        Optional <Token> token = newManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING);

        if(token.isPresent()) {
            stringLiteralNode.value = token.get().getValue();
        }
        return stringLiteralNode;
    }

    public CharLiteralNode charLiteralParser() throws SyntaxErrorException {
        var charLiteralNode = new CharLiteralNode();
        var ch = newManager.peekCurrent().get().getValue().charAt(0);

        if(newManager.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER).isPresent()) {
            charLiteralNode.value = ch;
        }
        return charLiteralNode;
    }

    public ExpressionNode expressionParser() throws SyntaxErrorException{
        var current = termParser();

        if(newManager.peekCurrent().get().getType() == Token.TokenTypes.PLUS ||
                newManager.peekCurrent().get().getType() == Token.TokenTypes.MINUS) {
            var expressionNode = new MathOpNode();
            expressionNode.left = current;
            if(newManager.matchAndRemove(Token.TokenTypes.PLUS).isPresent()){
                expressionNode.op = MathOpNode.MathOperations.add;
            } else if(newManager.matchAndRemove(Token.TokenTypes.MINUS).isPresent()){
                expressionNode.op = MathOpNode.MathOperations.subtract;
            }
            expressionNode.right = termParser();

            while(newManager.peekCurrent().get().getType() == Token.TokenTypes.PLUS ||
                    newManager.peekCurrent().get().getType() == Token.TokenTypes.MINUS) {
                var expressionNode2 = new MathOpNode();
                expressionNode2.left = expressionNode;
                if(newManager.matchAndRemove(Token.TokenTypes.PLUS).isPresent()){
                    expressionNode2.op = MathOpNode.MathOperations.add;
                } else if(newManager.matchAndRemove(Token.TokenTypes.MINUS).isPresent()){
                    expressionNode2.op = MathOpNode.MathOperations.subtract;
                }
                expressionNode2.right = termParser();
                return expressionNode2;
            }
            return expressionNode;
        } else {
            return current;
        }
    }

    public ExpressionNode termParser() throws SyntaxErrorException{
        var current = factorParser();

        if(newManager.peekCurrent().get().getType() == Token.TokenTypes.TIMES ||
        newManager.peekCurrent().get().getType() == Token.TokenTypes.DIVIDE ||
        newManager.peekCurrent().get().getType() == Token.TokenTypes.MODULO) {
            var expressionNode = new MathOpNode();
            expressionNode.left = current;
            if(newManager.matchAndRemove(Token.TokenTypes.TIMES).isPresent()) {
                expressionNode.op = MathOpNode.MathOperations.multiply;
            } else if(newManager.matchAndRemove(Token.TokenTypes.DIVIDE).isPresent()) {
                expressionNode.op = MathOpNode.MathOperations.divide;
            } else {
                expressionNode.op = MathOpNode.MathOperations.modulo;
            }
            expressionNode.right = factorParser();
            return expressionNode;
        } else {
            return current;
        }
    }

    public ExpressionNode factorParser() throws SyntaxErrorException{
        var methodCallExpressionNode = methodCallExpressionParser();

        if(methodCallExpressionNode.isPresent()){
            return methodCallExpressionNode.get();
        } else if(newManager.peekCurrent().get().getType() == Token.TokenTypes.NUMBER){
            return numberLiteralParser();
        } else if (newManager.peekCurrent().get().getType() == Token.TokenTypes.WORD){
            return variableReferenceParser();
        } else if (newManager.peekCurrent().get().getType() == Token.TokenTypes.TRUE
        || newManager.peekCurrent().get().getType() == Token.TokenTypes.FALSE) {

            if(newManager.matchAndRemove(Token.TokenTypes.TRUE).isPresent()) {
                var expressionNode = new BooleanLiteralNode(true);
                return expressionNode;
            } else if(newManager.matchAndRemove(Token.TokenTypes.FALSE).isPresent()) {
                var expressionNode = new BooleanLiteralNode(false);
                return expressionNode;
            }
        } else if (newManager.peekCurrent().get().getType() == Token.TokenTypes.QUOTEDSTRING){
            return stringLiteralParser();
        } else if (newManager.peekCurrent().get().getType() == Token.TokenTypes.QUOTEDCHARACTER){
            return charLiteralParser();
        } else if (newManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            var expressionNode = expressionParser();
            if(newManager.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()) {
                return expressionNode;
            } else {
                throw new SyntaxErrorException("Missing right parenthesis", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
            }
        } else if (newManager.matchAndRemove(Token.TokenTypes.NEW).isPresent()) {
            var newNode = new NewNode();

            if(newManager.peekCurrent().get().getType() == Token.TokenTypes.WORD){
                newNode.className = newManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
            }
            if(!newManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
                throw new SyntaxErrorException("Missing left parenthesis", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
            }
            if (!newManager.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()) {
                newNode.parameters.add(expressionParser());

                while(newManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                    newNode.parameters.add(expressionParser());
                }

                newManager.matchAndRemove(Token.TokenTypes.RPAREN);
            }

            return newNode;
        }
        throw new SyntaxErrorException("Invalid factor expression", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
    }

    public Optional<StatementNode> disambiguate() throws SyntaxErrorException {
        var methodCallNode = methodCallExpressionParser();
        if(methodCallNode.isPresent()) {
            var methodCallStatementNode = new MethodCallStatementNode();
            methodCallStatementNode.objectName = methodCallNode.get().objectName;
            methodCallStatementNode.methodName = methodCallNode.get().methodName;
            methodCallStatementNode.parameters = methodCallNode.get().parameters;
            return Optional.of(methodCallStatementNode);
        }
        var variableDeclarationNode = newManager.peekCurrent().get().getType();
        if(variableDeclarationNode == Token.TokenTypes.WORD) {
            if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)) {
                var statementNode = assignmentParser();
                return Optional.of(statementNode);
            } else if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.COMMA)) {
                var statementNode = methodCallParser();
                return Optional.of(statementNode);
            } else {
                var statementNode = methodCallExpressionParser();
            }
        }
        return Optional.empty();
    }

    public AssignmentNode assignmentParser() throws SyntaxErrorException {
        AssignmentNode assignmentNode = new AssignmentNode();

        assignmentNode.target = variableReferenceParser();
        if(!newManager.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()) {
            throw new SyntaxErrorException("No '=' found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        assignmentNode.expression = expressionParser();

        return assignmentNode;
    }

    public ExpressionNode booleanExpTermParser() throws SyntaxErrorException {
        BooleanOpNode boolExpTermNode = new BooleanOpNode();

        var currentNode = booleanExpFactorParser();

        if(newManager.peekCurrent().get().getType() == Token.TokenTypes.AND ||
                newManager.peekCurrent().get().getType() == Token.TokenTypes.OR) {
            while (newManager.peekCurrent().get().getType() == Token.TokenTypes.AND ||
                    newManager.peekCurrent().get().getType() == Token.TokenTypes.OR) {
                if (newManager.matchAndRemove(Token.TokenTypes.AND).isPresent()) {
                    //boolExpTermNode.op = BooleanOpNode.BooleanOperations.and;
                    BooleanOpNode newNode = new BooleanOpNode();
                    newNode.left = currentNode;
                    newNode.op = BooleanOpNode.BooleanOperations.and;
                    newNode.right = booleanExpFactorParser();
                    boolExpTermNode.left = newNode;
                } else if (newManager.matchAndRemove(Token.TokenTypes.OR).isPresent()) {
                    boolExpTermNode.op = BooleanOpNode.BooleanOperations.or;
                    boolExpTermNode.right = booleanExpFactorParser();
                }
            }
            return boolExpTermNode;
        } else {
         return currentNode;
        }
    }

    public ExpressionNode booleanExpFactorParser() throws SyntaxErrorException {

        if((newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)) ||
                newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
            ExpressionNode methodCallNode = new MethodCallExpressionNode();
            methodCallNode = methodCallExpressionParser().get();
            return methodCallNode;
        }
        if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.EQUAL) || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.NOTEQUAL)
        || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LESSTHANEQUAL) || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.GREATERTHANEQUAL)
        || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.GREATERTHAN) || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LESSTHAN)
        || newManager.peekCurrent().get().getType() == Token.TokenTypes.NUMBER) {

            CompareNode expressionNode = new CompareNode();
            expressionNode.left = expressionParser();

            if(newManager.matchAndRemove(Token.TokenTypes.EQUAL).isPresent()) {
                expressionNode.op = CompareNode.CompareOperations.eq;
            }else if(newManager.matchAndRemove(Token.TokenTypes.NOTEQUAL).isPresent()) {
                expressionNode.op = CompareNode.CompareOperations.ne;
            }else if(newManager.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL).isPresent()) {
                expressionNode.op = CompareNode.CompareOperations.le;
            }else if(newManager.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL).isPresent()) {
                expressionNode.op = CompareNode.CompareOperations.ge;
            }else if(newManager.matchAndRemove(Token.TokenTypes.LESSTHAN).isPresent()) {
                expressionNode.op = CompareNode.CompareOperations.lt;
            } else if(newManager.matchAndRemove(Token.TokenTypes.GREATERTHAN).isPresent()) {
                expressionNode.op = CompareNode.CompareOperations.gt;
            }

            expressionNode.right = expressionParser();

            return expressionNode;
        }
        if(newManager.peekCurrent().get().getType() == Token.TokenTypes.WORD) {
            ExpressionNode variableReferenceNode = new CompareNode();
            variableReferenceNode = variableReferenceParser();
            return variableReferenceNode;
        }
        throw new SyntaxErrorException("Invalid expression", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
    }

    public MethodCallStatementNode methodCallParser() throws SyntaxErrorException {
        var locals = new LinkedList<VariableReferenceNode>();

        if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.COMMA)){
            locals.add(variableReferenceParser());
            while(newManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                locals.add(variableReferenceParser());
            }
            if(!newManager.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()) {
                throw new SyntaxErrorException("No '=' found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
            }
            var methodCallNode = new MethodCallStatementNode(methodCallExpressionParser().get());
            methodCallNode.returnValues = locals;
            return methodCallNode;
        }
        throw new SyntaxErrorException("Invalid expression", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
    }

    public Optional <MethodCallExpressionNode> methodCallExpressionParser() throws SyntaxErrorException {
        MethodCallExpressionNode methodCallExpressionNode = new MethodCallExpressionNode();
        if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)
        || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
            if (newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)) {
                methodCallExpressionNode.objectName = Optional.of(newManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue());
                newManager.matchAndRemove(Token.TokenTypes.DOT);
            } else {
                methodCallExpressionNode.objectName = Optional.empty();
            }
            if (newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
                methodCallExpressionNode.methodName = newManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
            }
            if (!newManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
                throw new SyntaxErrorException("Missing left parenthesis", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
            }
            if (!newManager.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()) {
                methodCallExpressionNode.parameters.add(expressionParser());
                while (newManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                    methodCallExpressionNode.parameters.add(expressionParser());
                }
                newManager.matchAndRemove(Token.TokenTypes.RPAREN);
            }
            return Optional.of(methodCallExpressionNode);
        }

        return Optional.empty();
    }

    public VariableReferenceNode variableReferenceParser() throws SyntaxErrorException {
        VariableReferenceNode variableReferenceNode = new VariableReferenceNode();
        if(newManager.peekCurrent().get().getType() == Token.TokenTypes.WORD) {
            variableReferenceNode.name = newManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
        }
        if(newManager.peekCurrent().get().getType() == Token.TokenTypes.NUMBER) {
            variableReferenceNode.name = newManager.matchAndRemove(Token.TokenTypes.NUMBER).get().getValue();
        }
        return variableReferenceNode;
    }

    public ClassNode classParser() throws SyntaxErrorException {
        ClassNode classNode = new ClassNode();
        Optional <Token> optionalToken = newManager.matchAndRemove(Token.TokenTypes.WORD);
        if (!optionalToken.isPresent()) {
            throw new SyntaxErrorException("Word not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        classNode.name = optionalToken.get().getValue();

        if(newManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent()) {
            classNode.interfaces = new ArrayList<>();
            if(newManager.peekCurrent().get().getType() == Token.TokenTypes.WORD) {
                optionalToken = newManager.matchAndRemove(Token.TokenTypes.WORD);
                classNode.interfaces.add(optionalToken.get().getValue());
                if(newManager.peekCurrent().get().getType() == Token.TokenTypes.COMMA) {
                    while (newManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                        optionalToken = newManager.matchAndRemove(Token.TokenTypes.WORD);
                        classNode.interfaces.add(optionalToken.get().getValue());
                    }
                }
            } else {
                throw new SyntaxErrorException("Word not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
            }
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
            throw new SyntaxErrorException("Newline not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
            throw new SyntaxErrorException("Indent not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        while(!(newManager.peekCurrent().get().getType() == Token.TokenTypes.DEDENT)) {
            if(newManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).isPresent()) {
                classNode.constructors.add(constructorParser());
                continue;
            }
            if(newManager.peekCurrent().get().getType() == (Token.TokenTypes.PRIVATE) || newManager.peekCurrent().get().getType() == (Token.TokenTypes.SHARED)
                    || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.LPAREN)){
                classNode.methods.add(methodDeclarationParser());
                continue;
            }
            while(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.WORD)) {
                classNode.members.add(memberParser());
                if (newManager.peekCurrent().get().getType() == Token.TokenTypes.NEWLINE) {
                    requireNewLine();
                    if (newManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
                        continue;
                    }
                }
            }
        }
        newManager.matchAndRemove(Token.TokenTypes.DEDENT);

        return classNode;
    }

    public ConstructorNode constructorParser() throws SyntaxErrorException {
        ConstructorNode constructorNode = new ConstructorNode();
        constructorNode.parameters = new ArrayList<>();

        if(!newManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            throw new SyntaxErrorException("Left parenthesis not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
            constructorNode.parameters.addAll(variableDeclarationsParser());
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()) {
            throw new SyntaxErrorException("Right parenthesis not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
            throw new SyntaxErrorException("Newline not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
            throw new SyntaxErrorException("Indent not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        while(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
            constructorNode.locals.add(variableDeclarationParser());
            if(!newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
                throw new SyntaxErrorException("Newline not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
            }
        }
        while(!newManager.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()) {
            constructorNode.statements = new ArrayList<>();
            constructorNode.statements.addAll(statementsParser());
        }
        return constructorNode;
    }
    public StatementNode statementParser() throws SyntaxErrorException {
        if(newManager.matchAndRemove(Token.TokenTypes.IF).isPresent()) {
            StatementNode statementNode = ifParser();
            return statementNode;
        }else if(newManager.matchAndRemove(Token.TokenTypes.LOOP).isPresent()) {
            StatementNode statementNode = loopParser();
            return statementNode;
        } else {
            StatementNode statementNode = disambiguate().get();
            return statementNode;
        }
    }

    public LinkedList<StatementNode> statementsParser() throws SyntaxErrorException {
        var retVal = new LinkedList<StatementNode>();

        newManager.matchAndRemove(Token.TokenTypes.INDENT);

        while(newManager.peekCurrent().get().getType() == Token.TokenTypes.IF
                || newManager.peekCurrent().get().getType() == Token.TokenTypes.LOOP
                || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)
                || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.COMMA)
                || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)) {
            retVal.add(statementParser());
            if(!(newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) && !(newManager.peekCurrent().get().getType() == Token.TokenTypes.DEDENT)) {
                throw new SyntaxErrorException("Newline not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
            }
        }
        return retVal;
    }

    public MemberNode memberParser() throws SyntaxErrorException {
        MemberNode memberNode = new MemberNode();
        if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
            memberNode.declaration = variableDeclarationParser();
            if(newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
                if (newManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
                    if (newManager.matchAndRemove(Token.TokenTypes.ACCESSOR).isPresent()) {
                        memberNode.accessor = Optional.of(statementsParser());
                        if(!newManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()) {
                            throw new SyntaxErrorException("Colon not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
                        }
                        memberNode.accessor = Optional.of(statementsParser());
                    }
                    if (newManager.matchAndRemove(Token.TokenTypes.MUTATOR).isPresent()) {
                        memberNode.mutator = Optional.of(statementsParser());
                        if(!newManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()) {
                            throw new SyntaxErrorException("Colon not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
                        }
                        memberNode.mutator = Optional.of(statementsParser());
                    }
                }
            }
        }
        return memberNode;
    }

    public MethodDeclarationNode methodDeclarationParser() throws SyntaxErrorException {
        MethodDeclarationNode methodDeclarationNode = new MethodDeclarationNode();
        MethodHeaderNode methodHeaderNode = new MethodHeaderNode();
        methodDeclarationNode.parameters = new ArrayList<>();

        if(newManager.peekCurrent().get().getType() == Token.TokenTypes.SHARED || newManager.peekCurrent().get().getType() == Token.TokenTypes.PRIVATE) {
            if(newManager.matchAndRemove(Token.TokenTypes.SHARED).isPresent()) {
                methodDeclarationNode.isShared = true;
                methodDeclarationNode.isPrivate = false;
            } else if(newManager.matchAndRemove(Token.TokenTypes.PRIVATE).isPresent()) {
                methodDeclarationNode.isPrivate = true;
                methodDeclarationNode.isShared = false;
            }
            if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
                methodHeaderNode = methodHeaderParser();

                methodDeclarationNode.name = methodHeaderNode.name;
                methodDeclarationNode.parameters.addAll(methodHeaderNode.parameters);
                methodDeclarationNode.returns.addAll(methodHeaderNode.returns);
            }
        } else {
            methodDeclarationNode.isShared = false;
            methodDeclarationNode.isPrivate = false;
        }
        if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
            methodHeaderNode = methodHeaderParser();

            methodDeclarationNode.name = methodHeaderNode.name;
            methodDeclarationNode.parameters.addAll(methodHeaderNode.parameters);
            methodDeclarationNode.returns.addAll(methodHeaderNode.returns);
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
            throw new SyntaxErrorException("Newline not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
            throw new SyntaxErrorException("Indent not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)) {
            methodDeclarationNode.statements = statementsParser();
        }
        while(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
            methodDeclarationNode.locals.add(variableDeclarationParser());
            if(!newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
                throw new SyntaxErrorException("Newline not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
            }
        }
            while(newManager.peekCurrent().get().getType() == Token.TokenTypes.IF
                    || newManager.peekCurrent().get().getType() == Token.TokenTypes.LOOP
                    || ((newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN) && newManager.peek(3).get().getType() == Token.TokenTypes.LOOP))
                    || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)
                    || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.COMMA)) {
                methodDeclarationNode.statements = new ArrayList<>();
                methodDeclarationNode.statements.addAll(statementsParser());
            }

            newManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            newManager.matchAndRemove(Token.TokenTypes.DEDENT);
        return methodDeclarationNode;
    }

    public IfNode ifParser() throws SyntaxErrorException {
        IfNode ifNode = new IfNode();
        ifNode.condition = booleanExpTermParser();

        if(!newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
            throw new SyntaxErrorException("Newline not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(newManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
            ifNode.statements= statementsParser();
            newManager.matchAndRemove(Token.TokenTypes.DEDENT);
        }
        if(newManager.matchAndRemove(Token.TokenTypes.ELSE).isPresent()) {
            if(newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
                ElseNode elseNode = new ElseNode();
                elseNode.statements = new ArrayList<>();
                elseNode.statements = statementsParser();
                ifNode.elseStatement = Optional.of(elseNode);
            }
        } else {
            ifNode.elseStatement = Optional.empty();
        }
        return ifNode;
    }
    public LoopNode loopParser() throws SyntaxErrorException {
        LoopNode loopNode = new LoopNode();
        VariableReferenceNode optionalToken = new VariableReferenceNode();

        if (newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)) {
            optionalToken.name = newManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
            loopNode.assignment = Optional.of(optionalToken);
            if (!newManager.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()) {
                throw new SyntaxErrorException("Assign not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
            }
        } else {
            loopNode.assignment = Optional.empty();
        }
        loopNode.expression = booleanExpTermParser();
        if (!newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
            throw new SyntaxErrorException("Newline not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(newManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent() || newManager.peekCurrent().get().getType() == Token.TokenTypes.IF
                    || newManager.peekCurrent().get().getType() == Token.TokenTypes.LOOP
                    || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)
                    || newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.COMMA)) {
                loopNode.statements.addAll(statementsParser());
        }
        return loopNode;
    }

    public InterfaceNode interfaceParser() throws SyntaxErrorException {
        InterfaceNode interfaceNode = new InterfaceNode();
        Optional <Token> optionalToken = newManager.matchAndRemove(Token.TokenTypes.WORD);
        if(!optionalToken.isPresent()) {
            throw new SyntaxErrorException("Word not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        interfaceNode.name = optionalToken.get().getValue();
        interfaceNode.methods = new ArrayList<>();
        if(!newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
            throw new SyntaxErrorException("Newline not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
            throw new SyntaxErrorException("Indent not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(newManager.peekCurrent().get().getType() != Token.TokenTypes.WORD){
            throw new SyntaxErrorException("Word not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        interfaceNode.methods.add(methodHeaderParser());
        while (newManager.peekCurrent().get().getType() == Token.TokenTypes.NEWLINE) {
            requireNewLine();
            if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
                interfaceNode.methods.add(methodHeaderParser());
            }
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()){
            throw new SyntaxErrorException("Dedent not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        return interfaceNode;
    }

    public MethodHeaderNode methodHeaderParser() throws SyntaxErrorException {
        MethodHeaderNode methodHeaderNode = new MethodHeaderNode();
        Optional <Token> optionalToken = newManager.matchAndRemove(Token.TokenTypes.WORD);
        if(!optionalToken.isPresent()) {
            throw new SyntaxErrorException("Word not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        methodHeaderNode.name = optionalToken.get().getValue();
        methodHeaderNode.parameters = new ArrayList<>();
        methodHeaderNode.returns = new ArrayList<>();
        if(!newManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()){
            throw new SyntaxErrorException("Left parenthesis not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
            methodHeaderNode.parameters.addAll(variableDeclarationsParser());
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()) {
            throw new SyntaxErrorException("Right parenthesis not found", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
        if(!newManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()) {
            return methodHeaderNode;
        }
        if(newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
            methodHeaderNode.returns.add(variableDeclarationParser());
            while (newManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                if (newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
                    methodHeaderNode.returns.add(variableDeclarationParser());
                } else {
                    throw new SyntaxErrorException("Missing variable declarations after comma for returns", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
                }
            }
        }
        return methodHeaderNode;
    }

    public VariableDeclarationNode variableDeclarationParser() throws SyntaxErrorException {
        VariableDeclarationNode variableDeclarationNode = new VariableDeclarationNode();
        Optional <Token> optionalToken;
        if(!newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){
            return variableDeclarationNode;
        }
        optionalToken = newManager.matchAndRemove(Token.TokenTypes.WORD);
        variableDeclarationNode.type = optionalToken.get().getValue();

        optionalToken = newManager.matchAndRemove(Token.TokenTypes.WORD);
        variableDeclarationNode.name = optionalToken.get().getValue();

        return variableDeclarationNode;
    }

    public LinkedList<VariableDeclarationNode> variableDeclarationsParser() throws SyntaxErrorException {
        var retVal = new LinkedList<VariableDeclarationNode>();
        retVal.add(variableDeclarationParser());
        while (newManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            if (newManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
                retVal.add(variableDeclarationParser());
            } else {
                throw new SyntaxErrorException("Missing variable declaration inside method declaration", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
            }
        }
        return retVal;
    }

    public void requireNewLine() throws SyntaxErrorException {
        if (newManager.peekCurrent().get().getType() == Token.TokenTypes.NEWLINE) {
            while (newManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
                if (newManager.peekCurrent().get().getType() == Token.TokenTypes.NEWLINE) {
                    newManager.matchAndRemove(Token.TokenTypes.NEWLINE);
                }
            }
        } else {
            throw new SyntaxErrorException("Missing New line", newManager.getCurrentLine(), newManager.getCurrentColumnNumber());
        }
    }
}