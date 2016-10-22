import java.util.*;

class Scope<T> {
    private List<Map<String, T>> scopes = new ArrayList<>();

    Scope() { this.pushScope(); }

    private Map<String, T> lastScope() {
        return this.scopes.get(this.scopes.size() - 1);
    }

    void pushScope() {
        this.scopes.add(new HashMap<>());
    }

    void popScope() {
        if (this.scopes.size() == 1) { return; }
        this.scopes.remove(this.scopes.size() - 1);
    }

    boolean initVariable(String variableName, T value) {
        if (this.lastScope().get(variableName) != null) { return false; }
        this.lastScope().put(variableName, value);
        return true;
    }

    T getVariable(String variableName) {
        List<Map<String, T>> scopesCopy = new ArrayList<>(this.scopes);
        Collections.reverse(scopesCopy);
        for (Map<String, T> scope : scopesCopy) {
            if (scope.get(variableName) != null) {
                return scope.get(variableName);
            }
        }
        return null;
    }

    Integer count() {
        return this.scopes.size();
    }
}
