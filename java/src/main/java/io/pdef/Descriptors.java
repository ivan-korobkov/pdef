package io.pdef;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Primitive and collection descriptors.
 */
public class Descriptors {
	private Descriptors() {}

	public static DataDescriptor<Boolean> bool = primitive(TypeEnum.BOOL, Boolean.class);
	public static DataDescriptor<Short> int16 = primitive(TypeEnum.INT16, Short.class);
	public static DataDescriptor<Integer> int32 = primitive(TypeEnum.INT32, Integer.class);
	public static DataDescriptor<Long> int64 = primitive(TypeEnum.INT64, Long.class);
	public static DataDescriptor<Float> float0 = primitive(TypeEnum.FLOAT, Float.class);
	public static DataDescriptor<Double> double0 = primitive(TypeEnum.DOUBLE, Double.class);
	public static DataDescriptor<String> string = primitive(TypeEnum.STRING, String.class);
	public static DataDescriptor<Void> void0 = new AbstractDataDescriptor<Void>(
			TypeEnum.VOID, Void.class) {
		@Override
		public Void copy(final Void object) {
			return null;
		}
	};

	public static <T> ListDescriptorImpl<T> list(final DataDescriptor<T> element) {
		return new ListDescriptorImpl<T>(element);
	}

	public static <T> SetDescriptorImpl<T> set(final DataDescriptor<T> element) {
		return new SetDescriptorImpl<T>(element);
	}

	public static <K, V> MapDescriptorImpl<K, V> map(final DataDescriptor<K> key,
			final DataDescriptor<V> value) {
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

	private static class PrimitiveDescriptor<T> extends AbstractDataDescriptor<T> {
		private PrimitiveDescriptor(final TypeEnum type, final Class<T> javaClass) {
			super(type, javaClass);
		}

		@Override
		public T copy(final T object) {
			return object;
		}
	}

	private static class ListDescriptorImpl<T> extends AbstractDataDescriptor<List<T>>
			implements ListDescriptor<T> {
		private final DataDescriptor<T> element;

		@SuppressWarnings("unchecked")
		private ListDescriptorImpl(final DataDescriptor<T> element) {
			super(TypeEnum.LIST, (Class<List<T>>) (Class<?>) List.class);
			this.element = element;
			if (element == null) throw new NullPointerException("element");
		}

		@Override
		public DataDescriptor<T> getElement() {
			return element;
		}

		@Override
		public List<T> copy(final List<T> list) {
			if (list == null) {
				return null;
			}

			List<T> copy = new ArrayList<T>();
			for (T e : list) {
				T copied = element.copy(e);
				copy.add(copied);
			}

			return copy;
		}
	}

	private static class SetDescriptorImpl<T> extends AbstractDataDescriptor<Set<T>>
			implements SetDescriptor<T> {
		private final DataDescriptor<T> element;

		@SuppressWarnings("unchecked")
		private SetDescriptorImpl(final DataDescriptor<T> element) {
			super(TypeEnum.SET, (Class<Set<T>>) (Class<?>) Set.class);
			this.element = element;

			if (element == null) throw new NullPointerException("element");
		}

		@Override
		public DataDescriptor<T> getElement() {
			return element;
		}

		@Override
		public Set<T> copy(final Set<T> set) {
			if (set == null) {
				return null;
			}

			Set<T> copy = new HashSet<T>();
			for (T elem : set) {
				T copied = element.copy(elem);
				copy.add(copied);
			}

			return copy;
		}
	}

	private static class MapDescriptorImpl<K, V> extends AbstractDataDescriptor<Map<K, V>>
			implements MapDescriptor<K, V> {
		private final DataDescriptor<K> key;
		private final DataDescriptor<V> value;

		@SuppressWarnings("unchecked")
		MapDescriptorImpl(final DataDescriptor<K> key, final DataDescriptor<V> value) {
			super(TypeEnum.MAP, (Class<Map<K, V>>) (Class<?>) Map.class);
			this.key = key;
			this.value = value;
			if (key == null) throw new NullPointerException("key");
			if (value == null) throw new NullPointerException("value");
		}

		@Override
		public DataDescriptor<K> getKey() {
			return key;
		}

		@Override
		public DataDescriptor<V> getValue() {
			return value;
		}

		@Override
		public Map<K, V> copy(final Map<K, V> map) {
			if (map == null) {
				return null;
			}

			Map<K, V> copy = new LinkedHashMap<K, V>();
			for (Map.Entry<K, V> entry : map.entrySet()) {
				K ck = key.copy(entry.getKey());
				V cv = value.copy(entry.getValue());
				copy.put(ck, cv);
			}

			return copy;
		}
	}
}
