package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ObjectInput implements Input {
	private Object value;

	public ObjectInput(final Object value) {
		this.value = value;
	}

	@Override
	public boolean getBoolean() {
		return value == null ? false : (Boolean) value;
	}

	@Override
	public short getShort() {
		return value == null ? 0 : ((Number) value).shortValue();
	}

	@Override
	public int getInt() {
		return value == null ? 0 : ((Number) value).intValue();
	}

	@Override
	public long getLong() {
		return value == null ? 0 : ((Number) value).longValue();
	}

	@Override
	public float getFloat() {
		return value == null ? 0 : ((Number) value).floatValue();
	}

	@Override
	public double getDouble() {
		return value == null ? 0 : ((Number) value).doubleValue();
	}

	@Override
	public String getString() {
		return (String) value;
	}

	@Override
	public <T> List<T> getList(final Reader<T> elementReader) {
		if (value == null) return ImmutableList.of();

		List<?> list = (List<?>) value;
		List<T> result = Lists.newArrayList();
		ObjectInput in = new ObjectInput(null); // Reusable element input.

		for (Object e : list) {
			in.value = e;
			T r = elementReader.get(in);
			result.add(r);
		}

		return result;
	}

	@Override
	public <T> T getMessage(final Message.MessageReader<T> reader) {
		return value == null ? null : reader.get(new MessageInput((Map<?, ?>) value));
	}

	@Override
	public <T> T get(final Reader<T> reader) {
		return reader.get(this);
	}

	static class MessageInput implements Message.MessageInput {
		private final Map<?, ?> map;
		private final ObjectInput in; // Reusable field input.

		MessageInput(final Map<?, ?> map) {
			this.map = checkNotNull(map);
			in = new ObjectInput(null);
		}

		@Override
		public <T> T get(final String field, final Reader<T> reader) {
			in.value = map.get(field);
			T result = reader.get(in);
			in.value = null;
			return result;
		}
	}
}
