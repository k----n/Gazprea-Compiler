import java.util.ArrayList;

public class Type {
    public enum SPECIFIERS {
        UNDEFINED, VAR, CONST
    }

    public enum COLLECTION_TYPES {
        NONE, MATRIX, VECTOR, INTERVAL
    }
    public enum TYPES {
        TUPLE, BOOLEAN, INTEGER, REAL, CHARACTER, STRING, NULL, IDENTITY, VOID, OUTPUT_STREAM, INPUT_STREAM
    }

    public static final String strBOOLEAN = "boolean", strINTEGER="integer", strREAL="real", strCHARACTER="character", strSTRING="string",
            strINTERVAL="interval", strVECTOR="vector", strMATRIX="matrix", strTUPLE="tuple", strVAR="var", strCONST="const",
            strNULL="null", strIDENTITY="identity", strVOID="void", strOUT="output", strIN="input";

    // TABLE FOR IMPLICIT PROMOTION
    private static String[/*from*/][/*to*/] PROMOTION_TABLE =
            {/*  bool       int         char        real            NULL        IDNTY*/
    /*bool*/    {"noop",    "void",     "void",     "void",         "void",     "void"},
    /*int*/     {"void",    "noop",     "void",     "int_to_real",  "void",     "void"},
    /*char*/    {"void",    "void",     "noop",     "void",         "void",     "void"},
    /*real*/    {"void",    "void",     "void",     "noop",         "void",     "void"},
    /*NULL*/    {"nulBool", "nulInt",   "nulChar",  "nulReal",      "noop",     "void"},
    /*IDNTY*/   {"idBool",  "idInt",    "idChar",   "idReal",       "void",     "noop"}
            };

    private static String[/*from*/][/*to*/] CASTING_TABLE =
            {/*  bool           int             char                 real */
    /*bool*/    {"noop",        "bool_to_int",  "bool_to_char",      "void"},
    /*int*/     {"int_to_bool", "noop",         "int_to_char",       "int_to_real"},
    /*char*/    {"void",        "char_to_int",  "noop",              "char_to_real"},
    /*real*/    {"void",        "real_to_int",  "void",              "noop"},
            };

    private static String[] NULL_TABLE =
            /*bool          int         char            real*/
            {"null_bool",   "null_int", "null_char",    "null_real"};
    private static String[] ID_TABLE =
            /*bool          int         char            real*/
            {"id_bool",     "id_int",   "id_char",      "id_real"};


    private SPECIFIERS specifier;
    private COLLECTION_TYPES collection_type;
    private TYPES type;

    // special augmentation for tuple types
    private Tuple tupleType = null;

    // TODO: May need a refactor based on how structs (tuples) will be defined in C
    // collection type tuple constructor
    Type(SPECIFIERS specifier, TYPES type, COLLECTION_TYPES collection_type, Tuple tupleType) {
        this.specifier = specifier;
        this.type = type;
        this.collection_type = collection_type;
        this.tupleType = tupleType;
    }

    Type(SPECIFIERS specifier, TYPES type, Tuple tupleType) {
        this(specifier, type, null, tupleType);
    }

    // collection type variables constructor
    Type(SPECIFIERS specifier, TYPES type, COLLECTION_TYPES collection_type) {
        this(specifier, type, collection_type, null);
    }

    // non collection type variables constructor
    Type(SPECIFIERS specifier, TYPES type) {
        this(specifier, type, null, null);
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
            case OUTPUT_STREAM: return strOUT;
            case INPUT_STREAM: return strIN;
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

    private static Integer getTypeTableIndex(Type t) {
        switch(t.getType()) {
            case BOOLEAN: return 0;
            case INTEGER: return  1;
            case CHARACTER: return  2;
            case REAL: return 3;
            case NULL: return 4;
            case IDENTITY: return 5;
            default: throw new RuntimeException("Undefined type in table");
        }
    }

    public static String getNullFunction(Type t) {
        return NULL_TABLE[getTypeTableIndex(t)];
    }

    public static String getIdFunction(Type t) {
        return ID_TABLE[getTypeTableIndex(t)];
    }

    public static String getPromoteFunction(Type from, Type to) {
        int fromIndex = getTypeTableIndex(from);
        int toIndex = getTypeTableIndex(to);

        return PROMOTION_TABLE[fromIndex][toIndex];
    }

    public static String getCastingFunction(Type from, Type to) {
        int fromIndex = getTypeTableIndex(from);
        int toIndex = getTypeTableIndex(to);

        return CASTING_TABLE[fromIndex][toIndex];
    }
}