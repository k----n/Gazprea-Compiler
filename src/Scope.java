import java.util.*;

class Scope<T> {
    private List<Pair<Long, Map<String, T>>> scopes = new ArrayList<>();

    static private Long uniqueScopeId = 0L;

    Scope() {
        this.pushScope();
    }

    private Map<String, T> lastScope() {
        return this.scopes.get(this.scopes.size() - 1).right();
    }

    void pushScope() {
        this.scopes.add(new Pair<Long, Map<String, T>>(uniqueScopeId, new HashMap<>()));
        ++uniqueScopeId;
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
        List<Pair<Long, Map<String, T>>> scopesCopy = new ArrayList<>(this.scopes);
        Collections.reverse(scopesCopy);
        for (Pair<Long, Map<String, T>> scope : scopesCopy) {
            if (scope.right().get(variableName) != null) {
                return scope.right().get(variableName);
            }
        }
        return null;
    }

    Long uniqueScopeId() {
        return this.scopes.get(this.scopes.size() - 1).left();
    }
}
