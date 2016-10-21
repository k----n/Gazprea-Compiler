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
        program.add("variables", "");
        program.add("functions", functionIR);
        program.add("code", "");
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
        String functionName = ctx.Identifier().getText();

        if (this.functions.get(functionName) != null) {
            if (this.functions.get(functionName).isDefined() || ctx.functionBlock() == null) {
                System.err.println("Illegal duplicate definition of function " + functionName);
                System.exit(1);
            }
        }

        this.scope.pushScope();

        List<Argument> argumentList = this.visitArgumentList(ctx.argumentList());
        String returnType = this.visitReturnType(ctx.returnType());

        this.currentFunction = new Function(functionName, argumentList, returnType);

        argumentList.forEach(argument -> {
            Variable var = new Variable(argument.getName(), this.mangleVariableName(argument.getName()), argument.getType());
            this.scope.initVariable(argument.getName(), var);

            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.currentFunction.addLine(varLine.render());

            ST initLine = this.llvmGroup.getInstanceOf("varInit_" + argument.getType().getName());
            this.currentFunction.addLine(initLine.render());

            ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
            initAssign.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.currentFunction.addLine(initAssign.render());

            ST varAssign = this.llvmGroup.getInstanceOf("assignVariable");
            varAssign.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.currentFunction.addLine(varAssign.render());
        });

        if (ctx.functionBlock() != null) {
            this.visitFunctionBlock(ctx.functionBlock());
        }

        this.scope.popScope();

        this.functions.put(functionName, this.currentFunction);
        this.currentFunction = null;
        this.functionNameMappings.put(functionName, "GazFunc_" + functionName);

        return null;
    }

    @Override
    public Object visitProcedure(GazpreaParser.ProcedureContext ctx) {
        // TODO: FUNC + PROCEDURES COULD BE MISSING OR HAVE EXPTRA RETURNS
        // TODO: FUNC + PROCEDURES HAVE MUTABLE ARGUMETNS BUT ONLY VARS GET PASSED OUT
        String functionName = ctx.Identifier().getText();

        if (this.functions.get(functionName) != null) {
            if (this.functions.get(functionName).isDefined() || ctx.functionBlock() == null) {
                System.err.println("Illegal duplicate definition of function " + functionName);
                System.exit(1);
            }
        }

        this.scope.pushScope();

        List<Argument> argumentList = this.visitArgumentList(ctx.argumentList());
        String returnType = this.visitReturnType(ctx.returnType());

        this.currentFunction = new Function(functionName, argumentList, returnType);
        this.currentFunction.setProcedure();

        argumentList.forEach(argument -> {
            Variable var = new Variable(argument.getName(), this.mangleVariableName(argument.getName()), argument.getType());
            this.scope.initVariable(argument.getName(), var);

            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.currentFunction.addLine(varLine.render());

            ST initLine = this.llvmGroup.getInstanceOf("varInit_" + argument.getType().getName());
            this.currentFunction.addLine(initLine.render());

            ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
            initAssign.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.currentFunction.addLine(initAssign.render());

            ST varAssign = this.llvmGroup.getInstanceOf("assignVariable");
            varAssign.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.currentFunction.addLine(varAssign.render());
        });

        if (ctx.functionBlock() != null) {
            this.visitFunctionBlock(ctx.functionBlock());
        }

        this.scope.popScope();

        this.functions.put(functionName, this.currentFunction);
        this.currentFunction = null;
        this.functionNameMappings.put(functionName, "GazFunc_" + functionName);

        return null;
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
            this.currentFunction.addLine(line.render());
        }
        if (ctx.RightArrow() != null) {
            ST line = this.llvmGroup.getInstanceOf("rightArrowOperator");
            this.currentFunction.addLine(line.render());
        }
        return null;
    }

    @Override
    public Object visitExpression(GazpreaParser.ExpressionContext ctx) {
        if (ctx.Identifier() != null) {
            ST line = this.llvmGroup.getInstanceOf("pushVariable");
            line.add("name", this.scope.getVariable(ctx.Identifier().getText()).getMangledName());
            this.currentFunction.addLine(line.render());
        }
        if (ctx.literal() != null) {
            this.visitLiteral(ctx.literal());
        }
        if (ctx.expression() != null) {
            this.visitExpression(ctx.expression());
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
//                this.currentFunction.addLine(operatorCall.render());
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
            this.currentFunction.addLine(line.render());
        }
        return null;
    }

    @Override
    public Object visitReturnStatement(GazpreaParser.ReturnStatementContext ctx) {
        this.currentFunction.getArguments().forEach(argument -> {
            if (argument.getType().getSpecifier().equals("var")) {
                ST push = this.llvmGroup.getInstanceOf("pushVariable");
                push.add("name", this.scope.getVariable(argument.getName()).getMangledName());
                this.currentFunction.addLine(push.render());
            }
        });

        if (ctx.expression() != null) {
            this.visitExpression(ctx.expression());
        }

        ST line = this.llvmGroup.getInstanceOf("functionReturn");
        this.currentFunction.addLine(line.render());

        return null;
    }

    @Override
    public Object visitAssignment(GazpreaParser.AssignmentContext ctx) {
        this.visitExpression(ctx.expression());
        ST assign = this.llvmGroup.getInstanceOf("assignVariable");
        assign.add("name", this.scope.getVariable(ctx.Identifier().getText()).getMangledName());
        this.currentFunction.addLine(assign.render());
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
        this.currentFunction.addLine(functionCall.render());
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
            if (pair.left().getType().getSpecifier().equals("var")) {
                ST postCallAssign = this.llvmGroup.getInstanceOf("assignVariable");
                postCallAssign.add("name", this.scope.getVariable(pair.right()).getMangledName());
                this.currentFunction.addLine(postCallAssign.render());
            }
        });

        return null;
    }

    private String getVariableFromExpression(GazpreaParser.ExpressionContext ctx) {
        if (ctx.expression() != null /*&& ctx.expression().size() == 1*/) {
            return getVariableFromExpression(ctx.expression()/*.get(0)*/);
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
        Type type = this.visitType(ctx.type());
        String variableName = ctx.Identifier().getText();
//        String sizeData = this.visitSizeData(ctx.sizeData());
        if (ctx.expression() != null) {
            this.visitExpression(ctx.expression());
        } else {
            ST nullLine = this.llvmGroup.getInstanceOf("pushNull");
            this.currentFunction.addLine(nullLine.render());
        }

        Variable variable = new Variable(variableName, this.mangleVariableName(variableName), type);
        this.scope.initVariable(variableName, variable);

        ST varLine = this.llvmGroup.getInstanceOf("localVariable");
        varLine.add("name", this.scope.getVariable(variableName).getMangledName());
        this.currentFunction.addLine(varLine.render());

        if (variable.getType().getName().length() > 0) {
            ST initLine = this.llvmGroup.getInstanceOf("varInit_" + variable.getType().getName());
            this.currentFunction.addLine(initLine.render());

            ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
            initAssign.add("name", this.scope.getVariable(variableName).getMangledName());
            this.currentFunction.addLine(initAssign.render());
        }

        ST line = this.llvmGroup.getInstanceOf("assignVariable");
        line.add("name", this.scope.getVariable(variableName).getMangledName());
        this.currentFunction.addLine(line.render());

        return null;
    }

    @Override
    public Type visitType(GazpreaParser.TypeContext ctx) {
        String specifier = "";
        if (ctx.TypeSpecifier() != null) {
            specifier = ctx.TypeSpecifier().getText();
        }
        String name = "";
        if (ctx.typeName() != null) {
            name = this.visitTypeName(ctx.typeName());
        }
        boolean isVector = ctx.TypeType() != null;
        return new Type(specifier, name, isVector);
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
}

//import org.stringtemplate.v4.ST;
//import org.stringtemplate.v4.STGroup;
//import org.stringtemplate.v4.STGroupFile;
//
//import java.io.PrintWriter;
//import java.util.*;
//
//public class GazpreaCompiler extends GazpreaBaseVisitor<Object> {
//    private STGroup runtimeGroup;
//    private STGroup llvmGroup;
//
//    private Set<String> variablesToInject = new HashSet<>();
//    private List<String> functionsToInject = new ArrayList<>();
//    private Stack<List<String>> linesToInject = new Stack<>();
//
//    private Scope<Integer> variables = new Scope<>();
//
//    private Map<String, String> functionNameMappings = new HashMap<>();
//
//    GazpreaCompiler() {
//        this.runtimeGroup = new STGroupFile("./src/runtime.stg");
//        this.llvmGroup = new STGroupFile("./src/llvm.stg");
//
//        // Add builtins
//        this.functionNameMappings.put("std_output", "_Z10std_outputv");
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitCompilationUnit(GazpreaParser.CompilationUnitContext ctx) {
//        this.linesToInject.push(new ArrayList<>());
//
//        if (ctx.translationUnit() != null) this.visitTranslationUnit(ctx.translationUnit());
//
//        ST runtime = this.runtimeGroup.getInstanceOf("runtime");
//        runtime.add("variables", this.linesToInject);
//        runtime.add("functions", this.functionsToInject);
//        runtime.add("code", this.linesToInject.pop());
//
//        try {
//            PrintWriter writer = new PrintWriter("program.s", "UTF-8");
//            writer.println(runtime.render());
//            writer.close();
//        } catch (Exception e) { e.printStackTrace(); }
//
//        System.out.println(runtime.render());
//
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitTranslationUnit(GazpreaParser.TranslationUnitContext ctx) {
//        if (ctx.translationUnit() != null) {
//            this.visitTranslationUnit(ctx.translationUnit());
//        }
//        this.visitStatement_(ctx.statement_());
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitStatement_(GazpreaParser.Statement_Context ctx) {
//        if (ctx.statement() != null) {
//            this.visitStatement(ctx.statement());
//        }
//        if (ctx.notStatement() != null) {
//            this.visitNotStatement(ctx.notStatement());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitStatement(GazpreaParser.StatementContext ctx) {
//        if (ctx.declaration() != null) {
//            this.visitDeclaration(ctx.declaration());
//        }
//        if (ctx.assignment() != null) {
//            this.visitAssignment(ctx.assignment());
//        }
//        if (ctx.typedef() != null) {
//            this.visitTypedef(ctx.typedef());
//        }
//        if (ctx.iterator() != null) {
//            this.visitIterator(ctx.iterator());
//        }
//        if (ctx.infiniteLoop() != null) {
//            this.visitInfiniteLoop(ctx.infiniteLoop());
//        }
//        if (ctx.loop() != null) {
//            this.visitLoop(ctx.loop());
//        }
//        if (ctx.conditional() != null) {
//            this.visitConditional(ctx.conditional());
//        }
//        if (ctx.Break() != null) {
//            // TODO: Handle break
//        }
//        if (ctx.Continue() != null) {
//            // TODO: Handle continue
//        }
//        if (ctx.streamStatement() != null) {
//            this.visitStreamStatement(ctx.streamStatement());
//        }
//        if (ctx.procedureCall() != null) {
//            this.visitProcedureCall(ctx.procedureCall());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitNotStatement(GazpreaParser.NotStatementContext ctx) {
//        if (ctx.function() != null) {
//            this.visitFunction(ctx.function());
//        }
//        if (ctx.procedure() != null) {
//            this.visitProcedure(ctx.procedure());
//        }
//        if (ctx.block() != null) {
//            this.visitBlock(ctx.block());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitStreamStatement(GazpreaParser.StreamStatementContext ctx) {
//        this.visitExpression(ctx.expression(0));
//        this.visitExpression(ctx.expression(1));
//        if (ctx.LeftArrow() != null) {
//            // TODO: Handle left arrow operator
//        }
//        if (ctx.RightArrow() != null) {
//            ST line = this.llvmGroup.getInstanceOf("rightArrowOperator");
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitExpression(GazpreaParser.ExpressionContext ctx) {
//        this.visitIndexingExpression(ctx.indexingExpression());
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitIndexingExpression(GazpreaParser.IndexingExpressionContext ctx) {
//        if (ctx.rangeExpression() != null) {
//            this.visitRangeExpression(ctx.rangeExpression());
//        }
//        if (ctx.indexingExpression() != null) {
//            this.visitIndexingExpression(ctx.indexingExpression());
//            this.visitExpression(ctx.expression());
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitRangeExpression(GazpreaParser.RangeExpressionContext ctx) {
//        ctx.unaryExpression().forEach(this::visitUnaryExpression);
//        if (ctx.Interval() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitUnaryExpression(GazpreaParser.UnaryExpressionContext ctx) {
//        if (ctx.exponentiationExpression() != null) {
//            this.visitExponentiationExpression(ctx.exponentiationExpression());
//        }
//        if (ctx.unaryExpression() != null) {
//            this.visitUnaryExpression(ctx.unaryExpression());
//        }
//        if (ctx.Sign() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.Not() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitExponentiationExpression(GazpreaParser.ExponentiationExpressionContext ctx) {
//        if (ctx.exponentiationExpression() != null) {
//            this.visitExponentiationExpression(ctx.exponentiationExpression());
//        }
//        this.visitMultExpression(ctx.multExpression());
//        if (ctx.Exponentiation() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitMultExpression(GazpreaParser.MultExpressionContext ctx) {
//        if (ctx.multExpression() != null) {
//            this.visitMultExpression(ctx.multExpression());
//        }
//        this.visitDotProductExpression(ctx.dotProductExpression());
//        if (ctx.Multiplication() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.Division() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.Modulus() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitDotProductExpression(GazpreaParser.DotProductExpressionContext ctx) {
//        if (ctx.dotProductExpression() != null) {
//            this.visitDotProductExpression(ctx.dotProductExpression());
//        }
//        this.visitAddExpression(ctx.addExpression());
//        if (ctx.DotProduct() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitAddExpression(GazpreaParser.AddExpressionContext ctx) {
//        if (ctx.addExpression() != null) {
//            this.visitAddExpression(ctx.addExpression());
//        }
//        this.visitByExpression(ctx.byExpression());
//        if (ctx.Sign() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitByExpression(GazpreaParser.ByExpressionContext ctx) {
//        if (ctx.byExpression() != null) {
//            this.visitByExpression(ctx.byExpression());
//        }
//        this.visitComparisonExpression(ctx.comparisonExpression());
//        if (ctx.By() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitComparisonExpression(GazpreaParser.ComparisonExpressionContext ctx) {
//        if (ctx.comparisonExpression() != null) {
//            this.visitComparisonExpression(ctx.comparisonExpression());
//        }
//        this.visitEqualityExpression(ctx.equalityExpression());
//        if (ctx.LessThan() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.LessThanOrEqual() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.GreaterThan() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.GreaterThanOrEqual() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitEqualityExpression(GazpreaParser.EqualityExpressionContext ctx) {
//        if (ctx.equalityExpression() != null) {
//            this.visitEqualityExpression(ctx.equalityExpression());
//        }
//        this.visitBitwiseAndExpression(ctx.bitwiseAndExpression());
//        if (ctx.Equals() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.NotEqual() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitBitwiseAndExpression(GazpreaParser.BitwiseAndExpressionContext ctx) {
//        if (ctx.bitwiseAndExpression() != null) {
//            this.visitBitwiseAndExpression(ctx.bitwiseAndExpression());
//        }
//        this.visitBitwiseOrExpression(ctx.bitwiseOrExpression());
//        if (ctx.And() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitBitwiseOrExpression(GazpreaParser.BitwiseOrExpressionContext ctx) {
//        if (ctx.bitwiseOrExpression() != null) {
//            this.visitBitwiseOrExpression(ctx.bitwiseOrExpression());
//        }
//        this.visitLogicalOrExpression(ctx.logicalOrExpression());
//        if (ctx.Or() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.Xor() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitLogicalOrExpression(GazpreaParser.LogicalOrExpressionContext ctx) {
//        if (ctx.logicalOrExpression() != null) {
//            this.visitLogicalOrExpression(ctx.logicalOrExpression());
//        }
//        this.visitPrimaryExpression(ctx.primaryExpression());
//        if (ctx.Concatenation() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitPrimaryExpression(GazpreaParser.PrimaryExpressionContext ctx) {
//        if (ctx.Identifier() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.literal() != null) {
//            this.visitLiteral(ctx.literal());
//        }
//        if (ctx.expression() != null) {
//            this.visitExpression(ctx.expression());
//        }
//        if (ctx.As() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.generator() != null) {
//            this.visitGenerator(ctx.generator());
//        }
//        if (ctx.filter() != null) {
//            this.visitFilter(ctx.filter());
//        }
//        if (ctx.functionCall() != null) {
//            this.visitFunctionCall(ctx.functionCall());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitLiteral(GazpreaParser.LiteralContext ctx) {
//        if (ctx.NullLiteral() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.IdentityLiteral() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.BooleanLiteral() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.IntegerLiteral() != null) {
//            ST line = this.llvmGroup.getInstanceOf("pushInteger");
//            line.add("value", ctx.IntegerLiteral().getText());
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.RealLiteral() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.CharacterLiteral() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.StringLiteral() != null) {
//            ST line = this.llvmGroup.getInstanceOf(""); // TODO:
//            this.linesToInject.lastElement().add(line.render());
//        }
//        if (ctx.vectorLiteral() != null) {
//            this.visitVectorLiteral(ctx.vectorLiteral());
//        }
//        if (ctx.tupleLiteral() != null) {
//            this.visitTupleLiteral(ctx.tupleLiteral());
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitFunctionCall(GazpreaParser.FunctionCallContext ctx) {
//        String functionName = this.visitFunctionName(ctx.functionName());
//        ST functionCall = this.llvmGroup.getInstanceOf("functionCall");
//        String mangledFunctionName = this.functionNameMappings.get(functionName);
//        functionCall.add("name", mangledFunctionName);
//        // TODO: Process arguments
//        this.linesToInject.lastElement().add(functionCall.render());
//        return null;
//    }
//
//    @Override
//    public String visitFunctionName(GazpreaParser.FunctionNameContext ctx) {
//        if (ctx.Identifier() != null) {
//            return ctx.Identifier().getText();
//        }
//        if (ctx.BuiltinFunction() != null) {
//            return ctx.BuiltinFunction().getText();
//        }
//        return null;
//    }
//
//    /// - Returns: null
//    @Override
//    public Object visitFunction(GazpreaParser.FunctionContext ctx) {
//        Function Identifier typeData Returns type ((block | Assign expression ';') | ';')
//
//        String functionName = ctx.Identifier().getText();
//
//
//        return null;
//    }
//}
