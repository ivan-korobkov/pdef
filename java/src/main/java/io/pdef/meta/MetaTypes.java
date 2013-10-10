package io.pdef.meta;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.*;

import java.util.*;

/**
 * Primitive metatypes and collection metatype factories.
 * */
public class MetaTypes {
	private MetaTypes() {}

	private abstract static class PrimitiveType<T> extends DataType<T> {
		protected PrimitiveType(final TypeEnum type) {
			super(type);
		}

		@Override
		public T copy(final T object) {
			return object;
		}

		@Override
		public String toString(final T object) {
			return object == null ? null : object.toString();
		}

		@Override
		protected Object toNative(final T object) throws Exception {
			return object;
		}
	}

	public static DataType<Boolean> bool = new PrimitiveType<Boolean>(TypeEnum.BOOL) {
		@Override
		public Boolean fromNative(final Object object) throws Exception {
			return object instanceof String ? fromString((String) object) : (Boolean) object;
		}

		@Override
		public Boolean fromString(final String s) {
			return s != null && Boolean.parseBoolean(s);
		}
	};

	public static DataType<Short> int16 = new PrimitiveType<Short>(TypeEnum.INT16) {
		@Override
		public Short fromNative(final Object object) throws Exception {
			if (object == null) {
				return null;
			}
			if (object instanceof String) {
				return fromString((String) object);
			}

			return object instanceof Short ? (Short) object : ((Number) object).shortValue();
		}

		@Override
		public Short fromString(final String s) {
			return s == null ? null : Short.parseShort(s);
		}
	};

	public static DataType<Integer> int32 = new PrimitiveType<Integer>(TypeEnum.INT32) {
		@Override
		public Integer fromNative(final Object object) throws Exception {
			if (object == null) {
				return null;
			}
			if (object instanceof String) {
				return fromString((String) object);
			}

			return object instanceof Integer ? (Integer) object : ((Number) object).intValue();
		}

		@Override
		public Integer fromString(final String s) {
			return s == null ? null : Integer.parseInt(s);
		}
	};

	public static DataType<Long> int64 = new PrimitiveType<Long>(TypeEnum.INT64) {
		@Override
		public Long fromNative(final Object object) throws Exception {
			if (object == null) {
				return null;
			}
			if (object instanceof String) {
				return fromString((String) object);
			}

			return object instanceof Long ? (Long) object : ((Number) object).longValue();
		}

		@Override
		public Long fromString(final String s) {
			return s == null ? null : Long.parseLong(s);
		}
	};

	public static DataType<Float> float0 = new PrimitiveType<Float>(TypeEnum.FLOAT) {
		@Override
		public Float fromNative(final Object object) throws Exception {
			if (object == null) {
				return null;
			}
			if (object instanceof String) {
				return fromString((String) object);
			}

			return object instanceof Float ? (Float) object : ((Number) object).floatValue();
		}

		@Override
		public Float fromString(final String s) {
			return s == null ? null : Float.parseFloat(s);
		}
	};

	public static DataType<Double> double0 = new PrimitiveType<Double>(TypeEnum.DOUBLE) {
		@Override
		public Double fromNative(final Object object) throws Exception {
			if (object == null) {
				return null;
			}
			if (object instanceof String) {
				return fromString((String) object);
			}

			return object instanceof Double ? (Double) object : ((Number) object).doubleValue();
		}

		@Override
		public Double fromString(final String s) {
			return s == null ? null : Double.parseDouble(s);
		}
	};

	public static DataType<String> string = new PrimitiveType<String>(TypeEnum.STRING) {
		@Override
		public String fromNative(final Object object) {
			return (String) object;
		}

		@Override
		public String fromString(final String s) {
			return s;
		}
	};

	public static DataType<Void> void0 = new DataType<Void>(TypeEnum.VOID) {
		@Override
		public Void copy(final Void object) {
			return null;
		}

		@Override
		public Void fromNative(final Object object) {
			return null;
		}

		@Override
		protected Object toNative(final Void object) throws Exception {
			return null;
		}
	};

