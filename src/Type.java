public class Type {
    private String specifier;
    private String name;
    private boolean vectorKeyword;

    Type(String specifier, String name, boolean vectorKeyword) {
        this.specifier = specifier;
        this.name = name;
        this.vectorKeyword = vectorKeyword;
    }

    String getSpecifier() {
        return this.specifier;
    }

    String getName() {
        return this.name;
    }

    boolean isVectorKeyword() {
        return this.vectorKeyword;
    }
}