class Variable {
    private String name;
    private String mangledName;
    private Type type;

    Variable(String name, String mangledName, Type type) {
        this.name = name;
        this.mangledName = mangledName;
        this.type = type;
    }

    public void setType(Type type) {
        this.type = type;
    }
    public String getName() { return this.name; }
    public String getMangledName() { return this.mangledName; }
    public Type getType() { return this.type; }
}