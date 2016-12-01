import java.util.ArrayList;

public class Type {
    public enum SPECIFIERS {
        UNDEFINED, VAR, CONST
    }

    public enum COLLECTION_TYPES {
        MATRIX, VECTOR
    }
    public enum TYPES {
        TUPLE, BOOLEAN, INTEGER, REAL, CHARACTER, STRING, NULL, IDENTITY, VOID, OUTPUT_STREAM, INPUT_STREAM, INTERVAL
    }

    public static final String strBOOLEAN = "boolean", strINTEGER="integer", strREAL="real", strCHARACTER="character", strSTRING="string",
            strINTERVAL="interval", strVECTOR="vector", strMATRIX="matrix", strTUPLE="tuple", strVAR="var", strCONST="const",
            strNULL="null", strIDENTITY="identity", strVOID="void", strOUT="output", strIN="input";

    // TABLE FOR RESULT OF OPERATIONS
    // iv = integer, rv = real, bv = boolean -- 'v' is appended because llvm does that
    private static String[/*left*/][/*right*/] RESULT_TABLE =
            {/*  bool       int         char        real            NULL        IDNTY       TUPLE    INTERVAL*/
    /*bool*/    {"bv",      "void",     "void",     "void",         "bv",       "void",     "void",  "void"    },
    /*int*/     {"void",    "iv",       "void",     "rv",           "iv",       "void",     "void",  "void"    },
    /*char*/    {"void",    "void",     "cv",       "void",         "void",     "void",     "void",  "void"    },
    /*real*/    {"void",    "rv",       "void",     "rv",           "rv",       "void",     "void",  "void"    },
    /*NULL*/    {"bv",      "iv",       "void",     "rv",           "void",     "void",     "void",  "void"    },
    /*IDNTY*/   {"void",    "void",     "void",     "void",         "void",     "void",     "void",  "void"    },
    /*TUPLE*/   {"void",    "void",     "void",     "void",         "void",     "void",     "tuple", "void"    },
    /*INTERVAL*/{"void",    "skip",     "void",     "void",         "void",     "void",     "void",  "lv"      },
            };

    private static String[/*from*/][/*to*/] CASTING_TABLE =
            {/*  bool           int             char                 real      null     idty*/
    /*bool*/    {"bv",          "iv",           "cv",                "rv",   "void",    "void"},
    /*int*/     {"bv",          "iv",           "cv",                "rv",   "void",    "void"},
    /*char*/    {"bv",          "iv",           "cv",                "rv",   "void",    "void"},
    /*real*/    {"void",        "iv",           "void",              "rv",   "void",    "void"},
    /*null*/    {"bv",          "iv",           "cv",                "rv",   "void",    "void"},
    /*idty */   {"bv",          "iv",           "cv",                "rv",   "void",    "void"},
            };


    private SPECIFIERS specifier;
    private COLLECTION_TYPES collection_type;
    private TYPES type;

    // special augmentation for tuple types
    private Tuple tupleType = null;

    // special augmentation for vector types
    private Integer vectorSize = null;

    // special augmentation for Matrix types
    private Pair<Integer, Integer> matrixDimensions = null;

    // general Type constructor
    private Type(SPECIFIERS specifier, TYPES type, COLLECTION_TYPES collection_type, Tuple tupleType,
         Integer vectorSize, Pair<Integer, Integer> matrixDimensions) {
        this.specifier = specifier;
        this.type = type;
        this.collection_type = collection_type;
        this.tupleType = tupleType;
        this.vectorSize = vectorSize;
        this.matrixDimensions = matrixDimensions;
    }

    // collection Type tuple constructor
    Type(SPECIFIERS specifier, TYPES type, Tuple tupleType) {
        this(specifier, type, null, tupleType, null, null);
    }

    // vector collection Type constructors
    Type(SPECIFIERS specifier, TYPES type, COLLECTION_TYPES collection_type, Integer vectorSize) {
        this(specifier, type, collection_type, null, vectorSize, null);
    }

