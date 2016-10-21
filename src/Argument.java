class Argument {
    private Type type;
    private String name;

    Argument(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    Type getType() { return this.type; }
    String getName() { return this.name; }
}