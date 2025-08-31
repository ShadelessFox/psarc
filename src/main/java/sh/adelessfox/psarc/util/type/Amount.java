package sh.adelessfox.psarc.util.type;

public sealed interface Amount<T extends Amount<T>> extends Comparable<T> {
    T addTo(T amount);

    T subtractFrom(T amount);

    non-sealed abstract class OfLong<T extends OfLong<T>> implements Amount<T> {
        protected final long value;

        public OfLong(long value) {
            this.value = value;
        }

        @Override
        public T addTo(T amount) {
            return newInstance(value + amount.value);
        }

        @Override
        public T subtractFrom(T amount) {
            return newInstance(value - amount.value);
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
