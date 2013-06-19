package io.pdef;

public class Readers {
	private Readers() {}

	public static Reader<Boolean> boolean0 = new Reader<Boolean>() {
		@Override
		public Boolean get(final Input input) {
			return input.getBoolean();
		}
	};

	public static Reader<Short> int16 = new Reader<Short>() {
		@Override
		public Short get(final Input input) {
			return input.getShort();
		}
	};

	public static Reader<Integer> int32 = new Reader<Integer>() {
		@Override
		public Integer get(final Input input) {
			return input.getInt();
		}
	};

	public static Reader<Long> int64 = new Reader<Long>() {
		@Override
		public Long get(final Input input) {
			return input.getLong();
		}
	};

	public static Reader<Float> float0 = new Reader<Float>() {
		@Override
		public Float get(final Input input) {
			return input.getFloat();
		}
	};

	public static Reader<Double> double0 = new Reader<Double>() {
		@Override
		public Double get(final Input input) {
			return input.getDouble();
		}
	};

	public static Reader<String> string = new Reader<String>() {
		@Override
		public String get(final Input input) {
			return input.getString();
		}
	};
}
