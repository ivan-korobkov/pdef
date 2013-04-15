package io.pdef.descriptors;

import com.google.common.base.Objects;
import io.pdef.Message;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

public class FieldDescriptor {
	private final MessageDescriptor message;
	private final Field field;
	private final String name;
	private final Descriptor type;
	private final Accessor accessor;

	public FieldDescriptor(final Field field, final MessageDescriptor message) {
		this.field = checkNotNull(field);
		this.message = checkNotNull(message);
		name = field.getName();
		type = message.getPool().getDescriptor(field.getGenericType());
		accessor = new Accessor(field, message.getJavaType(), message.getBuilderType());
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.addValue(type.getJavaType())
				.toString();
	}

	public MessageDescriptor getMessage() {
		return message;
	}

	public String getName() {
		return field.getName();
	}

	public Field getField() {
		return field;
	}

	public Descriptor getType() {
		return type;
	}

	public void set(final Message.Builder builder, final Object value) {
		accessor.set(builder, value);
	}

	public Object get(final Message message) {
		return accessor.get(message);
	}

	static class Accessor {
		private final Method messageGetter;
		private final Method builderSetter;

		Accessor(final Field field, final Class<?> messageClass, final Class<?> builderClass) {
			String name = field.getName();
			String upperFirst = Character.toUpperCase(name.charAt(0)) + name.substring(1);
			String getterName = "get" + upperFirst;
			String setterName = "set" + upperFirst;
			Class<?> type = field.getType();
			try {
				messageGetter = messageClass.getMethod(getterName);
				builderSetter = builderClass.getMethod(setterName, type);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		public Object get(final Object message) {
			try {
				return messageGetter.invoke(message);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public void set(final Object builder, final Object value) {
			try {
				builderSetter.invoke(builder, value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
