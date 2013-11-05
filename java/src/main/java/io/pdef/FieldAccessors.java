package io.pdef;

import java.lang.reflect.Field;

public class FieldAccessors {
	private FieldAccessors() {}

	/**
	 * Returns a reflection-based field accessor for a declared field.
	 */
	public static <M, V> FieldAccessor<M, V> reflexive(final String name, final Class<M> cls) {
		return new ReflexiveFieldAccessor<M, V>(name, cls);
	}

	private static class ReflexiveFieldAccessor<M, V> implements FieldAccessor<M, V> {
		private final Field field;

		private ReflexiveFieldAccessor(final String name, final Class<M> cls) {
			Field field1 = null;
			for (Field field0 : cls.getDeclaredFields()) {
				if (field0.getName().equals(name)) {
					field1 = field0;
					break;
				}
			}

			if (field1 == null) throw new IllegalArgumentException("Field not found: " + name);
			field = field1;
			field.setAccessible(true);
		}

		@SuppressWarnings("unchecked")
		@Override
		public V get(final M message) {
			try {
				return (V) field.get(message);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void set(final M message, final V value) {
			try {
				field.set(message, value);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
