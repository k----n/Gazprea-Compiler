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

    private final String BUILT_INS[] = {
            "std_output",
            "std_input",
            "stream_state",
            "length",
            "rows",
            "columns",
            "reverse"
    };

    private Map<String, List<Function>> functions = new HashMap<>();
    private Map<String, List<Function>> builtinFunctions = new HashMap<>();
    private Map<String, Variable> variables = new HashMap<>();

    private List<String> topLevelCode = new ArrayList<>();

    private Map<String, String> functionNameMappings = new HashMap<>();

    private Map<String, Type> typedefs = new HashMap<>();

    private int conditionalIndex;
    private int loopIndex;

    private Stack<ArrayList<Type>> promoteType = new Stack<>();

    private Deque<Integer> currentLoop = new ArrayDeque<>();
    private Deque<Integer> currentIterator = new ArrayDeque<>();

    private Scope<Variable> scope = new Scope<>(); // Name mangler
    private Function currentFunction = null; // For adding code to it

    GazpreaCompiler() {
        this.runtimeGroup = new STGroupFile("./src/runtime.stg");
        this.llvmGroup = new STGroupFile("./src/llvm.stg");

        this.functionNameMappings.put("std_output", "_Z10std_outputv");
        this.functionNameMappings.put("std_input", "_Z9std_inputv");
        this.functionNameMappings.put("stream_state", "_Z12stream_statev");
        this.functionNameMappings.put("length", "_Z6lengthv");
        this.functionNameMappings.put("rows", "_Z4rowsv");
        this.functionNameMappings.put("columns", "_Z7columnsv");
        this.functionNameMappings.put("reverse", "_Z7reversev");

        List<Function> std_output_list = new ArrayList<>();
        std_output_list.add(new Function("std_output", new ArrayList<>(), new Type(Type.SPECIFIERS.CONST, Type.TYPES.OUTPUT_STREAM)));
        this.builtinFunctions.put("std_output", std_output_list);

        List<Function> std_input_list = new ArrayList<>();
        std_input_list.add(new Function("std_input", new ArrayList<>(), new Type(Type.SPECIFIERS.CONST, Type.TYPES.INPUT_STREAM)));
        this.builtinFunctions.put("std_input", std_input_list);

        List<Function> stream_state_list = new ArrayList<>();
        List<Argument> stream_state_args = new ArrayList<>();
        stream_state_args.add(new Argument(new Type(Type.SPECIFIERS.CONST, Type.TYPES.INPUT_STREAM), "inp"));
        stream_state_list.add(new Function("stream_state", stream_state_args, new Type(Type.SPECIFIERS.CONST, Type.TYPES.INTEGER)));
        this.builtinFunctions.put("stream_state", stream_state_list);

        List<Function> length_list = new ArrayList<>();
        List<Argument> length_args = new ArrayList<>();
        length_args.add(new Argument(new Type(Type.SPECIFIERS.CONST, null, Type.COLLECTION_TYPES.VECTOR), "vec"));
        length_list.add(new Function("length", length_args, new Type(Type.SPECIFIERS.CONST, Type.TYPES.INTEGER)));
        this.builtinFunctions.put("length", length_list);

        List<Function> rows_list = new ArrayList<>();
        List<Argument> rows_args = new ArrayList<>();
        rows_args.add(new Argument(new Type(Type.SPECIFIERS.CONST, null, Type.COLLECTION_TYPES.MATRIX), "mat"));
        rows_list.add(new Function("rows", rows_args, new Type(Type.SPECIFIERS.CONST, Type.TYPES.INTEGER)));
        this.builtinFunctions.put("rows", rows_list);

        List<Function> columns_list = new ArrayList<>();
        List<Argument> columns_args = new ArrayList<>();
        columns_args.add(new Argument(new Type(Type.SPECIFIERS.CONST, null, Type.COLLECTION_TYPES.MATRIX), "mat"));
        columns_list.add(new Function("columns", columns_args, new Type(Type.SPECIFIERS.CONST, Type.TYPES.INTEGER)));
        this.builtinFunctions.put("columns", columns_list);

        List<Function> reverse_list = new ArrayList<>();
        List<Argument> reverse_args = new ArrayList<>();
        reverse_args.add(new Argument(new Type(Type.SPECIFIERS.CONST, null, Type.COLLECTION_TYPES.VECTOR), "vec"));
        reverse_list.add(new Function("reverse", reverse_args, new Type(Type.SPECIFIERS.CONST, null, Type.COLLECTION_TYPES.VECTOR)));
        this.builtinFunctions.put("reverse", reverse_list);

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

        List<String> functionIR = new ArrayList<String>();
        for (Map.Entry<String, List<Function>> entry : functions.entrySet()) {
            List<Function> funcs = entry.getValue();
            for (int f = 0; f < funcs.size(); ++f) {
                ST functionLines = this.llvmGroup.getInstanceOf("function");
                functionLines.add("name", this.getFunctionLLVMName(funcs.get(f)));
                functionLines.add("code", funcs.get(f).render());
                functionIR.add(functionLines.render());
            }
        }

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

        this.scope.pushScope();

        List<Argument> argumentList = this.visitArgumentList(argumentListContext);
        Type returnType = this.visitReturnType(returnTypeContext);

        // argument list of current function is set a little ways down
        this.currentFunction = new Function(name, null, returnType);
        if (isProcedure) { this.currentFunction.setProcedure(); }

        for (int arg = 0; arg < argumentListContext.argument().size(); ++arg) {
            Argument argument = this.visitArgument(argumentListContext.argument(arg));

            Variable var = new Variable(argument.getName(), this.mangleVariableName(argument.getName()), argument.getType());

            this.scope.initVariable(argument.getName(), var);

            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", this.scope.getVariable(argument.getName()).getMangledName());
            this.addCode(varLine.render());

            if (argument.getType().getType() != Type.TYPES.TUPLE &&
                    argument.getType().getType() != Type.TYPES.INTERVAL) {
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
        this.addRedefineFunction(this.currentFunction);


        if (functionBlockContext != null) {
            this.visitFunctionBlock(functionBlockContext);
        }

        this.scope.popScope();
        this.currentFunction = null;
    }

    // gets the obfuscated name of the function
    private String getFunctionLLVMName(Function function) {
        // special case is main with no arguments
        if (function.getName().equals("main") && function.getArguments().size() == 0) {
            return "GazFunc_main";
        }

        List<Function> nameSakes = this.functions.get(function.getName());

        if (nameSakes == null) {
            throw new RuntimeException("getFunctionLLVMName: Function not defined");
        }

        for (int f = 0; f < nameSakes.size(); ++f) {
            if (Function.strictEquals(nameSakes.get(f), function)) {
                return "GazFunc_" + function.getName() + "." + Integer.toString(f);
            }
        }

        return null;
    }

    // gets the function already declared if it exists
    // otherwise returns null
    private Function getReferringFunction(Function function) {
        List<Function> nameSakes = this.functions.get(function.getName());

        if (nameSakes == null) {
            throw new RuntimeException("getReferringFunction: Function not defined");
        }

        for (Function sake : nameSakes) {
            if (Function.strictEquals(sake, function)) {
                return sake;
            }
        }

        return null;
    }

    // gets the function related to the function call if it exists
    // otherwise throws error
    private Function getReferringFunction(String name, List<Argument> arguments) {
        List<Function> possibilities = new ArrayList<>();

        if (this.functions.get(name) != null) {
            possibilities.addAll(this.functions.get(name));
        }

        if (this.builtinFunctions.get(name) != null) {
            possibilities.addAll(this.builtinFunctions.get(name));
        }

        for (Function poss : possibilities) {
            if (Function.looseEquals(arguments, poss)) {
                return poss;
            }
        }

        return null;
    }

    // this for redefining prototypes with their function definition or
    //    adding it to the functions mapping if it isn't already there
    private Void addRedefineFunction(Function function) {
        List<Function> nameSakes = this.functions.get(function.getName());

        if (nameSakes == null) {
            List<Function> newFunctions = new ArrayList<>();
            newFunctions.add(function);
            this.functions.put(function.getName(), newFunctions);
            return null;
        }

        boolean repeat = false;
        for (int f = 0; f < nameSakes.size(); ++f) {
            if (Function.strictEquals(nameSakes.get(f), function)) {
                nameSakes.set(f, this.currentFunction);
                repeat = true;
                break;
            }
        }

        if (!repeat) {
            nameSakes.add(function);
        }

        this.functions.put(function.getName(), nameSakes);

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

            this.scope.getLocalScopeVariables().forEach(pair -> {
                if (pair.right().getType().getSpecifier() != Type.SPECIFIERS.VAR) {
                    ST free = this.llvmGroup.getInstanceOf("freeVariable");
                    free.add("name", pair.right().getMangledName());
                    this.addCode(free.render());
                }
            });

            ST line = this.llvmGroup.getInstanceOf("functionReturn");
            this.addCode(line.render());
        }
        if (ctx.expression() != null) {
            this.scope.getLocalScopeVariables().forEach(pair -> {
                if (pair.right().getType().getSpecifier() != Type.SPECIFIERS.VAR) {
                    ST free = this.llvmGroup.getInstanceOf("freeVariable");
                    free.add("name", pair.right().getMangledName());
                    this.addCode(free.render());
                }
            });

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
        if (ctx.expression().size() == 1 && ctx.Dot().size() == 1 && ctx.RealLiteral().size() == 1 && ctx.getChild(0) == ctx.expression()){
            // CASE: expression Dot RealLiteral
            // Interval

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
        else if (ctx.expression().size() == 1 && ctx.Dot().size() == 1 && ctx.RealLiteral().size() == 1){
            // CASE: RealLiteral Dot expression
            // Interval
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
                } else if (type.getCollection_type() == Type.COLLECTION_TYPES.VECTOR) {
                    ST operatorCall = this.llvmGroup.getInstanceOf("negVector");
                    this.addCode(operatorCall.render());
                } else if (type.getCollection_type() == Type.COLLECTION_TYPES.MATRIX) {
                    ST negation = this.llvmGroup.getInstanceOf("negation");
                    negation.add("typeLetter", "mv");
                    this.addCode(negation.render());
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
                if (type.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                    ST negation = this.llvmGroup.getInstanceOf("negVector");
                    this.addCode(negation.render());
                }
                else {
                    ST negation = this.llvmGroup.getInstanceOf("negation");
                    negation.add("typeLetter", "bv");
                    this.addCode(negation.render());
                }
                return type;
            }
            else if (ctx.As() == null) {
                // CASE: parenthesis
                return type;
            }
            else {
                // CASE: casting case
                if (ctx.type() != null) {
                    Type newType = this.visitType(ctx.type());
                    if (newType.getCollection_type() == Type.COLLECTION_TYPES.VECTOR) {
                        this.visit(ctx.sizeData());
                        ST promoteVector = llvmGroup.getInstanceOf("promoteVector");
                        promoteVector.add("value", getLetterForType(newType));
                        this.addCode(promoteVector.render());
                    } else if (newType.getCollection_type() == Type.COLLECTION_TYPES.MATRIX) {

                    } else {
                        String typeLetter = Type.getCastingFunction(type, newType);
                        ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
                        promoteCall.add("typeLetter", typeLetter);
                        this.addCode(promoteCall.render());
                    }
                    return newType;
                }
                // TODO tuple case, NEED TO POP PREVIOUS EXPRESSION OFF STACK
                else if (ctx.tupleTypeDetails() != null) {
                    Type newType = this.visitTupleTypeDetails(ctx.tupleTypeDetails());

                    ST swapStack = this.llvmGroup.getInstanceOf("swapStack");
                    this.addCode(swapStack.render());

                    ST promoteTuple = llvmGroup.getInstanceOf("promoteTuple");
                    this.addCode(promoteTuple.render());

                    return newType;
                }
                else{
                    throw new Error("Cannot cast type");
                }
            }
        }
        else if (ctx.generator() != null) {
            return this.visitGenerator(ctx.generator());
        }
        else if (ctx.filter() != null) {
            return this.visitFilter(ctx.filter());
        }
        else if (ctx.functionCall() != null) {
            return this.visitFunctionCall(ctx.functionCall());
        }
        else if (ctx.expression()!= null && ctx.expression().size() == 2 && ctx.op != null && ctx.op.getText().equals("..")){
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
            String operator = null;
            String typeLetter = null;
            if (ctx.op != null){
                operator = ctx.op.getText();
                typeLetter = Type.getResultFunction(left, right);
                if (!(typeLetter.equals("skip")) && !typeLetter.equals("tuple") && !typeLetter.equals("tv")) {
                    for (int i = 0; i < 2; i++) {
                        ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
                        promoteCall.add("typeLetter", typeLetter);
                        this.addCode(promoteCall.render());
                        ST swapStack = this.llvmGroup.getInstanceOf("swapStack");
                        this.addCode(swapStack.render());
                    }
                }
            }
            if (operator == null){
                // TODO INDEXING ON MATRIX
                if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR) {
                    ST operatorCall = this.llvmGroup.getInstanceOf("indexVector");
                    this.addCode(operatorCall.render());
                    if(right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getType() == Type.TYPES.INTERVAL){
                        return new Type(Type.SPECIFIERS.VAR, left.getType(), Type.COLLECTION_TYPES.VECTOR);
                    }
                    else {
                        return new Type(Type.SPECIFIERS.VAR, left.getType());
                    }
                }
                else if (left.getCollection_type() == Type.COLLECTION_TYPES.MATRIX){
                    // left is the matrix
                    // right is the row
                    // column is the column
                    // this is the way they appear on the stack
                    if (ctx.expression(2) != null){
                        Type column = (Type)visit(ctx.expression(2));
                        ST operatorCall = this.llvmGroup.getInstanceOf("indexMatrix");
                        this.addCode(operatorCall.render());

                        if((right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getType() == Type.TYPES.INTERVAL) &&
                                (column.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || column.getType() == Type.TYPES.INTERVAL)){
                            return new Type(Type.SPECIFIERS.VAR, left.getType());

                        }
                        else {
                            return new Type(Type.SPECIFIERS.VAR, left.getType(), Type.COLLECTION_TYPES.VECTOR);
                        }
                    }
                }
                else {
                    throw new Error("Cannot index:" + left.getCollection_type().toString());
                }
            }
            ST operatorCall;
            switch(operator) {
                // CASE: concat
                case "||":
                    // TODO
                    operatorCall = this.llvmGroup.getInstanceOf("concatVector");
                    this.addCode(operatorCall.render());
                    return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                // CASE: OR
                case "or":
                    if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("logicalorVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    }
                    else {
                        operatorCall = this.llvmGroup.getInstanceOf("logicalor");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType("bv");
                    }
                // CASE: XOR
                case "xor":
                    if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("logicalxorVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    } else {
                        operatorCall = this.llvmGroup.getInstanceOf("logicalxor");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType("bv");
                    }
                // CASE: AND
                case "and":
                    if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("logicalandVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    } else {
                        operatorCall = this.llvmGroup.getInstanceOf("logicaland");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType("bv");
                    }
                // CASE: ==
                case "==":
                    // TODO TUPLE
                    // Interval case
                    if (right.getType() == Type.TYPES.INTERVAL && left.getType() == Type.TYPES.INTERVAL){
                        operatorCall = this.llvmGroup.getInstanceOf("equalInterval");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.BOOLEAN);
                    }
                    // Tuple Case
                    else if (left.getType() == Type.TYPES.TUPLE && right.getType() == Type.TYPES.TUPLE){
                        operatorCall = this.llvmGroup.getInstanceOf("equalTuple");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.BOOLEAN);
                    }
                    // Vector case
                    else if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("equalVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
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
                    // Tuple Case
                    else if (left.getType() == Type.TYPES.TUPLE && right.getType() == Type.TYPES.TUPLE){
                        operatorCall = this.llvmGroup.getInstanceOf("notequalTuple");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.BOOLEAN);
                    }
                    // Vector case
                    else if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("notequalVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    }
                    else {
                        operatorCall = this.llvmGroup.getInstanceOf("notequal");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType("bv");
                    }

                // CASE: <
                case "<":
                    if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("lessthanVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    } else {
                        operatorCall = this.llvmGroup.getInstanceOf("lessthan");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType("bv");
                    }
                // CASE: <=
                case "<=":
                    if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("lessthanequalVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    } else {
                        operatorCall = this.llvmGroup.getInstanceOf("lessthanequal");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType("bv");
                    }
                // CASE: >
                case ">":
                    if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("greaterthanVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    } else {
                        operatorCall = this.llvmGroup.getInstanceOf("greaterthan");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType("bv");
                    }
                // CASE: >=
                case ">=":
                    if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("greaterthanequalVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    } else {
                        operatorCall = this.llvmGroup.getInstanceOf("greaterthanequal");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType("bv");
                    }
                // CASE: by
                case "by":
                    // TODO make sure proper type is returned
                    if (left.getType().equals(Type.TYPES.INTERVAL)) {
                        String offset = ctx.expression(1).getText();
                        if (Integer.valueOf(offset) <= 0) {
                            throw new Error("Offset is lower than 1");
                        }
                        operatorCall = this.llvmGroup.getInstanceOf("byInterval");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTEGER, Type.COLLECTION_TYPES.VECTOR);
                    }
                    else {
                        // vector case
                        String offset = ctx.expression(1).getText();
                        if (Integer.valueOf(offset) <= 0) {
                            throw new Error("Offset is lower than 1");
                        }
                        operatorCall = this.llvmGroup.getInstanceOf("byVector");
                        this.addCode(operatorCall.render());
                    }
                    return new Type(Type.SPECIFIERS.VAR, left.getType(), Type.COLLECTION_TYPES.VECTOR);
                // CASE: +
                case "+":
                    // Interval case
                    if (right.getType() == Type.TYPES.INTERVAL && left.getType() == Type.TYPES.INTERVAL){
                        operatorCall = this.llvmGroup.getInstanceOf("addInterval");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
                    }
                    // Vector case
                    else if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("addVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
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
                    // Vector case
                    else if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("subVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
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
                    if (right.getCollection_type() == Type.COLLECTION_TYPES.MATRIX && right.getCollection_type() == Type.COLLECTION_TYPES.MATRIX){

                    }
                    else if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR && right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR) {
                        operatorCall = this.llvmGroup.getInstanceOf("dotProduct");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter);
                    }
                    else {
                        throw new Error("Incompatible types");
                    }
                // CASE: *
                case "*":
                    // Interval case
                    if (right.getType() == Type.TYPES.INTERVAL && left.getType() == Type.TYPES.INTERVAL){
                        operatorCall = this.llvmGroup.getInstanceOf("multInterval");
                        this.addCode(operatorCall.render());
                        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTERVAL);
                    }
                    // Vector case
                    else if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("multVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
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
                    // Vector case
                    else if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("divVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    }
                    else {
                        operatorCall = this.llvmGroup.getInstanceOf("division");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter);
                    }
                // CASE: %
                case "%":
                    if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("modVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    }
                    else {
                        operatorCall = this.llvmGroup.getInstanceOf("modulus");
                        operatorCall.add("typeLetter", typeLetter);
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter);
                    }
                // CASE: ^
                case "^":
                    if (left.getCollection_type() == Type.COLLECTION_TYPES.VECTOR || right.getCollection_type() == Type.COLLECTION_TYPES.VECTOR){
                        operatorCall = this.llvmGroup.getInstanceOf("expVector");
                        this.addCode(operatorCall.render());
                        return Type.getReturnType(typeLetter, Type.COLLECTION_TYPES.VECTOR);
                    }
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
            return this.visitStringLiteral(ctx.StringLiteral());
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

    @Override public Type visitGenerator(GazpreaParser.GeneratorContext ctx) {
        if (ctx.expression().size() == 3){
            // push a start vector for starting matrix
            ST startVector = this.llvmGroup.getInstanceOf("startVector");
            this.currentFunction.addLine(startVector.render());

            this.scope.pushScope();
            // init inner loop variable too
            String variableName2 = ctx.Identifier().get(1).getText(); // get the first identifier name
            Variable variable2 = new Variable(variableName2, this.mangleVariableName(variableName2), new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTEGER));

            if (this.currentFunction != null) {
                ST varLine = this.llvmGroup.getInstanceOf("localVariable");
                varLine.add("name", variable2.getMangledName());
                this.addCode(varLine.render());
            } else {
                this.variables.put(variableName2, variable2);
            }

            this.scope.initVariable(variableName2, variable2);

            ST initLine2 = this.llvmGroup.getInstanceOf("varInit_" + variable2.getType().getTypeLLVMString());
            this.currentFunction.addLine(initLine2.render());

            ST initAssign2 = this.llvmGroup.getInstanceOf("assignVariable");
            initAssign2.add("name", variable2.getMangledName());
            this.currentFunction.addLine((initAssign2.render()));

            // outer loop
            // first get the vector we need to iterate through
            Type type = this.visitExpression(ctx.expression().get(0)); // push the expression to stack and call function to iterate through it
            // convert to vector
            ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
            promoteCall.add("typeLetter", "vv");
            this.currentFunction.addLine((promoteCall.render()));
            // vector we need to iterate through is now on the stack
            String variableName = ctx.Identifier().get(0).getText(); // get the first identifier name

            if (type.getType() == Type.TYPES.INTERVAL){
                type.setType(Type.TYPES.INTEGER);
            }

            Variable variable = new Variable(variableName, this.mangleVariableName(variableName), new Type(Type.SPECIFIERS.VAR, type.getType()));

            if (this.currentFunction != null) {
                ST varLine = this.llvmGroup.getInstanceOf("localVariable");
                varLine.add("name", variable.getMangledName());
                this.addCode(varLine.render());
            } else {
                this.variables.put(variableName, variable);
            }

            this.scope.initVariable(variableName, variable);

            ST initLine = this.llvmGroup.getInstanceOf("varInit_" + variable.getType().getTypeLLVMString());
            this.currentFunction.addLine(initLine.render());

            ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
            initAssign.add("name", variable.getMangledName());
            this.currentFunction.addLine((initAssign.render()));

            ++this.loopIndex;

            int myLoopIndex = this.loopIndex;

            ST loopBegin = this.llvmGroup.getInstanceOf("loopStart");
            loopBegin.add("index", myLoopIndex);
            this.currentFunction.addLine(loopBegin.render());

            // where the magic should happen
            // get vector size
            // if size > 1, get first element, make new vector one element shorter
            // need to push to stack (in order of): new vector, i value
            // else if vector size is one element: push i value, startvector
            // get new value on stack: new value, startvector
            // swap on stack: startvector, new value
            // copy top of stack: startvector, startvector, new value
            // copied to check conditional, new value should remain on stack

            ST shrink = this.llvmGroup.getInstanceOf("shrinkIterateVectorGen");
            this.currentFunction.addLine(shrink.render());

            ST assignIterator = this.llvmGroup.getInstanceOf("assignByVar");
            assignIterator.add("name", variable.getMangledName());
            this.currentFunction.addLine(assignIterator.render());

            // INNER LOOP GOES HERE
            this.currentFunction.addLine(startVector.render());

            Type type2 = this.visitExpression(ctx.expression().get(1)); // push the expression to stack and call function to iterate through it
            // convert to vector
            this.currentFunction.addLine(promoteCall.render());
            // vector we need to iterate through is now on the stack
            if (type2.getType() == Type.TYPES.INTERVAL){
                type2.setType(Type.TYPES.INTEGER);
            }
            this.scope.getVariable(variable2.getName()).setType(type2);

            ++this.loopIndex;

            int myLoopIndex2 = this.loopIndex;

            ST loopBegin2 = this.llvmGroup.getInstanceOf("loopStart");
            loopBegin2.add("index", myLoopIndex2);
            this.currentFunction.addLine(loopBegin2.render());

            this.currentFunction.addLine(shrink.render());

            ST assignIterator2 = this.llvmGroup.getInstanceOf("assignByVar");
            assignIterator2.add("name", variable2.getMangledName());
            this.currentFunction.addLine(assignIterator2.render());

            Type type3 = this.visitExpression(ctx.expression().get(2));

            ST unwrap = this.llvmGroup.getInstanceOf("unwrap");
            this.currentFunction.addLine(unwrap.render());

            ST swapStack = this.llvmGroup.getInstanceOf("swapStack");
            this.currentFunction.addLine(swapStack.render());

            ST copyStack = this.llvmGroup.getInstanceOf("copyStack");
            this.currentFunction.addLine(copyStack.render());

            ST condition = this.llvmGroup.getInstanceOf("notEqualNull");
            this.currentFunction.addLine((condition.render()));

            ST loopConditional2 = this.llvmGroup.getInstanceOf("loopConditional");
            loopConditional2.add("index", myLoopIndex2);
            this.currentFunction.addLine(loopConditional2.render());

            ST endLoop2 = this.llvmGroup.getInstanceOf("loopEnd");
            endLoop2.add("index", myLoopIndex2);
            this.currentFunction.addLine(endLoop2.render());

            ST loopConditionalEnd2 = this.llvmGroup.getInstanceOf("loopConditionalEnd");
            loopConditionalEnd2.add("index", myLoopIndex2);
            this.currentFunction.addLine(loopConditionalEnd2.render());

            ST popStack = this.llvmGroup.getInstanceOf("popStack");
            this.currentFunction.addLine(popStack.render());

            ST endVector = this.llvmGroup.getInstanceOf("endVector");
            this.currentFunction.addLine(endVector.render());

            // end inner loop

            this.currentFunction.addLine(swapStack.render());

            this.currentFunction.addLine(copyStack.render());

            this.currentFunction.addLine(condition.render());

            ST loopConditional = this.llvmGroup.getInstanceOf("loopConditional");
            loopConditional.add("index", myLoopIndex);
            this.currentFunction.addLine(loopConditional.render());

            ST endLoop = this.llvmGroup.getInstanceOf("loopEnd");
            endLoop.add("index", myLoopIndex);
            this.currentFunction.addLine(endLoop.render());

            ST loopConditionalEnd = this.llvmGroup.getInstanceOf("loopConditionalEnd");
            loopConditionalEnd.add("index", myLoopIndex);
            this.currentFunction.addLine(loopConditionalEnd.render());

            this.currentFunction.addLine(popStack.render());

            this.scope.popScope();

            ST endMatrix = this.llvmGroup.getInstanceOf("endMatrix");
            this.currentFunction.addLine(endMatrix.render());

            return new Type(Type.SPECIFIERS.VAR, type3.getType(), Type.COLLECTION_TYPES.MATRIX);
        }
        else if (ctx.expression().size() == 2) {
            // push a start vector for starting vector
            ST startVector = this.llvmGroup.getInstanceOf("startVector");
            this.currentFunction.addLine(startVector.render());

            // first get the vector we need to iterate through
            Type type = this.visitExpression(ctx.expression().get(0)); // push the expression to stack and call function to iterate through it

            // convert to vector
            ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
            promoteCall.add("typeLetter", "vv");
            this.currentFunction.addLine((promoteCall.render()));

            // vector we need to iterate through is now on the stack

            // next we initiate the variable that we are going to assign to for each iteration of the vector
            this.scope.pushScope();
            String variableName = ctx.Identifier().get(0).getText(); // get the first identifier name

            if (type.getType() == Type.TYPES.INTERVAL){
                type.setType(Type.TYPES.INTEGER);
            }

            Variable variable = new Variable(variableName, this.mangleVariableName(variableName), new Type(Type.SPECIFIERS.VAR, type.getType()));

            if (this.currentFunction != null) {
                ST varLine = this.llvmGroup.getInstanceOf("localVariable");
                varLine.add("name", variable.getMangledName());
                this.addCode(varLine.render());
            } else {
                this.variables.put(variableName, variable);
            }

            this.scope.initVariable(variableName, variable);

            ST initLine = this.llvmGroup.getInstanceOf("varInit_" + variable.getType().getTypeLLVMString());
            this.currentFunction.addLine(initLine.render());

            ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
            initAssign.add("name", variable.getMangledName());
            this.currentFunction.addLine((initAssign.render()));

            // variable is now taken care of
            // next part is a work of black magic

            ++this.loopIndex;

            int myLoopIndex = this.loopIndex;

            ST loopBegin = this.llvmGroup.getInstanceOf("loopStart");
            loopBegin.add("index", myLoopIndex);
            this.currentFunction.addLine(loopBegin.render());

            // where the magic should happen
            // get vector size
            // if size > 1, get first element, make new vector one element shorter
            // need to push to stack (in order of): new vector, i value
            // else if vector size is one element: push i value, startvector
            // get new value on stack: new value, startvector
            // swap on stack: startvector, new value
            // copy top of stack: startvector, startvector, new value
            // copied to check conditional, new value should remain on stack

            ST shrink = this.llvmGroup.getInstanceOf("shrinkIterateVectorGen");
            this.currentFunction.addLine(shrink.render());

            ST assignIterator = this.llvmGroup.getInstanceOf("assignByVar");
            assignIterator.add("name", variable.getMangledName());
            this.currentFunction.addLine(assignIterator.render());

            this.visitExpression(ctx.expression().get(1));

	        ST unwrap = this.llvmGroup.getInstanceOf("unwrap");
            this.currentFunction.addLine(unwrap.render());

            ST swapStack = this.llvmGroup.getInstanceOf("swapStack");
            this.currentFunction.addLine(swapStack.render());

            ST copyStack = this.llvmGroup.getInstanceOf("copyStack");
            this.currentFunction.addLine(copyStack.render());

            ST condition = this.llvmGroup.getInstanceOf("notEqualNull");
            this.currentFunction.addLine((condition.render()));

            ST loopConditional = this.llvmGroup.getInstanceOf("loopConditional");
            loopConditional.add("index", myLoopIndex);
            this.currentFunction.addLine(loopConditional.render());

            ST endLoop = this.llvmGroup.getInstanceOf("loopEnd");
            endLoop.add("index", myLoopIndex);
            this.currentFunction.addLine(endLoop.render());

            ST loopConditionalEnd = this.llvmGroup.getInstanceOf("loopConditionalEnd");
            loopConditionalEnd.add("index", myLoopIndex);
            this.currentFunction.addLine(loopConditionalEnd.render());

            ST popStack = this.llvmGroup.getInstanceOf("popStack");
            this.currentFunction.addLine(popStack.render());

            // finalize the vector!
            ST endVector = this.llvmGroup.getInstanceOf("endVector");
            this.currentFunction.addLine(endVector.render());

            this.scope.popScope();
            return new Type(Type.SPECIFIERS.VAR, type.getType(), Type.COLLECTION_TYPES.VECTOR);
        }
        else {
            throw new Error("Invalid generator type");
        }
    }

    @Override public Type visitFilter(GazpreaParser.FilterContext ctx) {
        // first get the vector we need to iterate through
        Type type = this.visitExpression(ctx.expression()); // push the expression to stack and call function to iterate through it

        // convert to vector
        ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
        promoteCall.add("typeLetter", "vv");
        this.currentFunction.addLine((promoteCall.render()));

        // duplicate vector so last one is to iterate through
        ST copyStack = this.llvmGroup.getInstanceOf("copyStack");
        this.currentFunction.addLine(copyStack.render());

        // vector we need to iterate through is now on the stack
        // next we initiate the variable that we are going to assign to for each iteration of the vector
        this.scope.pushScope();
        String variableName = ctx.Identifier().getText(); // get the first identifier name

        if (type.getType() == Type.TYPES.INTERVAL){
            type.setType(Type.TYPES.INTEGER);
        }

        Variable variable = new Variable(variableName, this.mangleVariableName(variableName), new Type(Type.SPECIFIERS.VAR, type.getType()));

        if (this.currentFunction != null) {
            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", variable.getMangledName());
            this.addCode(varLine.render());
        } else {
            this.variables.put(variableName, variable);
        }

        this.scope.initVariable(variableName, variable);

        ST initLine = this.llvmGroup.getInstanceOf("varInit_" + variable.getType().getTypeLLVMString());
        this.currentFunction.addLine(initLine.render());

        ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
        initAssign.add("name", variable.getMangledName());
        this.currentFunction.addLine(initAssign.render());

        // now we will loop through each of these predicate thingies and they will push a tuple onto the stack
        this.visitPredicate(ctx.predicate(), variable);

        this.scope.popScope();

        // finally find the things that aren't in the vector and return tuple onto the stack
        ST getAdd = this.llvmGroup.getInstanceOf("getAddFilter");
        this.currentFunction.addLine(getAdd.render());

        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.TUPLE);
    }

    // function overloading
    private Type visitPredicate(GazpreaParser.PredicateContext ctx, Variable variable) {
        int size = ctx.filterexpression().size();

        ST startVector = this.llvmGroup.getInstanceOf("startVector");
        this.currentFunction.addLine(startVector.render());

        ST swapStack = this.llvmGroup.getInstanceOf("swapStack");
        this.currentFunction.addLine(swapStack.render());

        ST copyStack = this.llvmGroup.getInstanceOf("copyStack");
        for (int i = 0; i < size; i++){
            // duplicate vector to iterate through it
            this.currentFunction.addLine(copyStack.render());
        }

        for (int i = 0; i < size; i++){
            // create the loops
            ++this.loopIndex;

            int myLoopIndex = this.loopIndex;

            this.currentLoop.addFirst(myLoopIndex);

            // start vector for vector of values
            this.currentFunction.addLine(startVector.render());

            ST loopBegin = this.llvmGroup.getInstanceOf("loopStart");
            loopBegin.add("index", myLoopIndex);
            this.currentFunction.addLine(loopBegin.render());

            // where the magic should happen
            // get vector size
            // if size > 1, get first element, make new vector one element shorter
            // need to push to stack (in order of): new vector, i value
            // else if vector size is one element: push i value, startvector
            // get new value on stack: new value, startvector
            // swap on stack: startvector, new value

            ST shrink = this.llvmGroup.getInstanceOf("shrinkIterateVectorGen");
            this.currentFunction.addLine(shrink.render());

            ST assignIterator = this.llvmGroup.getInstanceOf("assignByVar");
            assignIterator.add("name", variable.getMangledName());
            this.currentFunction.addLine(assignIterator.render());

            this.visitFilterexpression(ctx.filterexpression(i));

            ST line = this.llvmGroup.getInstanceOf("pushVariable");
            line.add("name", variable.getMangledName());
            this.currentFunction.addLine(line.render());

            ST unwrap = this.llvmGroup.getInstanceOf("unwrap");
            this.currentFunction.addLine(unwrap.render());

            this.currentFunction.addLine(swapStack.render());

            // see if the stack has a true value, if true keep value on stack, otherwise pop stack again
            ST check = this.llvmGroup.getInstanceOf("notEqualFilter");
            this.currentFunction.addLine((check.render()));

            ST condition = this.llvmGroup.getInstanceOf("notEqualNull");
            this.currentFunction.addLine((condition.render()));

            ST loopConditional = this.llvmGroup.getInstanceOf("loopConditional");
            loopConditional.add("index", myLoopIndex);
            this.currentFunction.addLine(loopConditional.render());

            ST loopEnd = this.llvmGroup.getInstanceOf("loopEnd");
            loopEnd.add("index", myLoopIndex);
            this.currentFunction.addLine(loopEnd.render());

            ST loopConditionalEnd = this.llvmGroup.getInstanceOf("loopConditionalEnd");
            loopConditionalEnd.add("index", myLoopIndex);
            this.currentFunction.addLine(loopConditionalEnd.render());

            ST endVector = this.llvmGroup.getInstanceOf("endVector");
            this.addCode(endVector.render());

            this.currentLoop.removeFirst();

            // swap that vector with a iterator vector
            this.currentFunction.addLine(swapStack.render());
        }
        ST endTuple = this.llvmGroup.getInstanceOf("endTuple");
        this.addCode(endTuple.render());

        return null;
    }


    @Override public Type visitFilterexpression(GazpreaParser.FilterexpressionContext ctx) {
        // this will be a modified version of visit expression, it will be similar but not the same
        if (true) {
            throw new Error("Not completed yet");
        }
        return null;
    }


    @Override
    public Type visitTupleLiteral(GazpreaParser.TupleLiteralContext ctx) {
        Tuple tupleType = new Tuple();

        ST startVector = this.llvmGroup.getInstanceOf("startVector");
        this.addCode(startVector.render());

        for (int e = 0; e < ctx.expression().size(); ++e) {
            Type exprType = this.visitExpression(ctx.expression(e));
            tupleType.addField("" + (e + 1), exprType);
        }
        ST endTuple = this.llvmGroup.getInstanceOf("endTuple");
        this.addCode(endTuple.render());

        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.TUPLE, tupleType);
    }

    public Type visitStringLiteral(TerminalNode node) {
        String str = node.getText();
        str = str.substring(1, str.length() - 1);

        ST startVector = this.llvmGroup.getInstanceOf("startVector");
        this.addCode(startVector.render());

        for (int i = 0; i < str.length(); ++i) {
            if(str.charAt(i) == '\\') {
                int val;
                switch(str.charAt(i+1)) {
                    case 'a':
                        val = 7;
                        break;
                    case 'b':
                        val = '\b';
                        break;
                    case 'n':
                        val = '\n';
                        break;
                    case 'r':
                        val = '\r';
                        break;
                    case 't':
                        val = '\t';
                        break;
                    case '\\':
                        val = '\\';
                        break;
                    case '\'':
                        val = '\'';
                        break;
                    case '"':
                        val = '\"';
                        break;
                    case '0':
                        val = '\0';
                        break;
                    default:
                        throw new RuntimeException("Invalid escape sequence, brah\n");
                }
                ST pushCharacter = llvmGroup.getInstanceOf("pushCharacter");
                pushCharacter.add("value", (int) val);
                this.addCode(pushCharacter.render());
                ++i;
            } else {
                ST pushCharacter = llvmGroup.getInstanceOf("pushCharacter");
                pushCharacter.add("value", (int) str.charAt(i));
                this.addCode(pushCharacter.render());
            }
        }

        ST endVector = this.llvmGroup.getInstanceOf("endVector");
        this.addCode(endVector.render());

        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.CHARACTER, Type.COLLECTION_TYPES.VECTOR);
    }

    @Override
    public Type visitVectorLiteral(GazpreaParser.VectorLiteralContext ctx) {
        Integer size = ctx.expression().size();

        ST startVector = this.llvmGroup.getInstanceOf("startVector");
        this.addCode(startVector.render());

        // we are only concerned about implicitly promotable types i.e. integers, reals, nulls, identity
        // NOTE: a vector of nulls and identities will be returned as TYPES.NULL
        HashMap<Type.TYPES, Integer> elementTypeCounts = new HashMap<>();
        elementTypeCounts.put(Type.TYPES.REAL, 0);
        elementTypeCounts.put(Type.TYPES.INTEGER, 0);
        elementTypeCounts.put(Type.TYPES.BOOLEAN, 0);
        elementTypeCounts.put(Type.TYPES.CHARACTER, 0);
        elementTypeCounts.put(Type.TYPES.NULL, 0);
        elementTypeCounts.put(Type.TYPES.IDENTITY, 0);

        for (int expr = 0; expr < size; ++expr) {
            Type exprType = this.visitExpression(ctx.expression(expr));
            //if (elementTypeCounts.get(exprType.getType())!= null) {
                elementTypeCounts.put(exprType.getType(), elementTypeCounts.get(exprType.getType()) + 1);
            //}
        }
        ST endVector = this.llvmGroup.getInstanceOf("endVector");
        this.addCode(endVector.render());

        Type.TYPES highestRankType = Type.TYPES.NULL;
        Type.TYPES ranking[] = {Type.TYPES.REAL, Type.TYPES.INTEGER, Type.TYPES.BOOLEAN, Type.TYPES.CHARACTER, Type.TYPES.NULL, Type.TYPES.IDENTITY};
        for (int r = 0; r < ranking.length; ++r) {
            if (elementTypeCounts.get(ranking[r]) > 0) {
                highestRankType = ranking[r];
                break;
            }
        }

        Type type = new Type(Type.SPECIFIERS.VAR, highestRankType);

        // now we cast the vector!
        // pop stack first
        ST popStack = this.llvmGroup.getInstanceOf("popStack");
        this.addCode(popStack.render());

        this.addCode(startVector.render());

        for (int expr = 0; expr < ctx.expression().size(); ++expr) {
            Type exprType = this.visitExpression(ctx.expression(expr));
            String typeLetter = Type.getCastingFunction(exprType, type);
            ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
            promoteCall.add("typeLetter", typeLetter);
            this.addCode(promoteCall.render());
        }

        this.addCode(endVector.render());

        ST pushInteger = this.llvmGroup.getInstanceOf("pushInteger");
        pushInteger.add("value", size);
        this.addCode(pushInteger.render());
        ST setSizeData = this.llvmGroup.getInstanceOf("setVectorSize");
        this.addCode(setSizeData.render());

        // assign the contained type to the vector
        ST setVectorContainedType = this.llvmGroup.getInstanceOf("setVectorContainedType");
        int value;
        switch (highestRankType) {
            case BOOLEAN:
                value = 'b';
                break;
            case CHARACTER:
                value = 'c';
                break;
            case INTEGER:
                value = 'i';
                break;
            case REAL:
                value = 'r';
                break;
            case IDENTITY:
            case NULL:
                value ='n';
                break;
            default:
                throw new RuntimeException("Vector does not support this type");
        }

        setVectorContainedType.add("value", value);
        this.addCode(setVectorContainedType.render());

        return new Type(Type.SPECIFIERS.VAR, type.getType(), Type.COLLECTION_TYPES.VECTOR, size);
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

        this.scope.getLocalScopeVariables().forEach(pair -> {
            ST free = this.llvmGroup.getInstanceOf("freeVariable");
            free.add("name", pair.right().getMangledName());
            this.addCode(free.render());
        });

        ST line = this.llvmGroup.getInstanceOf("functionReturn");
        this.addCode(line.render());

        return null;
    }

    @Override
    public Object visitAssignment(GazpreaParser.AssignmentContext ctx) {
        // tuple assignment vs. regular assignment
        if (ctx.RealLiteral() != null || ctx.Dot() != null) {
            String varName = ctx.Identifier(0).getText();
            String field;

            if (ctx.RealLiteral() != null) {
                field = parseTupleAccessREAL(ctx.RealLiteral().getText());
            } else {
                field = ctx.Identifier(1).getText();
            }

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
        String functionName = this.visitFunctionName(ctx.functionName());

        if (Arrays.asList(BUILT_INS).contains(functionName)) {
            List<GazpreaParser.ExpressionContext> arguments = ctx.expression();
            Collections.reverse(arguments);
            List<Argument> callArgumentValues = new ArrayList<>();

            for (GazpreaParser.ExpressionContext argValue : arguments) {
                Type argType = this.visitExpression(argValue);
                Argument arg = new Argument(argType, null);
                callArgumentValues.add(arg);
            }

            // Special case for built in functions
            ST functionCall = this.llvmGroup.getInstanceOf("functionCall");

            Function refFunction = getReferringFunction(functionName, callArgumentValues);
            List<Argument> refFunctionArgs = refFunction.getArguments();

            functionCall.add("name", this.functionNameMappings.get(functionName));
            this.addCode(functionCall.render());

            List<String> varNames = ctx
                    .expression()
                    .stream()
                    .map(this::getVariableFromExpression)
                    .collect(Collectors.toList());

            Collections.reverse(arguments);
            Collections.reverse(varNames);

            zip.zip(refFunctionArgs, varNames, null, null).forEach(pair -> {
                if (pair.left().getType().getSpecifier().equals(Type.SPECIFIERS.VAR)) {
                    ST postCallAssign = this.llvmGroup.getInstanceOf("assignVariable");
                    postCallAssign.add("name", this.scope.getVariable(pair.right()).getMangledName());
                    this.addCode(postCallAssign.render());
                }
            });

            switch(functionName) {
                case "std_output":
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.OUTPUT_STREAM);
                case "std_input":
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INPUT_STREAM);
                case "stream_status":
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTEGER);
                case "length":
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTEGER);
                case "rows":
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTEGER);
                case "columns":
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.INTEGER);
                case "reverse":
                    return new Type(Type.SPECIFIERS.VAR, null, Type.COLLECTION_TYPES.VECTOR);
                default:
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.VOID);
            }
        }

        List<GazpreaParser.ExpressionContext> arguments = ctx.expression();
        Collections.reverse(arguments);
        List<Argument> callArgumentValues = new ArrayList<>();

        for (GazpreaParser.ExpressionContext argValue : arguments) {
            Type argType = this.visitExpression(argValue);
            Argument arg = new Argument(argType, null);
            callArgumentValues.add(arg);
        }

        ST functionCall = this.llvmGroup.getInstanceOf("functionCall");

        Function refFunction = getReferringFunction(functionName, callArgumentValues);
        List<Argument> refFunctionArgs = refFunction.getArguments();

        String mangledFunctionName = this.getFunctionLLVMName(refFunction);
        functionCall.add("name", mangledFunctionName);
        this.addCode(functionCall.render());

        List<String> varNames = ctx
                .expression()
                .stream()
                .map(this::getVariableFromExpression)
                .collect(Collectors.toList());

        Collections.reverse(arguments);
        Collections.reverse(varNames);

        zip.zip(refFunctionArgs, varNames, null, null).forEach(pair -> {
            if (pair.left().getType().getSpecifier().equals(Type.SPECIFIERS.VAR)) {
                ST postCallAssign = this.llvmGroup.getInstanceOf("assignVariable");
                postCallAssign.add("name", this.scope.getVariable(pair.right()).getMangledName());
                this.addCode(postCallAssign.render());
            }
        });


        return refFunction.getReturnType();

    }

    @Override
    public Type visitProcedureCall(GazpreaParser.ProcedureCallContext ctx) {
        return this.visitFunctionCall(ctx.functionCall());
    }

    @Override
    public Object visitConditional(GazpreaParser.ConditionalContext ctx) {
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

            ST initAssign = this.llvmGroup.getInstanceOf("assignVariable");
            initAssign.add("name", variable.getMangledName());
            this.currentFunction.addLine((initAssign.render()));
        }

        // go through everything except for innermost
        for (int i = 0; i < loopSize - 1; i++){
            Integer loopIndex = this.visitIteratorLoopVariable(ctx.iteratorLoopVariables().iteratorLoopVariable(i));
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

        return null;

    }

    @Override public Integer visitIteratorLoopVariable(GazpreaParser.IteratorLoopVariableContext ctx) {
        ++this.loopIndex;

        int myLoopIndex = this.loopIndex;

        // do declaration here
        String variableName = ctx.Identifier().getText();

        Type type = this.visitExpression(ctx.expression()); // push the expression to stack and call function to iterate through it

        this.scope.getVariable(variableName).setType(type);

        Variable variable = this.scope.getVariable(variableName);

        // convert to vector
        ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
        promoteCall.add("typeLetter", "vv");
        this.currentFunction.addLine((promoteCall.render()));

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

    private void processTrivialDeclaration(GazpreaParser.DeclarationContext ctx, Type lhsType) {
        // processes:
        // - boolean
        // - character
        // - integer
        // - real
        String variableName = ctx.Identifier().getText();

        if (ctx.expression() != null) {
            Type rhsType = this.visitExpression(ctx.expression());

            if (lhsType.getType() == Type.TYPES.NULL) {
                lhsType = rhsType;
            }
        } else {
            ST initLine;
            initLine = this.llvmGroup.getInstanceOf("varInit_" + lhsType.getTypeLLVMString());
            this.addCode(initLine.render());
        }

        Variable variable = new Variable(variableName, this.mangleVariableName(variableName), lhsType);

        if (this.currentFunction != null) {
            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", variable.getMangledName());
            this.addCode(varLine.render());
        } else {
            this.variables.put(variableName, variable);
        }

        if (lhsType.getType() != Type.TYPES.NULL) {
            ST initLine = this.llvmGroup.getInstanceOf("varInit_" + lhsType.getTypeLLVMString());
            this.addCode(initLine.render());

            ST line = this.llvmGroup.getInstanceOf("assignByVar");
            line.add("name", variable.getMangledName());
            this.addCode(line.render());
        }


        ST line = this.llvmGroup.getInstanceOf("assignByVar");
        line.add("name", variable.getMangledName());
        this.addCode(line.render());

        this.scope.initVariable(variableName, variable);
    }

    private int getLetterForType(Type input_type){
        switch (input_type.getType()) {
            case BOOLEAN: return 'b';
            case CHARACTER: return 'c';
            case INTEGER: return 'i';
            case REAL: return 'r';
            case IDENTITY:
            case NULL: return  'n';
            default:
                throw new RuntimeException("Vector does not support this type");
        }
    }

    private void processVectorDeclaration(GazpreaParser.DeclarationContext ctx, Type lhsType) {
        // processes:
        // - vectors of all types

        String variableName = ctx.Identifier().getText();

        // must be vector
        lhsType.setCollection_type(Type.COLLECTION_TYPES.VECTOR);

        ST popStack = this.llvmGroup.getInstanceOf("popStack");

        if (ctx.expression() != null) {

            ST startVector = this.llvmGroup.getInstanceOf("startVector");
            this.addCode(startVector.render());
            ST endVector = this.llvmGroup.getInstanceOf("endVector");
            this.addCode(endVector.render());

            // push size on stack for initializing vector
            if (ctx.sizeData() != null) {
                this.visitSizeData(ctx.sizeData());
            } else {
                ST pushInt = this.llvmGroup.getInstanceOf("pushInteger");
                pushInt.add("value", -1);
                this.addCode(pushInt.render());
            }
            ST setSizeData = this.llvmGroup.getInstanceOf("setVectorSize");
            this.addCode(setSizeData.render());

            ST setVectorContainedType = this.llvmGroup.getInstanceOf("setVectorContainedType");
            setVectorContainedType.add("value", getLetterForType(lhsType));
            this.addCode(setVectorContainedType.render());

            Type rhsType = this.visitExpression(ctx.expression());
            if (rhsType.getType() == Type.TYPES.INTERVAL) {
                ST promoteCall = this.llvmGroup.getInstanceOf("promoteTo");
                promoteCall.add("typeLetter", "vv");
                this.currentFunction.addLine((promoteCall.render()));
                rhsType.setType(Type.TYPES.INTEGER);
                rhsType.setCollection_type(Type.COLLECTION_TYPES.VECTOR);
            }
            if (rhsType.getType() == Type.TYPES.NULL && rhsType.getCollection_type() == null) {
                this.addCode(popStack.render());
                this.visitSizeData(ctx.sizeData());
                ST pushNullVector = this.llvmGroup.getInstanceOf("pushNullVector");
                pushNullVector.add("value", getLetterForType(lhsType));
                this.addCode(pushNullVector.render());
            }
            if (rhsType.getType() == Type.TYPES.IDENTITY && rhsType.getCollection_type() == null) {
                this.addCode(popStack.render());
                this.visitSizeData(ctx.sizeData());
                ST pushNullVector = this.llvmGroup.getInstanceOf("pushIdentityVector");
                pushNullVector.add("value", getLetterForType(lhsType));
                this.addCode(pushNullVector.render());
            }

            if (lhsType.getType() == Type.TYPES.NULL) {
                lhsType.setType(rhsType.getType());
            }

            this.addCode(this.llvmGroup.getInstanceOf("matchVectorSizes").render());
            this.addCode(this.llvmGroup.getInstanceOf("matchVectorTypes").render());

            // push size on stack for promotion
            if (ctx.sizeData() != null) {
                this.visitSizeData(ctx.sizeData());
            } else {
                ST pushInt = this.llvmGroup.getInstanceOf("pushInteger");
                pushInt.add("value", -1);
                this.addCode(pushInt.render());
            }
            ST promoteVector = this.llvmGroup.getInstanceOf("promoteVector");
            promoteVector.add("value", getLetterForType(lhsType));
            this.addCode(promoteVector.render());

            ST padVector = this.llvmGroup.getInstanceOf("padVector");
            this.addCode(padVector.render());
        } else {
            // case without rhs
            this.visitSizeData(ctx.sizeData());
            ST pushNullVector = this.llvmGroup.getInstanceOf("pushNullVector");
            pushNullVector.add("value", getLetterForType(lhsType));
            this.addCode(pushNullVector.render());
        }

        Variable variable = new Variable(variableName, this.mangleVariableName(variableName), lhsType);

        if (this.currentFunction != null) {
            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", variable.getMangledName());
            this.addCode(varLine.render());
        } else {
            this.variables.put(variableName, variable);
        }

        ST line = this.llvmGroup.getInstanceOf("assignByVar");
        line.add("name", variable.getMangledName());
        this.addCode(line.render());

        this.scope.initVariable(variableName, variable);
    }

    private void processIntervalDeclaration(GazpreaParser.DeclarationContext ctx, Type lhsType) {
        ST popStack = this.llvmGroup.getInstanceOf("popStack");

        String variableName = ctx.Identifier().getText();

        if (ctx.expression() != null) {
            Type rhsType = this.visitExpression(ctx.expression());

            if (rhsType.getType() == Type.TYPES.NULL) {
                // continue
            } else if (rhsType.getType() == Type.TYPES.IDENTITY) {
                this.addCode(popStack.render());

                ST pushIdentity = this.llvmGroup.getInstanceOf("pushIdentity");
                this.addCode(pushIdentity.render());

                ST promoteTo = this.llvmGroup.getInstanceOf("promoteTo");
                promoteTo.add("typeLetter", "lv");
                this.addCode(promoteTo.render());
            }
        }

        Variable variable = new Variable(variableName, this.mangleVariableName(variableName), lhsType);

        if (this.currentFunction != null) {
            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", variable.getMangledName());
            this.addCode(varLine.render());
        } else {
            this.variables.put(variableName, variable);
        }

        ST line = this.llvmGroup.getInstanceOf("assignByVar");
        line.add("name", variable.getMangledName());
        this.addCode(line.render());

        this.scope.initVariable(variableName, variable);
    }

    private void processMatrixDeclaration(GazpreaParser.DeclarationContext ctx, Type lhsType) {

    }

    private void processTupleDeclaration(GazpreaParser.DeclarationContext ctx, Type lhsType) {
        // processes:
        // - tuples of all types
        String variableName = ctx.Identifier().getText();

        ST popStack = this.llvmGroup.getInstanceOf("popStack");

        if (ctx.expression() != null) {
            Type rhsType = this.visitExpression(ctx.expression());

            if (rhsType.getType() == Type.TYPES.NULL) {
                this.addCode(popStack.render());
            } else if (rhsType.getType() == Type.TYPES.IDENTITY) {
                this.addCode(popStack.render());
                ST pushIdentityTuple = this.llvmGroup.getInstanceOf("pushIdentityTuple");
                this.addCode(pushIdentityTuple.render());
            } else {
                // This is a regular old tuple and this is where the casting needs to be done
                ST promoteTuple = this.llvmGroup.getInstanceOf("promoteTuple");
                this.addCode(promoteTuple.render());
            }
        }

        Variable variable = new Variable(variableName, this.mangleVariableName(variableName), lhsType);

        if (this.currentFunction != null) {
            ST varLine = this.llvmGroup.getInstanceOf("localVariable");
            varLine.add("name", variable.getMangledName());
            this.addCode(varLine.render());
        } else {
            this.variables.put(variableName, variable);
        }

        ST line = this.llvmGroup.getInstanceOf("assignByVar");
        line.add("name", variable.getMangledName());
        this.addCode(line.render());

        this.scope.initVariable(variableName, variable);
    }

    @Override
    public Object visitDeclaration(GazpreaParser.DeclarationContext ctx) {
        if (ctx.typedef() != null){
            visit(ctx.typedef());
            return  null;
        }

        Type lhsType = this.visitType(ctx.type());

        if (lhsType.getCollection_type() != null
                || lhsType.getType() != Type.TYPES.NULL
                || ctx.sizeData() != null) {
            // process when left hand side gives at least a little information (partial or total statically typed)
            if ((ctx.sizeData() != null && ctx.sizeData().Asteriks().size() + ctx.sizeData().expression().size() > 1)
                    || lhsType.getCollection_type() == Type.COLLECTION_TYPES.MATRIX) {
            } else if (ctx.sizeData() != null
                    || lhsType.getCollection_type() == Type.COLLECTION_TYPES.VECTOR) {
                processVectorDeclaration(ctx, lhsType);
            } else if (lhsType.getType() == Type.TYPES.TUPLE) {
                processTupleDeclaration(ctx, lhsType);
            } else if (lhsType.getType() == Type.TYPES.INTERVAL) {
                processIntervalDeclaration(ctx, lhsType);
            } else {
                processTrivialDeclaration(ctx, lhsType);
            }
        } else {
            // process when left hand side gives no information (total inferred type)
            String variableName = ctx.Identifier().getText();

            lhsType = this.visitExpression(ctx.expression());
            Variable variable = new Variable(variableName, this.mangleVariableName(variableName), lhsType);

            if (this.currentFunction != null) {
                ST varLine = this.llvmGroup.getInstanceOf("localVariable");
                varLine.add("name", variable.getMangledName());
                this.addCode(varLine.render());
            } else {
                this.variables.put(variableName, variable);
            }

            ST line = this.llvmGroup.getInstanceOf("assignByVar");
            line.add("name", variable.getMangledName());
            this.addCode(line.render());

            this.scope.initVariable(variableName, variable);
        }

        return null;
    }

    @Override
    public Pair<Type, TerminalNode> visitTupleTypeAtom(GazpreaParser.TupleTypeAtomContext ctx) {

        Type retType = this.visitType(ctx.type());

        if (ctx.sizeData() != null) {
            if (ctx.sizeData().expression().size() == 1) {
                // Vector case;
                retType.setCollection_type(Type.COLLECTION_TYPES.VECTOR);
                String lengthString = ctx.sizeData().expression(0).getText().replaceAll("_", "");
                retType.setVectorSize(Integer.parseInt(lengthString));
            } else {
                // Matrix case;
                retType.setCollection_type(Type.COLLECTION_TYPES.MATRIX);
                String lengthString = ctx.sizeData().expression(0).getText().replaceAll("_", "");
                String heightString = ctx.sizeData().expression(1).getText().replaceAll("_", "");
                Pair<Integer, Integer> dimensions = new Pair<>(Integer.parseInt(lengthString), Integer.parseInt(heightString));
                retType.setMatrixDimensions(dimensions);
            }
        }


        return new Pair<>(retType, ctx.Identifier());
    }

    @Override
    public Type visitTupleTypeDetails(GazpreaParser.TupleTypeDetailsContext ctx) {
        Tuple tupleType = new Tuple();

        ST startVector = this.llvmGroup.getInstanceOf("startVector");
        this.addCode(startVector.render());

        promoteType.push(new ArrayList<>());

        for (int i = 0; i < ctx.tupleTypeAtom().size(); ++i) {
            Pair<Type, TerminalNode> atom = this.visitTupleTypeAtom(ctx.tupleTypeAtom().get(i));
            Type atomType = atom.left();
            promoteType.peek().add(i, atomType);
            if (atom.right() != null) {
                tupleType.addField(atom.right().getText(), atomType);
            } else {
                tupleType.addField("" + (i+1), atomType);
            }

            ST st;
            if (atomType.getCollection_type() == Type.COLLECTION_TYPES.VECTOR) {
                ST pushInt = this.llvmGroup.getInstanceOf("pushInteger");
                pushInt.add("value", atomType.getVectorSize());
                this.addCode(pushInt.render());

                ST pushNullVector = this.llvmGroup.getInstanceOf("pushNullVector");
                pushNullVector.add("value", getLetterForType(atomType));
                this.addCode(pushNullVector.render());
            } else if (atomType.getCollection_type() == Type.COLLECTION_TYPES.MATRIX){
                //TODO: PUSH NULL MATRIX REPRESENTING THIS TUPLE SLOT
            } else {
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
                    default:
                        throw new RuntimeException("Bad type in tuple");
                }
            }
        }
        ST endTuple = this.llvmGroup.getInstanceOf("endTuple");
        this.addCode(endTuple.render());

        return new Type(Type.SPECIFIERS.VAR, Type.TYPES.TUPLE, tupleType);
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
                    return new Type(Type.SPECIFIERS.VAR, Type.TYPES.CHARACTER, Type.COLLECTION_TYPES.VECTOR);
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
                case Type.strVECTOR:
                typeType = Type.COLLECTION_TYPES.VECTOR;
                break;
                case Type.strMATRIX:
                typeType = Type.COLLECTION_TYPES.MATRIX;
                break;
                case Type.strINTERVAL:
                    // create empty vector type
                    ST st = this.llvmGroup.getInstanceOf("varInit_integer");
                    this.addCode(st.render());
                    this.addCode(st.render());
                    ST endInterval = this.llvmGroup.getInstanceOf("endInterval");
                    this.addCode(endInterval.render());
                    typeName = Type.TYPES.INTERVAL;
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
    // This function pushes the size onto the stack and assigns to the vector on the stack
    public Void visitSizeData(GazpreaParser.SizeDataContext ctx) {
        // Matrix size data
        if (ctx.expression().size() + ctx.Asteriks().size() > 1) {




        // Vector size data
        } else {
            if (ctx.expression().size() > 0) {
                this.visitExpression(ctx.expression(0));
            } else {
                ST pushInt = this.llvmGroup.getInstanceOf("pushInteger");
                pushInt.add("value", -1);
                this.addCode(pushInt.render());
            }
        }

        return null;
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
