import java.util.ArrayList;

public class Type {
    public enum SPECIFIERS {
        VAR, CONST
    }

    public enum COLLECTION_TYPES {
        MATRIX, VECTOR, TUPLE, INTERVAL
    }
    public enum TYPES {
        BOOLEAN, INTEGER, REAL, CHARACTER, STRING
    }

    public static final String strBOOLEAN = "boolean", strINTEGER="integer", strREAL="real", strCHARACTER="character", strSTRING="string",
            strINTERVAL="interval", strVECTOR="vector", strMATRIX="matrix", strTUPLE="tuple", strVAR="var", strCONST="const";

    private SPECIFIERS specifier;
    private COLLECTION_TYPES collection_type;
    private TYPES type;

    // special augmentation for tuple types
    private ArrayList<Type> tupleTypes;


    // TODO: May need a refactor based on how structs (tuples) will be defined in C
    // collection type tuple constructor
    Type(SPECIFIERS specifier, TYPES type, COLLECTION_TYPES collection_type, ArrayList<Type> tupleTypes) {
        this(specifier, type, collection_type);
        this.tupleTypes = tupleTypes;
    }

    // non collection tuple constructor
    Type(SPECIFIERS specifier, TYPES type, ArrayList<Type> tupleTypes) {
        this(specifier, type, null, tupleTypes);
    }

    // collection type variables constructor
    Type(SPECIFIERS specifier, TYPES type, COLLECTION_TYPES collection_type) {
        this.specifier = specifier;
        this.type = type;
        this.collection_type = collection_type;
        this.tupleTypes = null;
    }

    // non collection type variables constructor
    Type(SPECIFIERS specifier, TYPES type) {
        this.specifier = specifier;
        this.type = type;
        this.collection_type = null;
    }

    public SPECIFIERS getSpecifier() {
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
                return strBOOLEAN;
            case INTEGER:
                return "integer";
            case REAL:
                return "real";
            case CHARACTER:
                return "character";
            case STRING:
                return "string";
            default:
                return "";
        }
    }
}