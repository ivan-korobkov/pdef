package io.pdef.io;

public class ObjectValue implements InputValue {
	private final Object v;

	public ObjectValue(final Object v) {
		this.v = v;
	}

	@Override
	public boolean getBoolean() {
		return v == null ? false : (Boolean) v;
	}

	@Override
	public short getShort() {
		return v == null ? 0 : ((Number) v).shortValue();
	}

	@Override
	public int getInt() {
		return v == null ? 0 : ((Number) v).intValue();
	}

	@Override
	public long getLong() {
		return v == null ? 0 : ((Number) v).longValue();
	}

	@Override
	public float getFloat() {
		return v == null ? 0f : ((Number) v).floatValue();
	}

	@Override
	public double getDouble() {
		return v == null ? 0d : ((Number) v).doubleValue();
	}

	@Override
	public String getString() {
		return (String) v;
	}

	@Override
	public InputValue asValue() {
		return this;
	}

	@Override
	public InputMessage asMessage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputList asList() {
		throw new UnsupportedOperationException();
	}
}
