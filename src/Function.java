import java.util.ArrayList;
import java.util.List;

class Function {
    private String name;
    private List<Argument> arguments = new ArrayList<>();
    private Type returnType;
    private boolean defined = false;
    private boolean procedure = false;

    private List<String> lines = new ArrayList<>();

    Function(String name, List<Argument> arguments, Type returnType) {
        this.name = name;
        this.arguments = arguments;
        this.returnType = returnType;
    }

    Type getReturnType() {
        return this.returnType;
    }
    String getName() { return this.name; };

    void define() {
        this.defined = true;
    }
    void setProcedure() { this.procedure = true; }
    boolean isDefined() {
        return this.defined;
    }
    boolean isProcedure() { return this.procedure; }

    void setArguments(List<Argument> arguments) { this.arguments = arguments; }
    List<Argument> getArguments() { return this.arguments; }
    void addLine(String line) {
        this.lines.add(line);
    }

    String render() {
        return this.lines
                .stream()
                .reduce("", (lhs, rhs) -> lhs + "\n" + rhs);
    }

    // check if two functions have same argument types strictly
    // (matching function to function)
    public static boolean strictEquals(Function function1, Function function2) {
        if (function1 == null || function2 == null) {
            return function1 == null && function2 == null;
        }

        List<Argument> f1Args = function1.getArguments();
        List<Argument> f2Args = function2.getArguments();

        if (f2Args == null || f1Args == null) {
            return f1Args == null && f2Args == null;
        }

        if (f1Args.size() != f2Args.size()) {
            return false;
        }

        for (int a = 0; a < f1Args.size(); ++a) {
            if (!f1Args.get(a).getType().equals(f2Args.get(a).getType())) {
                return false;
            }
        }

        return true;
    }

    // check if two functions have same argument types loosely
    // (matching function call to function
    public static boolean looseEquals(List<Argument> callArguments, Function function1) {
        if (callArguments == null || function1 == null || function1.getArguments() == null) {
            return callArguments == null && (function1 == null || function1.getArguments() == null);
        }

        List<Argument> functionArgs = function1.getArguments();

        if (functionArgs.size() != callArguments.size()) {
            return false;
        }

        for (int a = 0; a < callArguments.size(); ++a) {
            if (callArguments.get(a).getType().looseEquals(functionArgs.get(a).getType()) ) {
                // Do nothing: this is the case where an input in the call matches a function argument
            } else {
                return false;
            }
        }

        return true;
    }
}