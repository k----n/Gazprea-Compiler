final class Pair<A, B> {
    private final A left;
    private final B right;

    Pair(A left, B right) {
        this.left = left;
        this.right = right;
    }

    A left() { return left; }
    B right() { return right; }
}