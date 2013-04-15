package io.pdef.descriptors;

import com.google.common.base.Objects;

import java.lang.reflect.Field;

import static com.google.common.base.Preconditions.checkNotNull;

public class FieldDescriptor {
	private final Field field;
	private final String name;
	private final Descriptor descriptor;

	public FieldDescriptor(final Field field, final DescriptorPool pool) {
		this.field = checkNotNull(field);
		name = field.getName();
		descriptor = pool.getDescriptor(field.getGenericType());
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.addValue(descriptor.getJavaType())
				.toString();
	}

	public String getName() {
		return field.getName();
	}

	public Field getField() {
		return field;
	}

	public Descriptor getDescriptor() {
		return descriptor;
	}
}
