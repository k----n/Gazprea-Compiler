import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

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
                    "procedure println(integer x) {\n" +
                            "    var out = std_output();\n" +
                            "    x -> out;\n" +
                            "    '\\n' -> out;\n" +
                            "}\n" +
                            "\n" +
                            "procedure println(character x) {\n" +
                            "    var out = std_output();\n" +
                            "    x -> out;\n" +
                            "    '\\n' -> out;\n" +
                            "}\n" +
                            "\n" +
                            "procedure main() returns integer {\n" +
                            "    tuple(integer, character) s;\n" +
                            "    tuple(integer, character) t = null;\n" +
                            "    tuple(integer, character) u = identity;\n" +
                            "\n" +
                            "    call println(s.1);\n" +
                            "    call println(s.2);\n" +
                            "    call println(t.1);\n" +
                            "    call println(t.2);\n" +
                            "    call println(u.1);\n" +
                            "    call println(u.2);\n" +
                            "\n" +
                            "    return 0; \n" +
                            "}");
            GazpreaLexer lex = new GazpreaLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lex);
            GazpreaParser parser = new GazpreaParser(tokens);
            ParseTree tree = parser.compilationUnit();

            GazpreaCompiler compiler = new GazpreaCompiler();
            compiler.visit(tree);
            //printTokens(tokens, lex);
        } else {
			StringBuilder b = new StringBuilder();
			for (String arg : args) {
				b.append(arg);
				b.append(" ");
			}
			b.deleteCharAt(b.lastIndexOf(" "));
			String testFilePath = b.toString();
			
			ANTLRFileStream input = new ANTLRFileStream(testFilePath);
			GazpreaLexer lex = new GazpreaLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lex);
			GazpreaParser parser = new GazpreaParser(tokens);
			ParseTree tree = parser.compilationUnit();
			
			GazpreaCompiler compiler = new GazpreaCompiler();
			compiler.visit(tree);
        }
    }
}
