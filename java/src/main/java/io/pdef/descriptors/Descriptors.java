package io.pdef.descriptors;

/** Primitive descriptors and collection descriptor factories. */
public class Descriptors {
	private Descriptors() {}

	private static class PrimitiveDescriptor<T> extends DataDescriptor<T> {
		private PrimitiveDescriptor(final TypeEnum type, final Class<T> javaClass) {
			super(type, javaClass);
		}

		@Override
		public T copy(final T object) {
			return object;
		}
	}

	private static <T> PrimitiveDescriptor<T> primitive(final TypeEnum type, final Class<T> cls) {
		return new PrimitiveDescriptor<T>(type, cls);
	}

	public static DataDescriptor<Boolean> bool = primitive(TypeEnum.BOOL, Boolean.class);
	public static DataDescriptor<Short> int16 = primitive(TypeEnum.INT16, Short.class);
	public static DataDescriptor<Integer> int32 = primitive(TypeEnum.INT32, Integer.class);
	public static DataDescriptor<Long> int64 = primitive(TypeEnum.INT64, Long.class);
	public static DataDescriptor<Float> float0 = primitive(TypeEnum.FLOAT, Float.class);
	public static DataDescriptor<Double> double0 = primitive(TypeEnum.DOUBLE, Double.class);
	public static DataDescriptor<String> string = primitive(TypeEnum.STRING, String.class);
	public static DataDescriptor<Void> void0 = new DataDescriptor<Void>(TypeEnum.VOID, Void.class) {
		@Override
		public Void copy(final Void object) {
			return null;
		}
	};
	public static DataDescriptor<Object> object = new DataDescriptor<Object>(TypeEnum.OBJECT,
			Object.class) {
		@Override
		public Object copy(final Object object) {
			return object;
		}
	};

	public static <T> ListDescriptor<T> list(final DataDescriptor<T> element) {
		return new ListDescriptor<T>(element);
	}

	public static <T> SetDescriptor<T> set(final DataDescriptor<T> element) {
		return new SetDescriptor<T>(element);
	}

	public static <K, V> MapDescriptor<K, V> map(final DataDescriptor<K> key,
			final DataDescriptor<V> value) {
		return new MapDescriptor<K, V>(key, value);
	}
}
