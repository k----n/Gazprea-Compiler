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

    private String tupleName;

    Tuple(String name, ArrayList<String> fields) {
        this.fieldId = 0;
        this.tupleName = name;
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

    public String getTupleName() {
        return tupleName;
    }

    private Boolean isStringId(String id) {
        try {
            Integer.parseInt(id);
        } catch(NumberFormatException e) {
            return false;
        }

        return true;
    }


}
