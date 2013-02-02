package com.ivankorobkov.pdef.data;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.DescriptorPool;
import com.ivankorobkov.pdef.GenericDescriptor;

import javax.annotation.Nonnull;

public class MessageField implements GenericDescriptor {

	private final String name;
	private final TypeToken<?> type;
	private final MessageFieldAccessor accessor;
	private final boolean readOnly;
	private final boolean lineFormat;

	private DataTypeDescriptor descriptor;

	public MessageField(final String name,
			final boolean readOnly,
			final boolean lineFormat,
			final TypeToken<?> type,
			final MessageFieldAccessor accessor) {
		this.name = checkNotNull(name);
		this.type = checkNotNull(type);
		this.accessor = checkNotNull(accessor);
		this.readOnly = readOnly;
		this.lineFormat = lineFormat;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.addValue(type)
				.toString();
	}

	@Override
	public final String getName() {
		return name;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isLineFormat() {
		return lineFormat;
	}

	public DataTypeDescriptor getDescriptor() {
		return descriptor;
	}

	public Object get(final Message message) {
		return accessor.get(message);
	}

	public void set(final Message message, final Object value) {
		accessor.set(message, value);
	}

	public boolean isSetIn(final Message message) {
		return accessor.isSetIn(message);
	}

	public void clear(final Message message) {
		accessor.clear(message);
	}

	@Override
	public void link(final DescriptorPool pool) {
		this.descriptor = pool.get(type);
	}

	public void merge(@Nonnull final Message message, @Nonnull final Message another) {
		Object currentValue = get(message);
		Object anotherValue = get(another);
		Object mergedValue = descriptor.merge(currentValue, anotherValue);
		set(message, mergedValue);
	}
}
