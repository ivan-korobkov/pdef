package pdef.descriptors;

import static com.google.common.base.Preconditions.checkNotNull;
import pdef.Symbol;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class FieldDescriptor implements Symbol {
	private final Field field;
	private final String name;
	private final Type type;

	public FieldDescriptor(final Field field) {
		this.field = checkNotNull(field);
		name = field.getName();
		type = field.getGenericType();
	}

	@Override
	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}
}
