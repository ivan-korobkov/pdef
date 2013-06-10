package io.pdef.io;

import java.util.Map;

public class ObjectMessage implements InputMessage {
	private final Map<String, Object> map;

	public ObjectMessage(final Map<String, Object> map) {
		this.map = map;
	}

	public static interface KnowsHowToDispatchItself {
		public void dispatch(ObjectMessage msg);
	}

	@Override
	public boolean getBoolean(final String field) {
		Object v = map.get(field);
		return v == null ? false : (Boolean) v;
	}

	@Override
	public short getShort(final String field) {
		Object v = map.get(field);
		return v == null ? 0 : ((Number) v).shortValue();
	}

	@Override
	public int getInt(final String field) {
		Object v = map.get(field);
		return v == null ? 0 : ((Number) v).intValue();
	}

	@Override
	public long getLong(final String field) {
		Object v = map.get(field);
		return v == null ? 0 : ((Number) v).longValue();
	}

	@Override
	public float getFloat(final String field) {
		Object v = map.get(field);
		return v == null ? 0f : ((Number) v).floatValue();
	}

	@Override
	public double getDouble(final String field) {
		Object v = map.get(field);
		return v == null ? 0d : ((Number) v).doubleValue();
	}

	@Override
	public String getString(final String field) {
		Object v = map.get(field);
		return (String) v;
	}

	@Override
	public InputValue getValue(final String field) {
		Object v = map.get(field);
		return new ObjectValue(v);
	}

	@Override
	public InputList getList(final String field) {
		return null;
	}

	@Override
	public InputValue asValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputMessage asMessage() {
		return this;
	}

	@Override
	public InputList asList() {
		throw new UnsupportedOperationException();
	}
}
