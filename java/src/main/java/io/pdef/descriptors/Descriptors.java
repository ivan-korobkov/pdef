package io.pdef.descriptors;

import io.pdef.TypeEnum;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Primitive and collection descriptors.
 */
public class Descriptors {
	private Descriptors() {}

	public static ValueDescriptor<Boolean> bool = primitive(TypeEnum.BOOL, Boolean.class);
	public static ValueDescriptor<Short> int16 = primitive(TypeEnum.INT16, Short.class);
	public static ValueDescriptor<Integer> int32 = primitive(TypeEnum.INT32, Integer.class);
	public static ValueDescriptor<Long> int64 = primitive(TypeEnum.INT64, Long.class);
	public static ValueDescriptor<Float> float0 = primitive(TypeEnum.FLOAT, Float.class);
	public static ValueDescriptor<Double> double0 = primitive(TypeEnum.DOUBLE, Double.class);
	public static ValueDescriptor<String> string = primitive(TypeEnum.STRING, String.class);
	public static ValueDescriptor<Void> void0 = primitive(TypeEnum.VOID, Void.class);

	public static <T> ListDescriptorImpl<T> list(final ValueDescriptor<T> element) {
		return new ListDescriptorImpl<T>(element);
	}

	public static <T> SetDescriptorImpl<T> set(final ValueDescriptor<T> element) {
		return new SetDescriptorImpl<T>(element);
	}

	public static <K, V> MapDescriptorImpl<K, V> map(final ValueDescriptor<K> key,
			final ValueDescriptor<V> value) {
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
			implements ValueDescriptor<T> {
		private PrimitiveDescriptor(final TypeEnum type, final Class<T> javaClass) {
			super(type, javaClass);
		}
	}

	private static class ListDescriptorImpl<T> extends BaseDescriptor<List<T>>
			implements ListDescriptor<T>, ValueDescriptor<List<T>> {
		private final ValueDescriptor<T> element;

		@SuppressWarnings("unchecked")
		private ListDescriptorImpl(final ValueDescriptor<T> element) {
			super(TypeEnum.LIST, (Class<List<T>>) (Class<?>) List.class);
			this.element = element;
			if (element == null) throw new NullPointerException("element");
		}

		@Override
		public ValueDescriptor<T> getElement() {
			return element;
		}
	}

	private static class SetDescriptorImpl<T> extends BaseDescriptor<Set<T>>
			implements SetDescriptor<T>, ValueDescriptor<Set<T>> {
		private final ValueDescriptor<T> element;

		@SuppressWarnings("unchecked")
		private SetDescriptorImpl(final ValueDescriptor<T> element) {
			super(TypeEnum.SET, (Class<Set<T>>) (Class<?>) Set.class);
			this.element = element;

			if (element == null) throw new NullPointerException("element");
		}

		@Override
		public ValueDescriptor<T> getElement() {
			return element;
		}
	}

	private static class MapDescriptorImpl<K, V> extends BaseDescriptor<Map<K, V>>
			implements MapDescriptor<K, V>, ValueDescriptor<Map<K, V>> {
		private final ValueDescriptor<K> key;
		private final ValueDescriptor<V> value;

		@SuppressWarnings("unchecked")
		MapDescriptorImpl(final ValueDescriptor<K> key, final ValueDescriptor<V> value) {
			super(TypeEnum.MAP, (Class<Map<K, V>>) (Class<?>) Map.class);
			this.key = key;
			this.value = value;
			if (key == null) throw new NullPointerException("key");
			if (value == null) throw new NullPointerException("value");
		}

		@Override
		public ValueDescriptor<K> getKey() {
			return key;
		}

		@Override
		public ValueDescriptor<V> getValue() {
			return value;
		}
	}
}
