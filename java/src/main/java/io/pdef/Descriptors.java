package io.pdef;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Descriptors {
	private Descriptors() {}

	public static Descriptor<Boolean> bool = new Descriptor<Boolean>() {
		@Override
		public Boolean getDefault() {
			return false;
		}

		@Override
		public Boolean read(final Input input) {
			return input.readBoolean();
		}

		@Override
		public void write(final Boolean value, final Output output) {
			output.write(value == null ? getDefault() : value);
		}
	};

	public static Descriptor<Short> int16 = new Descriptor<Short>() {
		@Override
		public Short getDefault() {
			return (short) 0;
		}

		@Override
		public Short read(final Input input) {
			return input.readShort();
		}

		@Override
		public void write(final Short value, final Output output) {
			output.write(value == null ? getDefault() : value);
		}
	};

	public static Descriptor<Integer> int32 = new Descriptor<Integer>() {
		@Override
		public Integer getDefault() {
			return 0;
		}

		@Override
		public Integer read(final Input input) {
			return input.readInt();
		}

		@Override
		public void write(final Integer value, final Output output) {
			output.write(value == null ? getDefault() : value);
		}
	};

	public static Descriptor<Long> int64 = new Descriptor<Long>() {
		@Override
		public Long getDefault() {
			return 0L;
		}

		@Override
		public Long read(final Input input) {
			return input.readLong();
		}

		@Override
		public void write(final Long value, final Output output) {
			output.write(value == null ? getDefault() : value);
		}
	};

	public static Descriptor<Float> float0 = new Descriptor<Float>() {
		@Override
		public Float getDefault() {
			return 0f;
		}

		@Override
		public Float read(final Input input) {
			return input.readFloat();
		}

		@Override
		public void write(final Float value, final Output output) {
			output.write(value == null ? getDefault() : value);
		}
	};

	public static Descriptor<Double> double0 = new Descriptor<Double>() {
		@Override
		public Double getDefault() {
			return 0d;
		}

		@Override
		public Double read(final Input input) {
			return input.readDouble();
		}

		@Override
		public void write(final Double value, final Output output) {
			output.write(value == null ? getDefault() : value);
		}
	};

	public static Descriptor<String> string = new Descriptor<String>() {
		@Override
		public String getDefault() {
			return null;
		}

		@Override
		public String read(final Input input) {
			return input.readString();
		}

		@Override
		public void write(final String value, final Output output) {
			output.write(value);
		}
	};

	public static Descriptor<Void> void0 = new Descriptor<Void>() {
		@Override
		public Void getDefault() {
			return null;
		}

		@Override
		public Void read(final Input input) {
			return null;
		}

		@Override
		public void write(final Void value, final Output output) {}
	};

	public static Descriptor<Object> object = new Descriptor<Object>() {
		@Override
		public Object getDefault() {
			return null;
		}

		@Override
		public Object read(final Input input) {
			return input.readObject();
		}

		@Override
		public void write(final Object value, final Output output) {
			output.writeObject(value);
		}
	};

	public static <T> Descriptor<T> enum0(final T defaultValue) {
		return new Descriptor<T>() {
			@Override
			public T getDefault() {
				return defaultValue;
			}

			@Override
			public T read(final Input input) {
				return null;
			}

			@Override
			public void write(final T value, final Output output) {
			}
		};
	}

	public static <T> Descriptor<List<T>> list(final Descriptor<T> element) {
		return null;
	}

	public static <T> Descriptor<Set<T>> set(final Descriptor<T> element) {
		return null;
	}

	public static <K, V> Descriptor<Map<K, V>> map(final Descriptor<K> key,
			final Descriptor<V> value) {
		return null;
	}
}
