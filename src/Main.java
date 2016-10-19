import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    public static void main(String[] args) throws Exception {
        String testFilePath = args[1];

        ANTLRFileStream input = new ANTLRFileStream(testFilePath);
        GazpreaLexer lex = new GazpreaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        GazpreaParser parser = new GazpreaParser(tokens);
        ParseTree tree = parser.compilationUnit();

        switch (args[0]) {
            case "llvm":
                GazpreaCompiler compiler = new GazpreaCompiler();
                compiler.visit(tree);
                break;
            default:
                break;
        }
    }
}