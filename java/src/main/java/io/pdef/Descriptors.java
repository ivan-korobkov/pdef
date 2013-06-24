package io.pdef;

public class Descriptors {
	private Descriptors() {}

	public static Descriptor<Boolean> bool = new Descriptor<Boolean>() {
		@Override
		public Boolean getDefault() {
			return false;
		}

		@Override
		public Boolean get(final Input input) {
			return input.getBoolean();
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
		public Short get(final Input input) {
			return input.getShort();
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
		public Integer get(final Input input) {
			return input.getInt();
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
		public Long get(final Input input) {
			return input.getLong();
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
		public Float get(final Input input) {
			return input.getFloat();
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
		public Double get(final Input input) {
			return input.getDouble();
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
		public String get(final Input input) {
			return input.getString();
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
		public Void get(final Input input) {
			return null;
		}

		@Override
		public void write(final Void value, final Output output) {}
	};

	public static <T> Descriptor<T> enum0(final T defaultValue) {
		return new Descriptor<T>() {
			@Override
			public T getDefault() {
				return defaultValue;
			}

			@Override
			public T get(final Input input) {
				return null;
			}

			@Override
			public void write(final T value, final Output output) {
			}
		};
	}
}
