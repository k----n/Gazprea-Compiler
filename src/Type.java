public class Type {
    public enum COLLECTION_TYPES {
        MATRIX, VECTOR, TUPLE, INTERVAL
    }
    public enum TYPES {
        BOOLEAN, INTEGER, REAL, CHARACTER, STRING
    }

    private String specifier;
    private COLLECTION_TYPES collection_type;
    private TYPES type;

    // collection type variables
    Type(String specifier, TYPES type, COLLECTION_TYPES collection_type) {
        this.specifier = specifier;
        this.type = type;
        this.collection_type = collection_type;
    }

    // non collection type variables
    Type(String specifier, TYPES type) {
        this.specifier = specifier;
        this.type = type;
        this.collection_type = null;
    }

    public String getSpecifier() {
        return specifier;
    }

    public COLLECTION_TYPES getCollection_type() {
        return collection_type;
    }

    public TYPES getType() {
        return type;
    }

    public String getTypeLLVMString() {
        switch(type) {
            case BOOLEAN:
                return "boolean";
            case INTEGER:
                return "integer";
            case REAL:
                return "real";
            case CHARACTER:
                return "character";
            case STRING:
                return "string";
        }
    }
}