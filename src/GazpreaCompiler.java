import org.antlr.v4.runtime.tree.TerminalNode;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

class GazpreaCompiler extends GazpreaBaseVisitor<Object> {
    private STGroup runtimeGroup;
    private STGroup llvmGroup;

    private final String BUILT_INS[] = {"std_output", "std_input", "stream_state"};

    private Map<String, Function> functions = new HashMap<>();
    private Map<String, Variable> variables = new HashMap<>();

    private List<String> topLevelCode = new ArrayList<>();

    private Map<String, String> functionNameMappings = new HashMap<>();

    private Map<String, Type> typedefs = new HashMap<>();

    private int conditionalIndex;
    private int loopIndex;

    private ArrayList<Type> promoteType = new ArrayList<>();


    private Deque<Integer> currentLoop = new ArrayDeque<>();

    private Scope<Variable> scope = new Scope<>(); // Name mangler
    private Function currentFunction = null; // For adding code to it

    GazpreaCompiler() {
        this.runtimeGroup = new STGroupFile("./src/runtime.stg");
        this.llvmGroup = new STGroupFile("./src/llvm.stg");

        this.functionNameMappings.put("std_output", "_Z10std_outputv");
        this.functionNameMappings.put("std_input", "_Z9std_inputv");
        this.functionNameMappings.put("stream_state", "_Z12stream_statev");

        loopIndex = 0;
        conditionalIndex = 0;
    }

