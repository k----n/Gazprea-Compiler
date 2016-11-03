import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by parash on 10/30/16.
 */
public class Tuple {
    // Get property number from id
    HashMap<String, Integer> id_to_number;

    // Get variable property number refers to
    HashMap<Integer, Type> variables;

    // Holds the last used field Id
    private Integer fieldId;

    Tuple() {
        this.fieldId = 0;
        id_to_number = new HashMap<>();
        variables = new HashMap<>();
    }

    // used to construct the tuple object
    public void addField(String field, Type fieldType) {
        ++this.fieldId;
        if (isStringId(field)) {
            this.id_to_number.put(field, this.fieldId);
        }

        this.variables.put(this.fieldId, fieldType);
    }

    public Integer getFieldNumber(String field) {
        if (isStringId(field)) {
            return Integer.parseInt(field);
        } else {
            return id_to_number.get(field);
        }
    }

    public Type getTypeOfField(Integer fieldNo) {
        return variables.get(fieldNo);
    }

    private Boolean isStringId(String id) {
        if (id == null) {
            return false;
        }

        try {
            Integer.parseInt(id);
        } catch(NumberFormatException e) {
            return false;
        }

        return true;
    }

    private ST getSTForType(Type.TYPES type) {
        STGroup llvmGroup = new STGroupFile("./src/llvm.stg");
        switch (type) {
            case BOOLEAN:
                return llvmGroup.getInstanceOf("pushNullIntegerToTuple(");
            case CHARACTER:
                return llvmGroup.getInstanceOf("pushNullRealToTuple");
            case INTEGER:
                return llvmGroup.getInstanceOf("pushNullBooleanToTuple");
            case REAL:
                return llvmGroup.getInstanceOf("pushNullCharacterToTuple");
            default:
                throw new RuntimeException("Invalid Tuple Type");
        }
    }

    public ST getInitializingStatements() {
        STGroup llvmGroup = new STGroupFile("./src/llvm.stg");
        ST varInit_Tuple =  llvmGroup.getInstanceOf("varInit_Tuple");

        for (int key = 1; key <= variables.size(); ++key) {
            Type var = variables.get(key);
            varInit_Tuple.add("tuple_statements", getSTForType(var.getType()));
        }

        return varInit_Tuple;
    }
}
