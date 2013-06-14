package io.pdef.io;

public class Writers {
	private Writers() {}

	public static Writer<Boolean> boolean0 = new Writer<Boolean>() {
		@Override
		public void write(final Boolean value, final Output output) {
			output.write(value);
		}
	};

	public static Writer<Short> int16 = new Writer<Short>() {
		@Override
		public void write(final Short value, final Output output) {
			output.write(value);
		}
	};

	public static Writer<Integer> int32 = new Writer<Integer>() {
		@Override
		public void write(final Integer value, final Output output) {
			output.write(value);
		}
	};

	public static Writer<Long> int64 = new Writer<Long>() {
		@Override
		public void write(final Long value, final Output output) {
			output.write(value);
		}
	};

	public static Writer<Float> float0 = new Writer<Float>() {
		@Override
		public void write(final Float value, final Output output) {
			output.write(value);
		}
	};

	public static Writer<Double> double0 = new Writer<Double>() {
		@Override
		public void write(final Double value, final Output output) {
			output.write(value);
		}
	};

	public static Writer<String> string = new Writer<String>() {
		@Override
		public void write(final String value, final Output output) {
			output.write(value);
		}
	};
}
