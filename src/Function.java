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
}