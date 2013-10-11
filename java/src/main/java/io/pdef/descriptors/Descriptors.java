package io.pdef.descriptors;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Primitive descriptors and collection descriptor factories. */
public class Descriptors {
	private Descriptors() {}

	private static class PrimitiveDescriptor<T> extends DataDescriptor<T> {
		public static <T> PrimitiveDescriptor<T> of(final TypeEnum type) {
			return new PrimitiveDescriptor<T>(type);
		}

		private PrimitiveDescriptor(final TypeEnum type) {
			super(type);
		}

		@Override
		public T copy(final T object) {
			return object;
		}
	}

	public static DataDescriptor<Boolean> bool = PrimitiveDescriptor.of(TypeEnum.BOOL);
	public static DataDescriptor<Short> int16 = PrimitiveDescriptor.of(TypeEnum.INT16);
	public static DataDescriptor<Integer> int32 = PrimitiveDescriptor.of(TypeEnum.INT32);
	public static DataDescriptor<Long> int64 = PrimitiveDescriptor.of(TypeEnum.INT64);
	public static DataDescriptor<Float> float0 = PrimitiveDescriptor.of(TypeEnum.FLOAT);
	public static DataDescriptor<Double> double0 = PrimitiveDescriptor.of(TypeEnum.DOUBLE);
	public static DataDescriptor<String> string = PrimitiveDescriptor.of(TypeEnum.STRING);
	public static DataDescriptor<Void> void0 = new DataDescriptor<Void>(TypeEnum.VOID) {
		@Override
		public Void copy(final Void object) {
			return null;
		}
	};
	public static DataDescriptor<Object> object = new DataDescriptor<Object>(TypeEnum.OBJECT) {
		@Override
		public Object copy(final Object object) {
			return object;
		}
	};

	public static <T> DataDescriptor<List<T>> list(final DataDescriptor<T> element) {
		return new ListDescriptor<T>(element);
	}

	public static <T> DataDescriptor<Set<T>> set(final DataDescriptor<T> element) {
		return new SetDescriptor<T>(element);
	}

	public static <K, V> DataDescriptor<Map<K, V>> map(final DataDescriptor<K> key,
			final DataDescriptor<V> value) {
		return new MapDescriptor<K, V>(key, value);
	}
}
