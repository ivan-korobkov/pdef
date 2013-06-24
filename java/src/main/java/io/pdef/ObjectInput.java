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
	public boolean readBoolean() {
		return value == null ? false : (Boolean) value;
	}

	@Override
	public short readShort() {
		return value == null ? 0 : ((Number) value).shortValue();
	}

	@Override
	public int readInt() {
		return value == null ? 0 : ((Number) value).intValue();
	}

	@Override
	public long readLong() {
		return value == null ? 0 : ((Number) value).longValue();
	}

	@Override
	public float readFloat() {
		return value == null ? 0 : ((Number) value).floatValue();
	}

	@Override
	public double readDouble() {
		return value == null ? 0 : ((Number) value).doubleValue();
	}

	@Override
	public String readString() {
		return (String) value;
	}

	@Override
	public Object readObject() {
		return value;
	}

	@Override
	public <T> List<T> readList(final Reader<T> elementReader) {
		if (value == null) return ImmutableList.of();

		List<?> list = (List<?>) value;
		List<T> result = Lists.newArrayList();
		ObjectInput in = new ObjectInput(null); // Reusable element input.

		for (Object e : list) {
			in.value = e;
			T r = elementReader.read(in);
			result.add(r);
		}

		return result;
	}

	@Override
	public <T> T readMessage(final MessageReader<T> reader) {
		return value == null ? null : reader.read(new MessageInput((Map<?, ?>) value));
	}

	@Override
	public <T> T read(final Reader<T> reader) {
		return reader.read(this);
	}

	static class MessageInput implements io.pdef.MessageInput {
		private final Map<?, ?> map;
		private final ObjectInput in; // Reusable field input.

		MessageInput(final Map<?, ?> map) {
			this.map = checkNotNull(map);
			in = new ObjectInput(null);
		}

		@Override
		public <T> T read(final String field, final Reader<T> reader) {
			in.value = map.get(field);
			T result = reader.read(in);
			in.value = null;
			return result;
		}
	}
}
