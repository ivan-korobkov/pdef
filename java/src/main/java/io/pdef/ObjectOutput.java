package io.pdef;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class ObjectOutput implements Output {
	private Object value;

	public Object toObject() {
		return value;
	}

	@Override
	public void write(final boolean v) {
		this.value = v;
	}

	@Override
	public void write(final short v) {
		this.value = v;
	}

	@Override
	public void write(final int v) {
		this.value = v;
	}

	@Override
	public void write(final long v) {
		this.value = v;
	}

	@Override
	public void write(final float v) {
		this.value = v;
	}

	@Override
	public void write(final double v) {
		this.value = v;
	}

	@Override
	public void write(final String v) {
		this.value = v;
	}

	@Override
	public <T> void writeList(final List<T> list, final Writer<T> elementWriter) {
	}

	@Override
	public <T> void write(final T object, final Writer<T> writer) {
		ObjectOutput output = new ObjectOutput();
		writer.write(object, output);
		this.value = output.toObject();
	}

	@Override
	public <T> void writeMessage(final T message, final MessageWriter<T> writer) {
		MessageOutput output = new MessageOutput();
		writer.write(message, output);
		this.value = output.toObject();
	}

	static class MessageOutput implements io.pdef.MessageOutput {
		private Map<String, Object> map = Maps.newLinkedHashMap();
		private ObjectOutput fieldOut; // Reusable field output.

		Map<String, Object> toObject() {
			return map;
		}

		@Override
		public <T> void write(final String field, final T value, final Writer<T> writer) {
			if (fieldOut == null) fieldOut = new ObjectOutput();
			fieldOut.value = null;

			writer.write(value, fieldOut);
			Object fieldValue = fieldOut.toObject();

			map.put(field, fieldValue);
		}
	}
}
