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

    private Scope<Variable> scope = new Scope<>(); // Name mangler
    private Function currentFunction = null; // For adding code to it

    GazpreaCompiler() {
        this.runtimeGroup = new STGroupFile("./src/runtime.stg");
        this.llvmGroup = new STGroupFile("./src/llvm.stg");

        this.functionNameMappings.put("std_output", "_Z10std_outputv");
        this.functionNameMappings.put("std_input", "_Z9std_inputv");
        this.functionNameMappings.put("stream_state", "_Z12stream_statev");
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

        this.currentFunction = new Function(name, argumentList, returnType);
        if (isProcedure) { this.currentFunction.setProcedure(); }

        argumentList.forEach(argument -> {
            Variable var = new Variable(argument.getName(), this.mangleVariableName(argument.getName()), argument.getType());
            this.scope.initVariable(argument.getName(), var);

            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.addCode(varLine.render());

            ST initLine = this.llvmGroup.getInstanceOf("varInit_" + argument.getType().getTypeLLVMString());
            this.addCode(initLine.render());

            ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
            initAssign.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.addCode(initAssign.render());

            ST varAssign = this.llvmGroup.getInstanceOf("assignVariable");
            varAssign.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.addCode(varAssign.render());
        });

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
        ctx.translationalUnit().forEach(this::visitTranslationalUnit);
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

    @Override
    public Type visitExpression(GazpreaParser.ExpressionContext ctx) {
        if (ctx.Identifier() != null) {
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
            if (ctx.As() == null && ctx.Sign() == null) {
                // CASE: parenthesis
                return this.visitExpression(ctx.expression(0));
            } else if (ctx.As() == null) {
                // CASE: + or - expression
                Type type = this.visitExpression(ctx.expression(0));

                if (ctx.Sign().getText() == "-") {
                    // TODO: call function that negatizes the expression
                }

                return type;
            } else {
                // CASE: casting case
                Type originalType = this.visitExpression(ctx.expression(0));
                Type newType = this.visitType(ctx.type());

                // TODO: get respective casting function

                return newType;
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
        else if (ctx.expression() != null && ctx.expression().size() == 2) {
            Type right = (Type)visit(ctx.expression(0));
            Type left = (Type)visit(ctx.expression(1));
            // TODO CHECK TYPE AND PROMOTE

            String operator = ctx.op.getText();
            if (operator == null){
                // TODO INDEXING OR MATRIX
                return null;
            }
            switch(operator) {
                // CASE: concat
                case "||": {

                };
                // CASE: OR
                case "or": {

                };
                // CASE: XOR
                case "xor": {

                };
                // CASE: AND
                case "and": {

                };
                // CASE: ==
                case "==": {
                    // TODO TUPLE

                };
                // CASE: !=
                case "!=": {
                    // TODO TUPLE


                };
                // CASE: <
                case "<": {

                };
                // CASE: <=
                case "<=": {

                };
                // CASE: >
                case ">": {

                };
                // CASE: >=
                case ">=": {

                };
                // CASE: by
                case "by": {

                };
                // CASE: +
                case "+": {

                };
                // CASE: -
                case "-": {

                };
                // CASE: dotproduct
                case "**": {

                };
                // CASE: *
                case "*": {
                    // TODO type promotion
                    ST operatorCall = this.llvmGroup.getInstanceOf("multiplication");
                    operatorCall.add("typeLetter", "iv"); // TODO get the type of values pushed to stack for typeLetter
                    this.addCode(operatorCall.render());
                    return null;
                }
                // CASE: /
                case "/": {

                };
                // CASE: %
                case "%": {

                };
                // CASE: ^
                case "^": {

                };
                // CASE: interval ..
                case "..": {

                };

            }

//            String operator = "";
//            if (ctx.Multiplication() != null) {
//                operator = ctx.Multiplication().getText();
//            }
//
//            ST operatorCall = null;
//            switch (operator) {
//                case "*": operatorCall = this.llvmGroup.getInstanceOf("multiplicationOperator");
//            }
//            if (operatorCall != null) {
//                this.addCode(operatorCall.render());
//            }
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
            line.add("value", ctx.getText());
            retType = Type.TYPES.INTEGER;
        } else if (ctx.BooleanLiteral() != null) {
            line = this.llvmGroup.getInstanceOf("pushBoolean");
            line.add("value", ctx.getText());
            retType = Type.TYPES.BOOLEAN;
        } else if (ctx.CharacterLiteral() != null) {
            retType = Type.TYPES.CHARACTER;
        } else if (ctx.StringLiteral() != null) {
            retType = Type.TYPES.STRING;
        } else if (ctx.RealLiteral() != null) {
            retType = Type.TYPES.REAL;
        } else if (ctx.tupleLiteral() != null) {
            // TODO: handle tuple types
        } else if (ctx.vectorLiteral() != null) {
            Type vectorType = this.visitVectorLiteral(ctx.vectorLiteral());
            retCollectionType = Type.COLLECTION_TYPES.VECTOR;
            retType = vectorType.getType();
        }
        if (line != null) {
            this.addCode(line.render());
        }

        // TODO: literals are considered constant types
        return new Type(Type.SPECIFIERS.CONST, retType, retCollectionType);
    }

    @Override
    public Type visitVectorLiteral(GazpreaParser.VectorLiteralContext ctx) {
        // TODO: Implement this function
        // return super.visitVectorLiteral(ctx);
        return new Type(null, null, null, null);
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

        ST line = this.llvmGroup.getInstanceOf("functionReturn");
        this.addCode(line.render());

        return null;
    }

    @Override
    public Object visitAssignment(GazpreaParser.AssignmentContext ctx) {
        this.visitExpression(ctx.expression());
        ST assign = this.llvmGroup.getInstanceOf("assignVariable");
        assign.add("name", this.scope.getVariable(ctx.Identifier().getText()).getMangledName());
        this.addCode(assign.render());
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

    private String getVariableFromExpression(GazpreaParser.ExpressionContext ctx) {
        if (ctx.expression() != null && ctx.expression().size() == 1) {
            return getVariableFromExpression(ctx.expression(0));
        }
        if (ctx.Identifier() != null) {
            return ctx.Identifier().getText();
        }
        return "";
    }

    @Override
    public String visitFunctionName(GazpreaParser.FunctionNameContext ctx) {
        return ctx.getText();
    }

    @Override
    public Object visitDeclaration(GazpreaParser.DeclarationContext ctx) {
        // Type does it's best to get the type of the variable
        Type declaredType = this.visitType(ctx.type());

        String variableName = ctx.Identifier().getText();
//        String sizeData = this.visitSizeData(ctx.sizeData());

        // expression portion type
        Type assignedType = null;

        if (ctx.expression() != null) {
            // expression portion is included

            assignedType = this.visitExpression(ctx.expression());

            if (!declaredType.equals(assignedType) && declaredType.getType() != Type.TYPES.NULL) {
                // case where variable is not implicitly declared and the declared type is different from
                // the assigned value
                String promotionFunction = Type.getPromoteFunction(assignedType, declaredType);
                // TODO: promote to declared type in LLVM
            } else {
                declaredType = assignedType;
            }
        } else {
            // expression portion is excluded

            // TODO: We can choose which null gets pushed with type
            String nullFunction = Type.getNullFunction(declaredType);
            ST nullLine = this.llvmGroup.getInstanceOf("pushNull");
            this.addCode(nullLine.render());
        }

        // TODO: differentiating between a tuple expression and a parenthesis expression

        Variable variable = new Variable(variableName, this.mangleVariableName(variableName), declaredType);
        this.scope.initVariable(variableName, variable);

        if (this.currentFunction != null) {
            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", this.scope.getVariable(variableName).getMangledName());
            this.currentFunction.addLine(varLine.render());
        } else {
            this.variables.put(variableName, variable);
        }

        if (variable.getType().getType() != Type.TYPES.NULL) {
            ST initLine = this.llvmGroup.getInstanceOf("varInit_" + variable.getType().getTypeLLVMString());
            this.addCode(initLine.render());

            ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
            initAssign.add("name", this.scope.getVariable(variableName).getMangledName());
            this.addCode(initAssign.render());
        }

        ST line = this.llvmGroup.getInstanceOf("assignVariable");
        line.add("name", this.scope.getVariable(variableName).getMangledName());
        this.addCode(line.render());

        return null;
    }

    @Override
    public Type visitType(GazpreaParser.TypeContext ctx) {
        Type.TYPES typeName = Type.TYPES.NULL;

        if (ctx.typeName() != null) {
            switch(ctx.typeName().getText()) {
                case Type.strBOOLEAN:
                    typeName = Type.TYPES.BOOLEAN; break;
                case Type.strCHARACTER:
                    typeName = Type.TYPES.CHARACTER; break;
                case Type.strINTEGER:
                    typeName = Type.TYPES.INTEGER; break;
                case Type.strREAL:
                    typeName = Type.TYPES.REAL; break;
                case Type.strSTRING:
                    // TODO: Consider purging string type by converting to vector char type
                    typeName = Type.TYPES.STRING; break;
                default:
                    throw(new RuntimeException("Type name does not exist"));
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

        Type.COLLECTION_TYPES typeType = Type.COLLECTION_TYPES.NONE;

        if (ctx.TypeType() != null) {
            switch(ctx.TypeType().getText()){
                case Type.strINTERVAL:
                    typeType = Type.COLLECTION_TYPES.INTERVAL;
                    break;
                case Type.strVECTOR:
                    typeType = Type.COLLECTION_TYPES.VECTOR;
                    break;
                case Type.strTUPLE:
                    typeType = Type.COLLECTION_TYPES.TUPLE;
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

    @Override
    public String visitTypeName(GazpreaParser.TypeNameContext ctx) {
        return ctx.getText(); // for now, just return this text
    }

    @Override
    public String visitSizeData(GazpreaParser.SizeDataContext ctx) {
        return ctx.getText(); // for now, just return this text
    }

    private String mangleVariableName(String name) {
        String mangledName = "GazVar_" + name + "_" + this.scope.count();
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
