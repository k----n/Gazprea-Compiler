import java.util.ArrayList;
import java.util.List;

class Function {
    private String name;
    private List<String> arguments = new ArrayList<>();
    private String returnType;
    private boolean defined = false;
    private boolean procedure = false;

    private List<String> lines = new ArrayList<>();

    Function(String name, List<String> arguments, String returnType) {
        this.name = name;
        this.arguments = arguments;
        this.returnType = returnType;
    }

    void define() {
        this.defined = true;
    }
    void setProcedure() { this.procedure = true; }
    boolean isDefined() {
        return this.defined;
    }
    boolean isProcedure() { return this.procedure; }

    void addLine(String line) {
        this.lines.add(line);
    }

    String render() {
        return this.lines
                .stream()
                .reduce("", (lhs, rhs) -> lhs + "\n" + rhs);
    }
}