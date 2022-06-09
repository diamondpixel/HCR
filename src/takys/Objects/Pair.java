package takys.Objects;

public record Pair<Key, Value>(Key key, Value value) {

    public Key getKey() {
        return key;
    }

    public Value getValue() {
        return value;
    }
}