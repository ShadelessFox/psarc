package sh.adelessfox.psarc.util.type;

public sealed interface Amount<T extends Amount<T>> extends Comparable<T> {
    T add(T other);

    T subtract(T other);

    T increment();

    T decrement();

    non-sealed abstract class OfLong<T extends OfLong<T>> implements Amount<T> {
        final long value;

        public OfLong(long value) {
            this.value = value;
        }

        @Override
        public T add(T other) {
            return newInstance(value + other.value);
        }

        @Override
        public T subtract(T other) {
            return newInstance(value - other.value);
        }

        @Override
        public T increment() {
            return newInstance(value + 1);
        }

        @Override
        public T decrement() {
            return newInstance(value - 1);
        }

        @Override
        public int compareTo(T o) {
            return Long.compare(value, o.value);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            OfLong<?> ofLong = (OfLong<?>) o;
            return value == ofLong.value;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        protected abstract T newInstance(long value);
    }
}
