import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

class GazpreaCompiler extends GazpreaBaseVisitor<Object> {
    private STGroup runtimeGroup;
    private STGroup llvmGroup;

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
        String returnType = this.visitReturnType(returnTypeContext);

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
    public String visitReturnType(GazpreaParser.ReturnTypeContext ctx) {
        if (ctx != null && ctx.type() != null) {
            return ctx.type().getText(); // for now, just return this text
        }
        return "";
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
            ST line = this.llvmGroup.getInstanceOf("pushVariable");
            line.add("name", this.scope.getVariable(ctx.Identifier().getText()).getMangledName());
            this.addCode(line.render());
        }
        if (ctx.literal() != null) {
            this.visitLiteral(ctx.literal());
        }
        if (ctx.expression() != null && ctx.expression().size() == 1) {
            this.visitExpression(ctx.expression(0));
        }
        if (ctx.functionCall() != null) {
            this.visitFunctionCall(ctx.functionCall());
        }
//        if (ctx.expression() != null && ctx.expression().size() == 2) {
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
//        }
        return null;
    }

    @Override
    public Object visitLiteral(GazpreaParser.LiteralContext ctx) {
        ST line = null;
        if (ctx.NullLiteral() != null) {
            line = this.llvmGroup.getInstanceOf("pushNull");
        }
        if (ctx.IdentityLiteral() != null) {
            line = this.llvmGroup.getInstanceOf("pushIdentity");
        }
        if (ctx.IntegerLiteral() != null) {
            line = this.llvmGroup.getInstanceOf("pushInteger");
            line.add("value", ctx.getText());
        }
        if (line != null) {
            this.addCode(line.render());
        }
        return null;
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
    public Object visitFunctionCall(GazpreaParser.FunctionCallContext ctx) {
        List<GazpreaParser.ExpressionContext> arguments = ctx.expression();
        Collections.reverse(arguments);
        arguments.forEach(this::visitExpression);

        String functionName = this.visitFunctionName(ctx.functionName());
        ST functionCall = this.llvmGroup.getInstanceOf("functionCall");
        String mangledFunctionName = this.functionNameMappings.get(functionName);
        functionCall.add("name", mangledFunctionName);
        this.addCode(functionCall.render());
        return null;
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
        if (ctx.expression() != null) {
            Type assignedType = this.visitExpression(ctx.expression());
        } else {
            // TODO: We can choose which null gets pushed with type
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

        if (variable.getType().getTypeLLVMString().length() > 0) {
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
        Type.TYPES typeName = Type.TYPES.UNKNOWN;

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
                    specifier = Type.SPECIFIERS.VAR; break;
                case Type.strCONST:
                    specifier= Type.SPECIFIERS.CONST; break;
                default:
                    throw(new RuntimeException("Specifier does not exist"));
            }
        }

        Type.COLLECTION_TYPES typeType = Type.COLLECTION_TYPES.NONE;

        if (ctx.TypeType() != null) {
            switch(ctx.TypeType().getText()){
                case Type.strINTERVAL:
                    typeType = Type.COLLECTION_TYPES.INTERVAL; break;
                case Type.strVECTOR:
                    typeType = Type.COLLECTION_TYPES.VECTOR; break;
                case Type.strTUPLE:
                    typeType = Type.COLLECTION_TYPES.TUPLE; break;
                case Type.strMATRIX:
                    typeType = Type.COLLECTION_TYPES.MATRIX; break;
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