	public static DataType<Object> object = new DataType<Object>(TypeEnum.OBJECT) {
		@Override
		public Object copy(final Object object) {
			return object;
		}

		@Override
		public Object fromNative(final Object object) {
			return object;
		}

		@Override
		public Object toNative(final Object object) {
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

	private static class ListType<T> extends DataType<List<T>> {
		private final DataType<T> element;

		public ListType(final DataType<T> element) {
			super(TypeEnum.LIST);
			this.element = checkNotNull(element);
		}

		@Override
		public List<T> copy(final List<T> list) {
			if (list == null) {
				return null;
			}

			List<T> copy = Lists.newArrayList();
			for (T e : list) {
				T copied = element.copy(e);
				copy.add(copied);
			}

			return copy;
		}

		@Override
		public List<T> fromNative(final Object object) throws Exception {
			if (object == null) {
				return null;
			}

			Collection<?> collection = (Collection<?>) object;
			List<T> result = Lists.newArrayList();

			for (Object elem : collection) {
				T parsed = element.parseFromNative(elem);
				result.add(parsed);
			}

			return result;
		}

		@Override
		public List<Object> toNative(final List<T> list) throws Exception {
			if (list == null) return null;

			List<Object> result = Lists.newArrayList();
			for (T elem : list) {
				Object serialized = element.serializeToNative(elem);
				result.add(serialized);
			}

			return result;
		}
	}

	private static class SetType<T> extends DataType<Set<T>> {
		private final DataType<T> element;

		public SetType(final DataType<T> element) {
			super(TypeEnum.SET);
			this.element = checkNotNull(element);
		}

		@Override
		public Set<T> copy(final Set<T> set) {
			if (set == null) {
				return null;
			}

			Set<T> copy = Sets.newHashSet();
			for (T elem : set) {
				T copied = element.copy(elem);
				copy.add(copied);
			}

			return copy;
		}

		@Override
		public Set<T> fromNative(final Object object) throws Exception {
			if (object == null) {
				return null;
			}

			Collection<?> collection = (Collection<?>) object;
			Set<T> result = Sets.newHashSet();

			for (Object elem : collection) {
				T parsed = element.parseFromNative(elem);
				result.add(parsed);
			}

			return result;
		}

		@Override
		public Set<Object> toNative(final Set<T> set) throws Exception {
			if (set == null) {
				return null;
			}

			Set<Object> result = Sets.newHashSet();
			for (T elem : set) {
				Object serialized = element.serializeToNative(elem);
				result.add(serialized);
			}

			return result;
		}
	}

	private static class MapType<K, V> extends DataType<Map<K, V>> {
		private final DataType<K> key;
		private final DataType<V> value;

		public MapType(final DataType<K> key, final DataType<V> value) {
			super(TypeEnum.MAP);
			this.key = checkNotNull(key);
			this.value = checkNotNull(value);
		}

		@Override
		public Map<K, V> copy(final Map<K, V> map) {
			if (map == null) {
				return null;
			}

			Map<K, V> copy = Maps.newHashMap();
			for (Map.Entry<K, V> entry : map.entrySet()) {
				K ck = key.copy(entry.getKey());
				V cv = value.copy(entry.getValue());
				copy.put(ck, cv);
			}

			return copy;
		}

		@Override
		public Map<K, V> fromNative(final Object object) throws Exception {
			if (object == null) {
				return null;
			}

			Map<?, ?> map = (Map<?, ?>) object;
			Map<K, V> result = Maps.newHashMap();

			for (Map.Entry<?, ?> e : map.entrySet()) {
				K k = key.fromNative(e.getKey());
				V v = value.fromNative(e.getValue());
				result.put(k, v);
			}

			return result;
		}

		@Override
		public Map<Object, Object> toNative(final Map<K, V> map) throws Exception {
			if (map == null) {
				return null;
			}

			Map<Object, Object> result = Maps.newHashMap();
			for (Map.Entry<K, V> e : map.entrySet()) {
				Object k = key.serializeToNative(e.getKey());
				Object v = value.serializeToNative(e.getValue());
				result.put(k, v);
			}

			return result;
		}
	}
}
