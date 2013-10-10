package io.pdef.meta;

import java.util.*;

/**
 * Primitive metatypes and collection metatype factories.
 * */
public class MetaTypes {
	private MetaTypes() {}

	private static class PrimitiveType<T> extends DataType<T> {
		public static <T> PrimitiveType<T> of(final TypeEnum type) {
			return new PrimitiveType<T>(type);
		}

		private PrimitiveType(final TypeEnum type) {
			super(type);
		}

		@Override
		public T copy(final T object) {
			return object;
		}
	}

	public static DataType<Boolean> bool = PrimitiveType.of(TypeEnum.BOOL);
	public static DataType<Short> int16 = PrimitiveType.of(TypeEnum.INT16);
	public static DataType<Integer> int32 = PrimitiveType.of(TypeEnum.INT32);
	public static DataType<Long> int64 = PrimitiveType.of(TypeEnum.INT64);
	public static DataType<Float> float0 = PrimitiveType.of(TypeEnum.FLOAT);
	public static DataType<Double> double0 = PrimitiveType.of(TypeEnum.DOUBLE);
	public static DataType<String> string = PrimitiveType.of(TypeEnum.STRING);
	public static DataType<Void> void0 = new DataType<Void>(TypeEnum.VOID) {
		@Override
		public Void copy(final Void object) {
			return null;
		}
	};
	public static DataType<Object> object = new DataType<Object>(TypeEnum.OBJECT) {
		@Override
		public Object copy(final Object object) {
			return object;
		}
	};

	public static <T> DataType<List<T>> list(final DataType<T> element) {
		return new ListType<T>(element);
	}

	public static <T> DataType<Set<T>> set(final DataType<T> element) {
		return new SetType<T>(element);
	}

	public static <K, V> DataType<Map<K, V>> map(final DataType<K> key, final DataType<V> value) {
		return new MapType<K, V>(key, value);
	}
}
