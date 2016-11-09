import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

public class Main {

    private static Boolean DEBUG = false;

    private static void printTokens(CommonTokenStream cts, GazpreaLexer lexer) {
        for (int i = 0; i < cts.size(); ++i) {
            System.out.print(lexer.getVocabulary().getSymbolicName(cts.get(i).getType()));
            System.out.print('[');
            System.out.print(cts.get(i).getText());
            System.out.println(']');
        }
    }

    public static void main(String[] args) throws Exception {



        if (DEBUG) {
            ANTLRInputStream input = new ANTLRInputStream(
                    "procedure main() returns integer {\n" +
                    "    null -> std_output();\n" +
                    "    identity -> std_output();\n" +
                    "    return 0;\n" +
                    "}");
            GazpreaLexer lex = new GazpreaLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lex);
            GazpreaParser parser = new GazpreaParser(tokens);
            ParseTree tree = parser.compilationUnit();

            GazpreaCompiler compiler = new GazpreaCompiler();
            compiler.visit(tree);
            //printTokens(tokens, lex);
        } else {
            String testFilePath = args[1];

            FileInputStream inputStream = new FileInputStream(testFilePath);

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            String file = result.toString("UTF-8");
            file = file.replace("..","$$");

            ANTLRInputStream input = new ANTLRInputStream(file);
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
}