    // matrix collection Type constructors
    Type(SPECIFIERS specifier, TYPES type, COLLECTION_TYPES collection_type, Pair<Integer, Integer> matrixDimensions) {
        this(specifier, type, collection_type, null, null, matrixDimensions);
    }

    // collection type constructor without need to specify size
    Type(SPECIFIERS specifier, TYPES type, COLLECTION_TYPES collection_type) {
        this(specifier, type, collection_type, null, null, null);
    }

    // non collection Type constructor
    Type(SPECIFIERS specifier, TYPES type) {
        this(specifier, type, null, null, null, null);
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
            case TUPLE:     return strTUPLE;
            default:        return "";
        }
    }

    public static Type getReturnType(String type) {
        TYPES retType;
        switch(type) {
            case "iv":
                retType = TYPES.INTEGER;
                break;
            case "rv":
                retType = TYPES.REAL;
                break;
            case "bv":
                retType = TYPES.BOOLEAN;
                break;
            case "cv":
                retType = TYPES.CHARACTER;
                break;
            default: return null;
        }
        return new Type(Type.SPECIFIERS.CONST, retType);
    }

    public static Type getReturnType(String type, Type.COLLECTION_TYPES collection_type) {
        TYPES retType;
        switch(type) {
            case "iv":
                retType = TYPES.INTEGER;
                break;
            case "rv":
                retType = TYPES.REAL;
                break;
            case "bv":
                retType = TYPES.BOOLEAN;
                break;
            case "cv":
                retType = TYPES.CHARACTER;
                break;
            default: return null;
        }
        return new Type(Type.SPECIFIERS.VAR, retType, collection_type);
    }

    @Override
    // compares types by 2 main components
    // TODO: consider doing tuple types comparison as well
    public boolean equals(Object obj) {
        if (! (obj instanceof Type) ) {
            return false;
        }

        Type otherType = (Type) obj;

        if (this.type != null && this.type.equals(otherType.getType())) {
            // continue
        } else if (this.type == otherType.getType()) {
            // continue
        } else {
            return false;
        }


        if (this.collection_type != null && this.collection_type.equals(otherType.getCollection_type())) {
            // continue
        } else if (this.collection_type == otherType.getCollection_type()) {
            // continue
        } else {
            return false;
        }

        return true;
    }

    // returns true if [this] Type can be implicitly promoted to Type obj
    public boolean looseEquals(Object obj) {
        if (! (obj instanceof Type)) {
            return false;
        }

        Type otherType = (Type) obj;

        if (this.type != null
            && (    this.type.equals(otherType.getType())
                || (   this.type.equals(TYPES.INTEGER)
                    && otherType.getType() != null
                    && otherType.getType().equals(TYPES.REAL))
                )
            ) {
            // continue
        } else if (this.getType() == otherType.getType()) {
            // continue
        } else {
            return false;
        }

        if (this.collection_type != null && this.collection_type.equals(otherType.getCollection_type())) {
            // continue
        } else if (this.collection_type == otherType.getCollection_type()) {
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
            case TUPLE: return 6;
            case INTERVAL: return 7;
            default: throw new RuntimeException("Undefined type in table");
        }
    }

    public static String getResultFunction(Type left, Type right) {
        int fromIndex = getTypeTableIndex(left);
        int toIndex = getTypeTableIndex(right);

        String result = RESULT_TABLE[fromIndex][toIndex];

        if (result.equals("void")){
            throw new Error("Incompatible types");
        }

        return result;
    }

    public static String getCastingFunction(Type from, Type to) {
        int fromIndex = getTypeTableIndex(from);
        int toIndex = getTypeTableIndex(to);

        String result = CASTING_TABLE[fromIndex][toIndex];

        if (result.equals("void")){
            throw new Error("Cannot cast to this type");
        }

        return result;
    }

    public void setCollection_type(COLLECTION_TYPES collection_type) {
        this.collection_type = collection_type;
    }

    public Tuple getTupleType() {
        return tupleType;
    }
    public Integer getVectorSize() { return vectorSize; }
    public Pair<Integer, Integer> getMatrixDimensions() { return matrixDimensions; }

}