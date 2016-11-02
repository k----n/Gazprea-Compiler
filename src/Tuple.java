import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by parash on 10/30/16.
 */
public class Tuple {
    // Get property number from id
    HashMap<String, Integer> id_to_number;

    // Get variable property number refers to
    HashMap<Integer, Variable> variables;

    // Holds the last used field Id
    private Integer fieldId;

    Tuple(ArrayList<String> fields) {
        this.fieldId = 0;
        fields.forEach(this::addField);
    }

    // used to construct the tuple object
    public void addField(String field) {
        ++this.fieldId;
        if (isStringId(field)) {
            this.id_to_number.put(field, this.fieldId);
        }

        this.variables.put(this.fieldId, null);
    }

    public Integer getFieldNumber(String field) {
        if (isStringId(field)) {
            return Integer.parseInt(field);
        } else {
            return id_to_number.get(field);
        }
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

    private static String typeToLLVMStructType(Type type) {
        // TODO: USE THIS FUNCTION
        switch (type.getType()) {
            case INTEGER:
                return "i32";
            case CHARACTER:
                return "i8";
            case BOOLEAN:
                return "i1";
            case REAL:
                return "float";
            default:
                return "";
        }
    }
}
