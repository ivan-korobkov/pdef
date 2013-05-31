package io.pdef;

import com.google.common.base.Objects;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

/** Pdef message field descriptor. */
public class PdefField {
	private final Field field;
	private final String name;

	private final PdefMessage message;
	private final PdefDescriptor descriptor;

	private final Method messageGet;
	private final Method messageHas;
	private final Method builderSet;

	PdefField(final Field field, final PdefMessage message) {
		this.field = checkNotNull(field);
		this.message = checkNotNull(message);
		descriptor = message.descriptorOf(field.getGenericType());
		name = getFieldName(field);

		String upperFirst = upperFirst(field);
		String get = "get" + upperFirst;
		String has = "has" + upperFirst;
		String set = "set" + upperFirst;
		try {
			messageGet = message.getJavaClass().getMethod(get);
			messageHas = message.getJavaClass().getMethod(has);
			builderSet = message.getBuilderClass().getMethod(set, field.getType());
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.addValue(descriptor)
				.toString();
	}

	public Field getField() {
		return field;
	}

	public String getName() {
		return name;
	}

	public PdefMessage getMessage() {
		return message;
	}

	public PdefDescriptor getDescriptor() {
		return descriptor;
	}

	public Object get(final Object message) {
		try {
			return messageGet.invoke(message);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isSet(final Object message) {
		try {
			return (Boolean) messageHas.invoke(message);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public void set(final Object builder, final Object value) {
		try {
			builderSet.invoke(builder, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/** Returns a lower-cased field name. */
	static String getFieldName(final Field field) {
		return field.getName().toLowerCase();
	}

	static String upperFirst(final Field field) {
		return Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
	}
}
