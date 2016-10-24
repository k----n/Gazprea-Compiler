import java.util.ArrayList;

public class Type {
    public enum SPECIFIERS {
        UNDEFINED, VAR, CONST
    }

    public enum COLLECTION_TYPES {
        NONE, MATRIX, VECTOR, TUPLE, INTERVAL
    }
    public enum TYPES {
        BOOLEAN, INTEGER, REAL, CHARACTER, STRING, NULL, IDENTITY, VOID
    }

    public static final String strBOOLEAN = "boolean", strINTEGER="integer", strREAL="real", strCHARACTER="character", strSTRING="string",
            strINTERVAL="interval", strVECTOR="vector", strMATRIX="matrix", strTUPLE="tuple", strVAR="var", strCONST="const",
            strNULL="null", strIDENTITY="identity", strVOID="void";

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
        switch(this.type) {
            case BOOLEAN:   return strBOOLEAN;
            case INTEGER:   return strINTEGER;
            case REAL:      return strREAL;
            case CHARACTER: return strCHARACTER;
            case STRING:    return strSTRING;
            case NULL:      return "null";
            case IDENTITY:  return "identity";
            default:        return "";
        }
    }

    @Override
    // compares types by 2 main components
    // TODO: consider doing tuple types comparison as well
    public boolean equals(Object obj) {
        if (! (obj instanceof Type) ) {
            return false;
        }

        Type type = (Type) obj;

        if (this.type == null && type.getType() == null) {
            // continue
        } else if (this.type == null || type.getType() == null) {
            return false;
        } else if (this.type.equals(type.getType())) {
            // continue
        } else {
            return false;
        }

        /*
        if (this.specifier == null && type.getSpecifier() == null) {
            // continue
        } else if (this.specifier == null || type.getSpecifier() == null) {
            return false;
        } else if (this.specifier.equals(type.getSpecifier())) {
            // continue
        } else {
            return false;
        }
        */


        if (this.collection_type == null && type.getCollection_type() == null) {
            // continue
        } else if (this.collection_type == null || type.getCollection_type() == null) {
            return false;
        } else if (this.collection_type.equals(type.getCollection_type())) {
            // continue
        } else {
            return false;
        }

        return true;
    }
}