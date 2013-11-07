package io.pdef;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Primitive and collection descriptors.
 */
public class Descriptors {
	private Descriptors() {}

	public static DataTypeDescriptor<Boolean> bool = primitive(TypeEnum.BOOL, Boolean.class);
	public static DataTypeDescriptor<Short> int16 = primitive(TypeEnum.INT16, Short.class);
	public static DataTypeDescriptor<Integer> int32 = primitive(TypeEnum.INT32, Integer.class);
	public static DataTypeDescriptor<Long> int64 = primitive(TypeEnum.INT64, Long.class);
	public static DataTypeDescriptor<Float> float0 = primitive(TypeEnum.FLOAT, Float.class);
	public static DataTypeDescriptor<Double> double0 = primitive(TypeEnum.DOUBLE, Double.class);
	public static DataTypeDescriptor<String> string = primitive(TypeEnum.STRING, String.class);
	public static DataTypeDescriptor<Void> void0 = primitive(TypeEnum.VOID, Void.class);

	public static <T> ListDescriptorImpl<T> list(final DataTypeDescriptor<T> element) {
		return new ListDescriptorImpl<T>(element);
	}

	public static <T> SetDescriptorImpl<T> set(final DataTypeDescriptor<T> element) {
		return new SetDescriptorImpl<T>(element);
	}

	public static <K, V> MapDescriptorImpl<K, V> map(final DataTypeDescriptor<K> key,
			final DataTypeDescriptor<V> value) {
		return new MapDescriptorImpl<K, V>(key, value);
	}

	private static <T> PrimitiveDescriptor<T> primitive(final TypeEnum type, final Class<T> cls) {
		return new PrimitiveDescriptor<T>(type, cls);
	}

	/**
	 * Returns an interface descriptor or throws an IllegalArgumentException.
	 */
	@Nullable
	public static <T> InterfaceDescriptor<T> findInterfaceDescriptor(final Class<T> cls) {
		if (!cls.isInterface()) {
			throw new IllegalArgumentException("Interface required, got " + cls);
		}

		Field field;
		try {
			field = cls.getField("DESCRIPTOR");
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException("No DESCRIPTOR field in " + cls);
		}

		if (!InterfaceDescriptor.class.isAssignableFrom(field.getType())) {
			throw new IllegalArgumentException("Not an InterfaceDescriptor field, " + field);
		}

		try {
			// Get the static TYPE field.
			@SuppressWarnings("unchecked")
			InterfaceDescriptor<T> descriptor = (InterfaceDescriptor<T>) field.get(null);
			return descriptor;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static class PrimitiveDescriptor<T> extends BaseDescriptor<T>
			implements DataTypeDescriptor<T> {
		private PrimitiveDescriptor(final TypeEnum type, final Class<T> javaClass) {
			super(type, javaClass);
		}
	}

	private static class ListDescriptorImpl<T> extends BaseDescriptor<List<T>>
			implements ListDescriptor<T>, DataTypeDescriptor<List<T>> {
		private final DataTypeDescriptor<T> element;

		@SuppressWarnings("unchecked")
		private ListDescriptorImpl(final DataTypeDescriptor<T> element) {
			super(TypeEnum.LIST, (Class<List<T>>) (Class<?>) List.class);
			this.element = element;
			if (element == null) throw new NullPointerException("element");
		}

		@Override
		public DataTypeDescriptor<T> getElement() {
			return element;
		}
	}

	private static class SetDescriptorImpl<T> extends BaseDescriptor<Set<T>>
			implements SetDescriptor<T>, DataTypeDescriptor<Set<T>> {
		private final DataTypeDescriptor<T> element;

		@SuppressWarnings("unchecked")
		private SetDescriptorImpl(final DataTypeDescriptor<T> element) {
			super(TypeEnum.SET, (Class<Set<T>>) (Class<?>) Set.class);
			this.element = element;

			if (element == null) throw new NullPointerException("element");
		}

		@Override
		public DataTypeDescriptor<T> getElement() {
			return element;
		}
	}

	private static class MapDescriptorImpl<K, V> extends BaseDescriptor<Map<K, V>>
			implements MapDescriptor<K, V>, DataTypeDescriptor<Map<K, V>> {
		private final DataTypeDescriptor<K> key;
		private final DataTypeDescriptor<V> value;

		@SuppressWarnings("unchecked")
		MapDescriptorImpl(final DataTypeDescriptor<K> key, final DataTypeDescriptor<V> value) {
			super(TypeEnum.MAP, (Class<Map<K, V>>) (Class<?>) Map.class);
			this.key = key;
			this.value = value;
			if (key == null) throw new NullPointerException("key");
			if (value == null) throw new NullPointerException("value");
		}

		@Override
		public DataTypeDescriptor<K> getKey() {
			return key;
		}

		@Override
		public DataTypeDescriptor<V> getValue() {
			return value;
		}
	}
}