    @Override
    public Object visitCompilationUnit(GazpreaParser.CompilationUnitContext ctx) {
        this.scope.pushScope();
        ctx.topLevelCode().forEach(this::visitTopLevelCode);
        this.scope.popScope();

        List<String> globalVariables = this.variables
                .entrySet()
                .stream()
                .map(entry -> {
                    ST varLine = this.llvmGroup.getInstanceOf("globalVariable");
                    varLine.add("name", entry.getValue().getMangledName());
                    return varLine.render();
                })
                .collect(Collectors.toList());

        List<String> functionIR = this.functions
                .entrySet()
                .stream()
                .map(entry -> {
                    ST functionLines = this.llvmGroup.getInstanceOf("function");
                    functionLines.add("name", entry.getKey());
                    functionLines.add("code", entry.getValue().render());
                    return functionLines.render();
                })
                .collect(Collectors.toList());

        ST program = this.runtimeGroup.getInstanceOf("runtime");
        program.add("variables", globalVariables);
        program.add("functions", functionIR);
        program.add("code", this.topLevelCode);
        program.add("structs", "");
        String code = program.render();

        System.out.println(code);
        try {
            PrintWriter writer = new PrintWriter("program.s", "UTF-8");
            writer.println(code);
            writer.close();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public Object visitFunction(GazpreaParser.FunctionContext ctx) {
        this.createFunctionOrProcedure(
                ctx.Identifier().getText(),
                false,
                ctx.argumentList(),
                ctx.returnType(),
                ctx.functionBlock()
        );
        return null;
    }

    @Override
    public Object visitProcedure(GazpreaParser.ProcedureContext ctx) {
        this.createFunctionOrProcedure(
                ctx.Identifier().getText(),
                true,
                ctx.argumentList(),
                ctx.returnType(),
                ctx.functionBlock()
        );
        return null;
    }

    private void createFunctionOrProcedure(String name, boolean isProcedure, GazpreaParser.ArgumentListContext argumentListContext, GazpreaParser.ReturnTypeContext returnTypeContext, GazpreaParser.FunctionBlockContext functionBlockContext) {
        if (this.functions.get(name) != null) {
            if (this.functions.get(name).isDefined() || functionBlockContext == null) {
                System.err.println("Illegal duplicate definition of function " + name);
                System.exit(1);
            }
        }

        this.scope.pushScope();

        List<Argument> argumentList = this.visitArgumentList(argumentListContext);
        Type returnType = this.visitReturnType(returnTypeContext);

        this.currentFunction = new Function(name, null, returnType);
        if (isProcedure) { this.currentFunction.setProcedure(); }

        for (int arg = 0; arg < argumentListContext.argument().size(); ++arg) {
            Argument argument = this.visitArgument(argumentListContext.argument(arg));

            Variable var = new Variable(argument.getName(), this.mangleVariableName(argument.getName()), argument.getType());

            this.scope.initVariable(argument.getName(), var);

            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.addCode(varLine.render());

            if (argument.getType().getType() != Type.TYPES.TUPLE) {
                ST initLine = this.llvmGroup.getInstanceOf("varInit_" + argument.getType().getTypeLLVMString());
                this.addCode(initLine.render());
            }

            ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
            initAssign.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.addCode(initAssign.render());

            ST varAssign = this.llvmGroup.getInstanceOf("assignVariable");
            varAssign.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.addCode(varAssign.render());
        }

        this.currentFunction.setArguments(argumentList);

        if (functionBlockContext != null) {
            this.visitFunctionBlock(functionBlockContext);
        }

        this.scope.popScope();

        this.functions.put(name, this.currentFunction);
        this.currentFunction = null;
        this.functionNameMappings.put(name, "GazFunc_" + name);
    }

    @Override
    public List<Argument> visitArgumentList(GazpreaParser.ArgumentListContext ctx) {
        return ctx.argument()
                .stream()
                .map(this::visitArgument)
                .collect(Collectors.toList());
    }

    @Override
    public Argument visitArgument(GazpreaParser.ArgumentContext ctx) {
        return new Argument(this.visitType(ctx.type()), ctx.Identifier().getText());
    }

    @Override
    public Type visitReturnType(GazpreaParser.ReturnTypeContext ctx) {
        if (ctx != null && ctx.type() != null) {
            return visitType(ctx.type());
        } else {
            return new Type(null, Type.TYPES.VOID);
        }
    }

    @Override
    public Object visitFunctionBlock(GazpreaParser.FunctionBlockContext ctx) {
        this.currentFunction.define();
        if (ctx.block() != null) {
            this.visitBlock(ctx.block());

            this.currentFunction.getArguments().forEach(argument -> {
                if (argument.getType().getSpecifier().equals(Type.SPECIFIERS.VAR)) {
                    ST push = this.llvmGroup.getInstanceOf("pushVariableValue");
                    push.add("name", this.scope.getVariable(argument.getName()).getMangledName());
                    this.addCode(push.render());
                }
            });

            ST line = this.llvmGroup.getInstanceOf("functionReturn");
            this.addCode(line.render());
        }
        if (ctx.expression() != null) {
            this.visitExpression(ctx.expression());
            ST line = this.llvmGroup.getInstanceOf("functionReturn");
            this.addCode(line.render());
        }
        return null;
    }

    @Override
    public Object visitBlock(GazpreaParser.BlockContext ctx) {
        this.scope.pushScope();
        for (int i = 0; i < ctx.translationalUnit().size(); ++i) {
            this.visitTranslationalUnit(ctx.translationalUnit(i));
        }
        this.scope.popScope();

        return null;
    }

    @Override
    public Object visitStreamStatement(GazpreaParser.StreamStatementContext ctx) {
        ctx.expression().forEach(this::visitExpression);
        if (ctx.LeftArrow() != null) {
            ST line = this.llvmGroup.getInstanceOf("leftArrowOperator");
            this.addCode(line.render());
        }
        if (ctx.RightArrow() != null) {
            ST line = this.llvmGroup.getInstanceOf("rightArrowOperator");
            this.addCode(line.render());
        }
        return null;
    }

    private String parseTupleAccessREAL(String real){
        return real.substring(real.indexOf('.') + 1);
    }

    @Override
    public Type visitExpression(GazpreaParser.ExpressionContext ctx) {
        if (ctx.expression() != null && ctx.Dot()!= null && ctx.RealLiteral() != null && ctx.getChild(0) == ctx.expression()){
            // CASE: expression Dot RealLiteral
            // Interval

            ST startVector = this.llvmGroup.getInstanceOf("startVector");
            this.addCode(startVector.render());

            String rightInt = ctx.RealLiteral(0).getText().replaceAll("\\.","");
            ST right = this.llvmGroup.getInstanceOf("pushInteger");
            right.add("value", rightInt.replaceAll("_", ""));
            this.addCode(right.render());

            // assume expression will be pushed to stack
            Type left = this.visitExpression(ctx.expression(0));

            if (!(left.getType().equals(Type.TYPES.INTEGER))){
                throw new Error("Types must be integer in interval");
            }

            ST endInterval = this.llvmGroup.getInstanceOf("endInterval");
            this.addCode(endInterval.render());

            return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
        }
        else if (ctx.RealLiteral().size() == 2){
            // CASE: RealLiteral RealLiteral
            // Interval

            String leftInt = ctx.RealLiteral(0).getText().replaceAll("\\.","");
            String rightInt = ctx.RealLiteral(1).getText().replaceAll("\\.","");

            ST startVector = this.llvmGroup.getInstanceOf("startVector");
            this.addCode(startVector.render());

            // since we can't visit literal, push left and right to stack here
            ST right = this.llvmGroup.getInstanceOf("pushInteger");
            right.add("value", rightInt.replaceAll("_", ""));
            this.addCode(right.render());

            ST left = this.llvmGroup.getInstanceOf("pushInteger");
            left.add("value", leftInt.replaceAll("_", ""));
            this.addCode(left.render());

            ST endInterval = this.llvmGroup.getInstanceOf("endInterval");
            this.addCode(endInterval.render());

            return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
        }
        else if (ctx.expression() != null && ctx.Dot()!= null && ctx.RealLiteral() != null && ctx.getChild(2) == ctx.expression()){
            // CASE: RealLiteral Dot expression
            // Interval

            ST startVector = this.llvmGroup.getInstanceOf("startVector");
            this.addCode(startVector.render());

            // assume expression will be pushed to stack
            Type right = this.visitExpression(ctx.expression(0));

            if (!(right.getType().equals(Type.TYPES.INTEGER))){
                throw new Error("Types must be integer in interval");
            }

            String leftInt = ctx.RealLiteral(0).getText().replaceAll("\\.","");
            ST left = this.llvmGroup.getInstanceOf("pushInteger");
            left.add("value", leftInt.replaceAll("_", ""));
            this.addCode(left.render());

            ST endInterval = this.llvmGroup.getInstanceOf("endInterval");
            this.addCode(endInterval.render());

            return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
        }
        else if (ctx.expression().size()== 2 && ctx.Dot().size() == 2){
            // CASE: expression Dot Dot expression
            // Interval

            ST startVector = this.llvmGroup.getInstanceOf("startVector");
            this.addCode(startVector.render());

            // assume expression will be pushed to stack
            Type right = this.visitExpression(ctx.expression(1));
            Type left = this.visitExpression(ctx.expression(0));

            if (!(left.getType().equals(Type.TYPES.INTEGER)) && !(right.getType().equals(Type.TYPES.INTEGER))){
                throw new Error("Types must be integer in interval");
            }

            ST endInterval = this.llvmGroup.getInstanceOf("endInterval");
            this.addCode(endInterval.render());

            return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
        }
        else if (ctx.Dot() != null && ctx.Dot().size() != 0 || ctx.RealLiteral() != null && ctx.RealLiteral().size() != 0) {
            // first get the tuple on the stack
            Type type = this.visitExpression(ctx.expression(0));

            // then get the field respective to the tuple on the stack
            String field;

            if (ctx.RealLiteral() != null && ctx.RealLiteral().size() > 0) {
                field = parseTupleAccessREAL(ctx.RealLiteral(0).getText());
            } else {
                field = ctx.Identifier().getText();
            }

            Tuple tupleType = type.getTupleType();
            Integer fieldNumber = tupleType.getFieldNumber(field);

            ST getTupleField = this.llvmGroup.getInstanceOf("getAt");
            getTupleField.add("index", fieldNumber - 1);
            this.addCode(getTupleField.render());

            return tupleType.getTypeOfField(fieldNumber);
        }
        else if (ctx.Identifier() != null) {
            // TODO: This should unwrap the variable and put it on the stack and return the type
            ST line = this.llvmGroup.getInstanceOf("pushVariable");
            Variable variable = this.scope.getVariable(ctx.Identifier().getText());
            line.add("name", variable.getMangledName());
            this.addCode(line.render());

            return this.scope.getVariable(ctx.Identifier().getText()).getType();
        }
        else if (ctx.literal() != null) {
            return this.visitLiteral(ctx.literal());
        }
        else if (ctx.expression() != null && ctx.expression().size() == 1) {
            // CASE: where there is only one expression in the expression statement
            Type type = this.visitExpression(ctx.expression(0));
            if (ctx.As() == null && ctx.op != null && ctx.op.getText().equals("-")) {
                if (type.getType() == Type.TYPES.INTERVAL){
                    ST operatorCall = this.llvmGroup.getInstanceOf("negInterval");
                    this.addCode(operatorCall.render());
                } else if (type.getType().equals(Type.TYPES.INTEGER)) {
                    ST negation = this.llvmGroup.getInstanceOf("negation");
                    negation.add("typeLetter", "iv");
                    this.addCode(negation.render());
                } else {
                    ST negation = this.llvmGroup.getInstanceOf("negation");
                    negation.add("typeLetter", "rv");
                    this.addCode(negation.render());
                }
                return type;
            }
            else if (ctx.As() == null && ctx.op != null && ctx.op.getText().equals("+")) {
                return type;
            }
            else if (ctx.As() == null && ctx.op != null && ctx.op.getText().equals("not")) {
                ST negation = this.llvmGroup.getInstanceOf("negation");
                negation.add("typeLetter", "bv");
                this.addCode(negation.render());
                return type;
            }
            else if (ctx.As() == null) {
                // CASE: parenthesis
                return type;
            } else {
                // CASE: casting case
                Type newType;
                if (ctx.type() != null) {
                    newType = this.visitType(ctx.type());
                    String typeLetter = Type.getCastingFunction(type, newType);
                    ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
                    promoteCall.add("typeLetter", typeLetter);
                    this.addCode(promoteCall.render());

                    return newType;
                }
                // TODO tuple case
                Tuple tupleType = new Tuple();

                ST startVector = this.llvmGroup.getInstanceOf("startVector");
                this.addCode(startVector.render());
                for (int e = 0; e < ctx.tupleTypeDetails().tupleTypeAtom().size(); ++e){
                    Type exprType = visitType(visitTupleTypeAtom(ctx.tupleTypeDetails().tupleTypeAtom().get(e)).left());
                    String typeLetter = Type.getCastingFunction(promoteType.get(e), exprType);
                    visitExpression(ctx.expression(0).literal().tupleLiteral().expression(e));
                    ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
                    promoteCall.add("typeLetter", typeLetter);
                    this.addCode(promoteCall.render());
                    exprType = Type.getReturnType(typeLetter);
                    tupleType.addField("" + (e+1), exprType);
                }
                ST endTuple = this.llvmGroup.getInstanceOf("endTuple");
                this.addCode(endTuple.render());
                return new Type(Type.SPECIFIERS.VAR, Type.TYPES.TUPLE, tupleType);

            }
        }
        else if (ctx.generator() != null) {
            // TODO
        }
        else if (ctx.filter() != null) {
            // TODO
        }
        else if (ctx.functionCall() != null) {
            return this.visitFunctionCall(ctx.functionCall());
        }
        else if (ctx.expression()!= null && ctx.expression().size() == 2 && ctx.op.getText() == ".."){
            ST startVector = this.llvmGroup.getInstanceOf("startVector");
            this.addCode(startVector.render());

            // assume expression will be pushed to stack
            Type right =  this.visitExpression(ctx.expression(1));
            Type left = this.visitExpression(ctx.expression(0));

            if (!(left.getType().equals(Type.TYPES.INTEGER)) && !(right.getType().equals(Type.TYPES.INTEGER))){
                throw new Error("Types must be ineger in interval");
            }

            ST endInterval = this.llvmGroup.getInstanceOf("endInterval");
            this.addCode(endInterval.render());

            return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
        }
        else if (ctx.expression() != null && ctx.expression().size() == 2) {
            // CASE: where there is two expressions in the expression statement
            Type left = (Type)visit(ctx.expression(0));
            Type right = (Type)visit(ctx.expression(1));
            String typeLetter = Type.getResultFunction(left, right);

            if (left.getCollection_type()== null && right.getCollection_type()==null) {
                for (int i = 0; i < 2; i++) {
                    ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
                    promoteCall.add("typeLetter", typeLetter);
                    this.addCode(promoteCall.render());
                    ST swapStack = this.llvmGroup.getInstanceOf("swapStack");
                    this.addCode(swapStack.render());
                }
            }

            String operator = ctx.op.getText();
            if (operator == null){
                // TODO INDEXING OR MATRIX
                return null;
            }
            ST operatorCall;
            switch(operator) {
                // CASE: concat
                case "||":
                    // TODO
                return null;
                // CASE: OR
                case "or":
                    operatorCall = this.llvmGroup.getInstanceOf("logicalor");
                    operatorCall.add("typeLetter", typeLetter);
                    this.addCode(operatorCall.render());
                    return Type.getReturnType("bv");
                // CASE: XOR
                case "xor":
                    operatorCall = this.llvmGroup.getInstanceOf("logicalxor");
                    operatorCall.add("typeLetter", typeLetter);
                    this.addCode(operatorCall.render());
                    return Type.getReturnType("bv");

                // CASE: AND
                case "and":
                    operatorCall = this.llvmGroup.getInstanceOf("logicaland");
                    operatorCall.add("typeLetter", typeLetter);
                    this.addCode(operatorCall.render());
                    return Type.getReturnType("bv");

                // CASE: ==
                case "==":
                    // TODO TUPLE
                    // Interval case
                    if (right.getType() == Type.TYPES.INTERVAL && left.getType() == Type.TYPES.INTERVAL){
                        operatorCall = this.llvmGroup.getInstanceOf("equalInterval");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.BOOLEAN);
                    }
                    else {
                        operatorCall = this.llvmGroup.getInstanceOf("equal");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType("bv");
                    }

                // CASE: !=
                case "!=":
                    // TODO TUPLE
                    // Interval case
                    if (right.getType() == Type.TYPES.INTERVAL && left.getType() == Type.TYPES.INTERVAL){
                        operatorCall = this.llvmGroup.getInstanceOf("notequalInterval");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.BOOLEAN);
                    }
                    else {
                        operatorCall = this.llvmGroup.getInstanceOf("notequal");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType("bv");
                    }

                // CASE: <
                case "<":
                    operatorCall = this.llvmGroup.getInstanceOf("lessthan");
                    operatorCall.add("typeLetter", typeLetter);
                    this.addCode(operatorCall.render());
                    return Type.getReturnType("bv");

                // CASE: <=
                case "<=":
                    operatorCall = this.llvmGroup.getInstanceOf("lessthanequal");
                    operatorCall.add("typeLetter", typeLetter);
                    this.addCode(operatorCall.render());
                    return Type.getReturnType("bv");

                // CASE: >
                case ">":
                    operatorCall = this.llvmGroup.getInstanceOf("greaterthan");
                    operatorCall.add("typeLetter", typeLetter);
                    this.addCode(operatorCall.render());
                    return Type.getReturnType("bv");

                // CASE: >=
                case ">=":
                    operatorCall = this.llvmGroup.getInstanceOf("greaterthanequal");
                    operatorCall.add("typeLetter", typeLetter);
                    this.addCode(operatorCall.render());
                    return Type.getReturnType("bv");

                // CASE: by
                case "by":
                    // TODO
                    String offset = ctx.expression(1).getText();
                    if (Integer.getInteger(offset) <= 0){
                        throw new Error("Offset is lower than 1");
                    }
                    operatorCall = this.llvmGroup.getInstanceOf("byInterval");
                    operatorCall.add("value", offset);
                    this.addCode(operatorCall.render());
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTEGER, Type.COLLECTION_TYPES.VECTOR);
                // CASE: +
                case "+":
                    // Interval case
                    if (right.getType() == Type.TYPES.INTERVAL && left.getType() == Type.TYPES.INTERVAL){
                        operatorCall = this.llvmGroup.getInstanceOf("addInterval");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
                    }
                    else {
                        operatorCall = this.llvmGroup.getInstanceOf("addition");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter);
                    }
                // CASE: -
                case "-":
                    if (right.getType() == Type.TYPES.INTERVAL && left.getType() == Type.TYPES.INTERVAL){
                        operatorCall = this.llvmGroup.getInstanceOf("subInterval");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
                    }
                    else {
                        operatorCall = this.llvmGroup.getInstanceOf("subtraction");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter);
                    }
                // CASE: dotproduct
                case "**":
                    // TODO
                    if (!(left.getCollection_type().equals(Type.COLLECTION_TYPES.VECTOR)) && !(right.getCollection_type().equals(Type.COLLECTION_TYPES.VECTOR))){
                        throw new Error("Types must be vectors");
                    }
                return null;
                // CASE: *
                case "*":
                    // Interval case
                    if (right.getType() == Type.TYPES.INTERVAL && left.getType() == Type.TYPES.INTERVAL){
                        operatorCall = this.llvmGroup.getInstanceOf("multInterval");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
                    }
                    else {
                        operatorCall = this.llvmGroup.getInstanceOf("multiplication");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter);
                    }
                // CASE: /
                case "/":
                    // Interval case
                    if (right.getType() == Type.TYPES.INTERVAL && left.getType() == Type.TYPES.INTERVAL){
                        operatorCall = this.llvmGroup.getInstanceOf("divInterval");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
                    }
                    else {
                        operatorCall = this.llvmGroup.getInstanceOf("division");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter);
                    }
                // CASE: %
                case "%":
                    operatorCall = this.llvmGroup.getInstanceOf("modulus");
                    operatorCall.add("typeLetter", typeLetter);
                    this.addCode(operatorCall.render());
                    return Type.getReturnType(typeLetter);
                // CASE: ^
                case "^":
                    operatorCall = this.llvmGroup.getInstanceOf("exponentiation");
                    operatorCall.add("typeLetter", typeLetter);
                    this.addCode(operatorCall.render());
                    return Type.getReturnType(typeLetter);
            }
        }
        return null;
    }

    @Override
    // pushes the literal onto the stack and returns the type of the literal
    public Type visitLiteral(GazpreaParser.LiteralContext ctx) {
        ST line = null;
        Type.TYPES retType = null;
        Type.COLLECTION_TYPES retCollectionType = null;
        if (ctx.NullLiteral() != null) {
            line = this.llvmGroup.getInstanceOf("pushNull");
            retType = Type.TYPES.NULL;
        } else if (ctx.IdentityLiteral() != null) {
            line = this.llvmGroup.getInstanceOf("pushIdentity");
            retType = Type.TYPES.IDENTITY;
        } else if (ctx.IntegerLiteral() != null) {
            line = this.llvmGroup.getInstanceOf("pushInteger");
            line.add("value", ctx.getText().replaceAll("_", ""));
            retType = Type.TYPES.INTEGER;
        } else if (ctx.BooleanLiteral() != null) {
            line = this.llvmGroup.getInstanceOf("pushBoolean");
            line.add("value", ctx.getText());
            retType = Type.TYPES.BOOLEAN;
        } else if (ctx.CharacterLiteral() != null) {
            line = this.llvmGroup.getInstanceOf("pushCharacter");
            String character = ctx.getText().substring(1, ctx.getText().length()-1);
            char val = 0;
            switch (character) {
                case "\\a":
                    val = 7;
                    break;
                case "\\b":
                    val = '\b';
                    break;
                case "\\n":
                    val = '\n';
                    break;
                case "\\r":
                    val = '\r';
                    break;
                case "\\t":
                    val = '\t';
                    break;
                case "\\\\":
                    val = '\\';
                    break;
                case "\\'":
                    val = '\'';
                    break;
                case "\\\"":
                    val = '\"';
                    break;
                case "\\0":
                    val = '\0';
                    break;
                default:
                    val = character.charAt(0);
                    break;
            }
            line.add("value", (int) val);
            retType = Type.TYPES.CHARACTER;
        } else if (ctx.StringLiteral() != null) {
            // TODO
            retType = Type.TYPES.STRING;
        } else if (ctx.RealLiteral() != null) {
            line = this.llvmGroup.getInstanceOf("pushReal");
            float val = Float.parseFloat(ctx.getText().replaceAll("_", ""));
            String hex_val = Long.toHexString(Double.doubleToLongBits(val));
            line.add("value", "0x" + hex_val.toUpperCase());
            retType = Type.TYPES.REAL;
        } else if (ctx.tupleLiteral() != null) {
            return this.visitTupleLiteral(ctx.tupleLiteral());
        } else if (ctx.vectorLiteral() != null) {
            return this.visitVectorLiteral(ctx.vectorLiteral());
        }
        if (line != null) {
            this.addCode(line.render());
        }

        return new Type(Type.SPECIFIERS.VAR, retType, retCollectionType);
    }

    @Override
    public Type visitTupleLiteral(GazpreaParser.TupleLiteralContext ctx) {
        Tuple tupleType = new Tuple();

        ST startVector = this.llvmGroup.getInstanceOf("startVector");
        this.addCode(startVector.render());
        for (int e = 0; e < ctx.expression().size(); ++e) {
            Type exprType = this.visitExpression(ctx.expression(e));
            tupleType.addField("" + (e+1), exprType);
        }
        ST endTuple = this.llvmGroup.getInstanceOf("endTuple");
        this.addCode(endTuple.render());

        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.TUPLE, tupleType);
    }

    @Override
    public Type visitVectorLiteral(GazpreaParser.VectorLiteralContext ctx) {
        Type.TYPES type = Type.TYPES.NULL;
        Integer size = ctx.expression().size();

        ST startVector = this.llvmGroup.getInstanceOf("startVector");
        this.addCode(startVector.render());

        // we are only concerned about implicitly promotable types i.e. integers, reals, nulls, identity
        // NOTE: a vector of nulls and identities will be returned as TYPES.NULL
        HashMap<Type.TYPES, Integer> elementTypeCounts = new HashMap<>();
        elementTypeCounts.put(Type.TYPES.REAL, 0);
        elementTypeCounts.put(Type.TYPES.INTEGER, 0);

        ArrayList<Type> typesOfElements = new ArrayList<>();

        for (int expr = 0; expr < ctx.expression().size(); ++expr) {
            Type exprType = this.visitExpression(ctx.expression(expr));
            if (exprType.getType() != Type.TYPES.REAL
                    && exprType.getType() != Type.TYPES.INTEGER
                    && exprType.getType() != Type.TYPES.IDENTITY
                    && exprType.getType() != Type.TYPES.NULL) {
                type = exprType.getType();
            } else if (exprType.getType() == Type.TYPES.INTEGER) {
                elementTypeCounts.put(Type.TYPES.INTEGER, elementTypeCounts.get(Type.TYPES.INTEGER) + 1);
            } else if (exprType.getType() == Type.TYPES.REAL) {
                elementTypeCounts.put(Type.TYPES.REAL, elementTypeCounts.get(Type.TYPES.INTEGER) + 1);
            }
        }
        ST endVector = this.llvmGroup.getInstanceOf("endVector");
        this.addCode(endVector.render());

        // process the implicit promotion possibilities
        if (elementTypeCounts.get(Type.TYPES.INTEGER) > 0 && elementTypeCounts.get(Type.TYPES.REAL) > 0) {
            type = Type.TYPES.REAL;
        }

        // now we cast the vector!
        // TODO: CAST THE VECTOR

        return new Type(Type.SPECIFIERS.VAR, type, Type.COLLECTION_TYPES.VECTOR, size);
    }

    @Override
    public Object visitReturnStatement(GazpreaParser.ReturnStatementContext ctx) {

        this.currentFunction.getArguments().forEach(argument -> {
            if (argument.getType().getSpecifier().equals(Type.SPECIFIERS.VAR)) {
                ST push = this.llvmGroup.getInstanceOf("pushVariableValue");
                push.add("name", this.scope.getVariable(argument.getName()).getMangledName());
                this.addCode(push.render());
            }
        });

        if (ctx.expression() != null) {
            this.visitExpression(ctx.expression());
        }

        if (this.currentFunction.getReturnType().getType() != Type.TYPES.NULL
            && this.currentFunction.getReturnType().getType() != Type.TYPES.VOID) {
            ST unwrap = this.llvmGroup.getInstanceOf("unwrap");
            this.addCode(unwrap.render());
        }

        ST line = this.llvmGroup.getInstanceOf("functionReturn");
        this.addCode(line.render());

        return null;
    }

    @Override
    public Object visitAssignment(GazpreaParser.AssignmentContext ctx) {
        // tuple asssignment vs. regular assignment
        if (ctx.RealLiteral() != null || ctx.Dot() != null) {
            String varName = ctx.Identifier(0).getText();
            String field;

            if (ctx.RealLiteral() != null) {
                field = parseTupleAccessREAL(ctx.RealLiteral().getText());
            } else {
                field = ctx.Identifier(1).getText();
            }
/*
            // first get the tuple on the stack
            ST line = this.llvmGroup.getInstanceOf("pushVariable");
            Variable variable = this.scope.getVariable(varName);
            line.add("name", variable.getMangledName());
            this.addCode(line.render());

            // then get the field respective to the tuple on the stack
            Tuple tupleType = variable.getType().getTupleType();
            Integer fieldNumber = tupleType.getFieldNumber(field);

            ST getTupleField = this.llvmGroup.getInstanceOf("getAt");
            getTupleField.add("index", fieldNumber - 1);
            this.addCode(getTupleField.render());
*/
            // TODO fix this tuple assignment
            // first get the tuple on the stack
            ST line = this.llvmGroup.getInstanceOf("pushVariable");
            Variable variable = this.scope.getVariable(varName);
            line.add("name", variable.getMangledName());
            this.addCode(line.render());

            // then get the field respective to the tuple on the stack
            Tuple tupleType = variable.getType().getTupleType();
            Integer fieldNumber = tupleType.getFieldNumber(field);

            Type visitType = this.visitExpression(ctx.expression()); // push assigning value to stack

            ST assignTupleField = this.llvmGroup.getInstanceOf("assignTupleField");
            assignTupleField.add("index", fieldNumber - 1);
            this.addCode(assignTupleField.render());

            ST assign = this.llvmGroup.getInstanceOf("assignByVar");
            assign.add("name", variable.getMangledName());
            this.addCode(assign.render());


        } else {
            // TODO check to see if assigning type is valid
            Type visitType = this.visitExpression(ctx.expression());

            if (visitType.getType().equals(Type.TYPES.TUPLE) && ctx.Identifier().size() > 1){
                ST getAt = this.llvmGroup.getInstanceOf("getAt2");
                this.addCode(getAt.render());
                for (int i = ctx.Identifier().size() - 1; i >= 0; i-- ){
                    ST assign = this.llvmGroup.getInstanceOf("assignByVar");
                    assign.add("name", this.scope.getVariable(ctx.Identifier(i).getText()).getMangledName());
                    this.addCode(assign.render());
                }
                return null;
            }

            ST assign = this.llvmGroup.getInstanceOf("assignByVar");
            assign.add("name", this.scope.getVariable(ctx.Identifier(0).getText()).getMangledName());
            this.addCode(assign.render());
        }
        return null;
    }

    @Override
    public Type visitFunctionCall(GazpreaParser.FunctionCallContext ctx) {
        List<GazpreaParser.ExpressionContext> arguments = ctx.expression();
        Collections.reverse(arguments);
        arguments.forEach(this::visitExpression);

        String functionName = this.visitFunctionName(ctx.functionName());

        ST functionCall = this.llvmGroup.getInstanceOf("functionCall");

        String mangledFunctionName = this.functionNameMappings.get(functionName);
        functionCall.add("name", mangledFunctionName);
        this.addCode(functionCall.render());

        if (!Arrays.asList(BUILT_INS).contains(functionName)) {
            // For non built in functions
            Function function = this.functions.get(functionName);
            return function.getReturnType();
        } else {
            // For built in functions
            switch(functionName) {
                case "std_output":
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.OUTPUT_STREAM);
                case "std_input":
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INPUT_STREAM);
                case "stream_status":
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTEGER);
                default:
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.VOID);
            }
        }
    }

    @Override
    public Object visitProcedureCall(GazpreaParser.ProcedureCallContext ctx) {
        this.visitFunctionCall(ctx.functionCall());

        String functionName = this.visitFunctionName(ctx.functionCall().functionName());
        Function function = this.functions.get(functionName);
        List<Argument> arguments = function.getArguments();
        List<String> varNames = ctx
                .functionCall()
                .expression()
                .stream()
                .map(this::getVariableFromExpression)
                .collect(Collectors.toList());
        Collections.reverse(arguments);
        Collections.reverse(varNames);

        zip.zip(arguments, varNames, null, null).forEach(pair -> {
            if (pair.left().getType().getSpecifier().equals(Type.SPECIFIERS.VAR)) {
                ST postCallAssign = this.llvmGroup.getInstanceOf("assignVariable");
                postCallAssign.add("name", this.scope.getVariable(pair.right()).getMangledName());
                this.addCode(postCallAssign.render());
            }
        });

        return null;
    }

    @Override
    public Object visitConditional(GazpreaParser.ConditionalContext ctx) {;
        // If
        this.visitExpression(ctx.expression());

        ++this.conditionalIndex;
        int myConditionalIndex = this.conditionalIndex;

        ST startConditional = this.llvmGroup.getInstanceOf("conditionalStart");
        startConditional.add("index", myConditionalIndex);
        this.addCode(startConditional.render());

        this.visitTranslationalUnit(ctx.translationalUnit(0));

        ST endConditional = this.llvmGroup.getInstanceOf("conditionalEnd");
        endConditional.add("index", myConditionalIndex);
        this.addCode(endConditional.render());

        // Else

        if (ctx.Else() != null) {
            this.visitExpression(ctx.expression());
            ST NOTop = this.llvmGroup.getInstanceOf("negation");
            NOTop.add("typeLetter", "bv");
            this.currentFunction.addLine(NOTop.render());

            ++this.conditionalIndex;
            myConditionalIndex = this.conditionalIndex;

            startConditional = this.llvmGroup.getInstanceOf("conditionalStart");
            startConditional.add("index", myConditionalIndex);
            this.currentFunction.addLine(startConditional.render());

            this.visitTranslationalUnit(ctx.translationalUnit(1));

            endConditional = this.llvmGroup.getInstanceOf("conditionalEnd");
            endConditional.add("index", myConditionalIndex);
            this.currentFunction.addLine(endConditional.render());
        }

        return null;
    }

    private String getVariableFromExpression(GazpreaParser.ExpressionContext ctx) {
        if (ctx.expression() != null && ctx.expression().size() == 1) {
            return getVariableFromExpression(ctx.expression(0));
        }
        if (ctx.Identifier() != null) {
            return ctx.Identifier().getText();
        }
        return "";
    }

    @Override public Type visitInfiniteLoop(GazpreaParser.InfiniteLoopContext ctx) {
        ++this.loopIndex;

        int myLoopIndex = this.loopIndex;

        currentLoop.addFirst(myLoopIndex);

        ST startInfiniteLoop = this.llvmGroup.getInstanceOf("loopStart");
        startInfiniteLoop.add("index", currentLoop.peekFirst());
        this.currentFunction.addLine(startInfiniteLoop.render());

        this.visitTranslationalUnit(ctx.translationalUnit());

        ST endInfiniteLoop = this.llvmGroup.getInstanceOf("loopEnd");
        endInfiniteLoop.add("index", currentLoop.peekFirst());
        this.currentFunction.addLine(endInfiniteLoop.render());

        currentLoop.removeFirst();

        return null;
    }

    private Deque<Integer> currentIterator = new ArrayDeque<>();
    @Override public Type visitIteratorLoop(GazpreaParser.IteratorLoopContext ctx) {
        Integer loopSize = ctx.iteratorLoopVariables().iteratorLoopVariable().size();

        this.scope.pushScope();
        // initialize id's of iterators
        for (int i = 0; i < loopSize; i++){
            String variableName = ctx.iteratorLoopVariables().iteratorLoopVariable(i).Identifier().getText();

            Variable variable = new Variable(variableName, this.mangleVariableName(variableName), new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTEGER));

            if (this.currentFunction != null) {
                ST varLine = this.llvmGroup.getInstanceOf("localVariable");
                varLine.add("name", variable.getMangledName());
                this.addCode(varLine.render());
            } else {
                this.variables.put(variableName, variable);
            }

            this.scope.initVariable(variableName, variable);

            ST initLine = this.llvmGroup.getInstanceOf("varInit_" + variable.getType().getTypeLLVMString());
            this.currentFunction.addLine((initLine.render()));

        }

        // go through everything except for innermost
        for (int i = 0; i < loopSize - 1; i++){
            Integer loopIndex = this.visitIteratorLoopVariable(ctx.iteratorLoopVariables().iteratorLoopVariable(loopSize - 1));
            currentIterator.addFirst(loopIndex);
        }

        // render innermost loop code
        Integer loopIndex = this.visitIteratorLoopVariable(ctx.iteratorLoopVariables().iteratorLoopVariable(loopSize - 1));

        this.visitTranslationalUnit(ctx.translationalUnit());

        ST endLoop = this.llvmGroup.getInstanceOf("loopEnd");
        endLoop.add("index", loopIndex);
        this.currentFunction.addLine(endLoop.render());

        ST loopConditionalEnd = this.llvmGroup.getInstanceOf("loopConditionalEnd");
        loopConditionalEnd.add("index", loopIndex);
        this.currentFunction.addLine(loopConditionalEnd.render());

        this.scope.popScope();
        // go through everything except for innermost
        for (int i = 0; i < loopSize - 1; i++){
            Integer index = currentIterator.removeFirst();
            ST endLoop1 = this.llvmGroup.getInstanceOf("loopEnd");
            endLoop1.add("index", index);
            this.currentFunction.addLine(endLoop1.render());

            ST loopConditionalEnd1 = this.llvmGroup.getInstanceOf("loopConditionalEnd");
            loopConditionalEnd1.add("index", index);
            this.currentFunction.addLine(loopConditionalEnd1.render());
        }

        currentIterator.clear();

        return null;

    }

    @Override public Integer visitIteratorLoopVariable(GazpreaParser.IteratorLoopVariableContext ctx) {
        ++this.loopIndex;

        int myLoopIndex = this.loopIndex;

        // do declaration here
        String variableName = ctx.Identifier().getText();
        //System.out.println(this.scope.getVariable(variableName).getMangledName());

        // initialize as integer type but could be cast to whatever is inside vector
        Variable variable = new Variable(variableName, this.mangleVariableName(variableName), new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTEGER));

        ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
        initAssign.add("name", variable.getMangledName());
        this.currentFunction.addLine((initAssign.render()));

        ST nullLine = this.llvmGroup.getInstanceOf("pushNull");
        this.currentFunction.addLine((nullLine.render()));

        ST line = this.llvmGroup.getInstanceOf("assignVariable");
        line.add("name", variable.getMangledName());

        this.currentFunction.addLine((line.render()));

        this.visitExpression(ctx.expression()); // push the expression to stack and call function to iterate through it

        // convert to vector
        ST promote = this.llvmGroup.getInstanceOf("promoteToVector");
        this.currentFunction.addLine((promote.render()));

        ST loopBegin = this.llvmGroup.getInstanceOf("loopStart");
        loopBegin.add("index", myLoopIndex);
        this.currentFunction.addLine(loopBegin.render());

        // where the magic should happen
        // get vector size
        // if size > 1, get first element, make new vector one element shorter
        // need to push to stack (in order of): new vector, i value, i value
        // else if vector size is one element: push startvector, i value, i value
        // two i values because one for assigning and one for checking conditional

        ST shrink = this.llvmGroup.getInstanceOf("shrinkIterateVector");
        this.currentFunction.addLine(shrink.render());

        ST assignIterator = this.llvmGroup.getInstanceOf("assignVariable");
        assignIterator.add("name", variable.getMangledName());
        this.addCode((assignIterator.render()));

        ST condition = this.llvmGroup.getInstanceOf("notEqualNull");
        this.currentFunction.addLine((condition.render()));

        ST loopConditional = this.llvmGroup.getInstanceOf("loopConditional");
        loopConditional.add("index", myLoopIndex);
        this.currentFunction.addLine(loopConditional.render());

        return myLoopIndex;
    }

    @Override
    public Object visitPrePredicatedLoop(GazpreaParser.PrePredicatedLoopContext ctx) {
        ++this.loopIndex;

        int myLoopIndex = this.loopIndex;

        this.currentLoop.addFirst(myLoopIndex);

        ST loopBegin = this.llvmGroup.getInstanceOf("loopStart");
        loopBegin.add("index", myLoopIndex);
        this.currentFunction.addLine(loopBegin.render());

        this.visitExpression(ctx.expression());

        ST loopConditional = this.llvmGroup.getInstanceOf("loopConditional");
        loopConditional.add("index", myLoopIndex);
        this.currentFunction.addLine(loopConditional.render());

        this.visitTranslationalUnit(ctx.translationalUnit());

        ST loopEnd = this.llvmGroup.getInstanceOf("loopEnd");
        loopEnd.add("index", myLoopIndex);
        this.currentFunction.addLine(loopEnd.render());

        ST loopConditionalEnd = this.llvmGroup.getInstanceOf("loopConditionalEnd");
        loopConditionalEnd.add("index", myLoopIndex);
        this.currentFunction.addLine(loopConditionalEnd.render());

        this.currentLoop.removeFirst();

        return null;
    }

    @Override public Type visitPostPredicatedLoop(GazpreaParser.PostPredicatedLoopContext ctx) {
        ++this.loopIndex;

        int myLoopIndex = this.loopIndex;

        currentLoop.addFirst(myLoopIndex);


        ST startLoop = this.llvmGroup.getInstanceOf("loopStart");
        startLoop.add("index", currentLoop.peekFirst());
        this.currentFunction.addLine(startLoop.render());

        this.visitTranslationalUnit(ctx.translationalUnit());

        this.visitExpression(ctx.expression());

        ST loopConditional = this.llvmGroup.getInstanceOf("loopConditional");
        loopConditional.add("index", currentLoop.peekFirst());
        this.currentFunction.addLine(loopConditional.render());

        ST endLoop = this.llvmGroup.getInstanceOf("loopEnd");
        endLoop.add("index", currentLoop.peekFirst());
        this.currentFunction.addLine(endLoop.render());

        ST loopConditionalEnd = this.llvmGroup.getInstanceOf("loopConditionalEnd");
        loopConditionalEnd.add("index", currentLoop.peekFirst());
        this.currentFunction.addLine(loopConditionalEnd.render());

        currentLoop.removeFirst();

        return null;
    }


    @Override public Type visitBreakStatement(GazpreaParser.BreakStatementContext ctx) {
        //  br label %_break_<index>_label
        ST breakLabel = this.llvmGroup.getInstanceOf("breakStatement");
        breakLabel.add("index", currentLoop.peekFirst());
        this.currentFunction.addLine(breakLabel.render());
        return null;
    }

    @Override public Type visitContinueStatement(GazpreaParser.ContinueStatementContext ctx) {
        ST continueLabel = this.llvmGroup.getInstanceOf("continueStatement");
        continueLabel.add("index", currentLoop.peekFirst());
        this.currentFunction.addLine(continueLabel.render());
        return null;
    }

    @Override
    public String visitFunctionName(GazpreaParser.FunctionNameContext ctx) {
        return ctx.getText();
    }

    @Override
    public Object visitDeclaration(GazpreaParser.DeclarationContext ctx) {
        if (ctx.typedef() != null){
            visit(ctx.typedef());
            return  null;
        }

        Type declaredType = this.visitType(ctx.type());
        String variableName = ctx.Identifier().getText();

        // check to see that variable name is not a typedef
        if (typedefs.containsKey(variableName)){
            throw new Error("Cannot use type names");
        }

//        String sizeData = this.visitSizeData(ctx.sizeData());

        Variable variable = new Variable(variableName, this.mangleVariableName(variableName), declaredType);

        if (this.currentFunction != null) {
            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", variable.getMangledName());
            this.addCode(varLine.render());
        } else {
            this.variables.put(variableName, variable);
        }

        if (variable.getType().getType() != Type.TYPES.NULL
                && variable.getType().getType() != Type.TYPES.TUPLE
                && variable.getType().getType() != Type.TYPES.INTERVAL ) {
            ST initLine = this.llvmGroup.getInstanceOf("varInit_" + variable.getType().getTypeLLVMString());
            this.addCode(initLine.render());

            ST initAssign = this.llvmGroup.getInstanceOf("assignByVar");
            initAssign.add("name", variable.getMangledName());
            this.addCode(initAssign.render());
        } else if (variable.getType().getType() == Type.TYPES.TUPLE && ctx.expression() != null
                || variable.getType().getType() == Type.TYPES.INTERVAL && ctx.expression() != null) {
            ST initAssign = this.llvmGroup.getInstanceOf("assignByVar");
            initAssign.add("name", variable.getMangledName());
            this.addCode(initAssign.render());
        }

        // expression type
        if (ctx.expression() != null) {
            // expression is included
            Type assignedType = this.visitExpression(ctx.expression());
            if (variable.getType().getType() == Type.TYPES.NULL) {
                variable.setType(assignedType);
            }
        } else {
            if (declaredType.getType() != Type.TYPES.TUPLE && declaredType.getType() != Type.TYPES.INTERVAL) {
                // expression portion is excluded
                ST nullLine = this.llvmGroup.getInstanceOf("pushNull");
                this.addCode(nullLine.render());
            }
        }

        this.scope.initVariable(variableName, variable);

        ST line = this.llvmGroup.getInstanceOf("assignByVar");
        line.add("name", variable.getMangledName());

        this.addCode(line.render());

        return null;
    }

    @Override
    public Pair<GazpreaParser.TypeContext, TerminalNode> visitTupleTypeAtom(GazpreaParser.TupleTypeAtomContext ctx) {
        return new Pair<>(ctx.type(), ctx.Identifier());
    }

    @Override
    public Type visitTupleTypeDetails(GazpreaParser.TupleTypeDetailsContext ctx) {
        Tuple tupleType = new Tuple();

        ST startVector = this.llvmGroup.getInstanceOf("startVector");
        this.addCode(startVector.render());
        for (int i = 0; i < ctx.tupleTypeAtom().size(); ++i) {
            Pair<GazpreaParser.TypeContext, TerminalNode> atom = this.visitTupleTypeAtom(ctx.tupleTypeAtom().get(i));
            Type atomType = this.visitType(atom.left());
            promoteType.add(i, atomType);
            if (atom.right() != null) {
                tupleType.addField(atom.right().getText(), atomType);
            } else {
                tupleType.addField("0", atomType);
            }
            ST st;
            switch (atomType.getType()) {
                case BOOLEAN:
                    st = this.llvmGroup.getInstanceOf("varInit_boolean");
                    this.addCode(st.render());
                    break;
                case INTEGER:
                    st = this.llvmGroup.getInstanceOf("varInit_integer");
                    this.addCode(st.render());
                    break;
                case REAL:
                    st = this.llvmGroup.getInstanceOf("varInit_real");
                    this.addCode(st.render());
                    break;
                case CHARACTER:
                    st = this.llvmGroup.getInstanceOf("varInit_character");
                    this.addCode(st.render());
                    break;
                default: throw new RuntimeException("Bad type in tuple");
            }
        }
        ST endTuple = this.llvmGroup.getInstanceOf("endTuple");
        this.addCode(endTuple.render());

        Type type = new Type(Type.SPECIFIERS.VAR, Type.TYPES.TUPLE, tupleType);

        return type;
    }

    @Override
    public Type visitType(GazpreaParser.TypeContext ctx) {

        Type.TYPES typeName = Type.TYPES.NULL;
        if (ctx.typeName() != null) {
            String typeNameString = this.visitTypeName(ctx.typeName());
            switch(typeNameString) {
                case Type.strBOOLEAN:
                    typeName = Type.TYPES.BOOLEAN; break;
                case Type.strCHARACTER:
                    typeName = Type.TYPES.CHARACTER; break;
                case Type.strINTEGER:
                    typeName = Type.TYPES.INTEGER; break;
                case Type.strREAL:
                    typeName = Type.TYPES.REAL; break;
                case Type.strTUPLE:
                    return this.visitTupleTypeDetails(ctx.tupleTypeDetails());
                case Type.strSTRING:
                    // TODO: Consider purging string type by converting to vector char type
                    typeName = Type.TYPES.STRING; break;
                default:
                    if (typedefs.containsKey(typeNameString)){
                        typeName = typedefs.get(typeNameString).getType();
                    }
                    else {
                        throw (new RuntimeException("Type name does not exist" + typeNameString));
                    }
            }
        }

        Type.SPECIFIERS specifier = Type.SPECIFIERS.UNDEFINED;
        if (ctx.TypeSpecifier() != null) {
            switch (ctx.TypeSpecifier().getText()) {
                case Type.strVAR:
                    specifier = Type.SPECIFIERS.VAR;
                    break;
                case Type.strCONST:
                    specifier= Type.SPECIFIERS.CONST;
                    break;
                default:
                    throw(new RuntimeException("Specifier does not exist"));
            }
        }

        Type.COLLECTION_TYPES typeType = null;
        if (ctx.TypeType() != null) {
            switch(ctx.TypeType().getText()){
                case Type.strINTERVAL:
                    // create empty vector type
                    ST startVector = this.llvmGroup.getInstanceOf("startVector");
                    this.addCode(startVector.render());
                    ST st = this.llvmGroup.getInstanceOf("varInit_integer");
                    this.addCode(st.render());
                    this.addCode(st.render());
                    ST endInterval = this.llvmGroup.getInstanceOf("endInterval");
                    this.addCode(endInterval.render());
                    typeName = Type.TYPES.INTERVAL;
                    break;
                case Type.strVECTOR:
                    typeType = Type.COLLECTION_TYPES.VECTOR;
                    break;
                case Type.strMATRIX:
                    typeType = Type.COLLECTION_TYPES.MATRIX;
                    break;
                default:
                    throw(new RuntimeException("Type type does not exist"));
            }
        }

        return new Type(specifier, typeName, typeType);
    }

    @Override public Type visitTypedef(GazpreaParser.TypedefContext ctx) {
        typedefs.put(ctx.Identifier().getText(), this.visitType(ctx.type()));
        return null;
    }


    @Override
    public String visitTypeName(GazpreaParser.TypeNameContext ctx) {
        String string;
        if (ctx.BuiltinType() != null) {
            string = ctx.BuiltinType().getText();
        } else {
            string = ctx.Identifier().getText();
        }

        return string; // for now, just return this text
    }

    @Override
    public String visitSizeData(GazpreaParser.SizeDataContext ctx) {
        return ctx.getText(); // for now, just return this text
    }

    private String mangleVariableName(String name) {
        String mangledName = "GazVar_" + name + "_" + this.scope.uniqueScopeId();
        if (currentFunction != null) {
            mangledName = "%" + mangledName;
        } else {
            mangledName = "@" + mangledName;
        }
        return mangledName;
    }

    private void addCode(String code) {
        if (this.currentFunction != null) {
            this.currentFunction.addLine(code);
        } else {
            this.topLevelCode.add(code);
        }
    }
}
