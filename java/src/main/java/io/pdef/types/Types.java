package io.pdef.types;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.*;

import java.util.*;

/** Primitive types and collection type factories. */
public class Types {
	private Types() {}

	private abstract static class PrimitiveType extends DataType {
		protected PrimitiveType(final TypeEnum type, final Class<?> javaClass) {
			super(type, javaClass);
		}

		@Override
		public Object copy(final Object object) {
			return object;
		}

		@Override
		public String doToString(final Object o) {
			return o == null ? null : o.toString();
		}
	}

	public static PrimitiveType bool = new PrimitiveType(TypeEnum.BOOL, Boolean.class) {
		@Override
		public Boolean doParseNative(final Object o) throws Exception {
			return o instanceof String ? doParseString((String) o) : doToNative(o);
		}

		@Override
		public Boolean doToNative(final Object o) {
			return (Boolean) o;
		}

		@Override
		public Boolean doParseString(final String s) {
			return s != null && Boolean.parseBoolean(s);
		}
	};

	public static PrimitiveType int16 = new PrimitiveType(TypeEnum.INT16, Short.class) {
		@Override
		public Short doParseNative(final Object o) throws Exception {
			return o instanceof String ? doParseString((String) o) : doToNative(o);
		}

		@Override
		public Short doToNative(final Object o) {
			if (o == null) return null;
			return o instanceof Short ? (Short) o : ((Number) o).shortValue();
		}

		@Override
		public Short doParseString(final String s) {
			return s == null ? null : Short.parseShort(s);
		}
	};

	public static PrimitiveType int32 = new PrimitiveType(TypeEnum.INT32, Integer.class) {
		@Override
		public Integer doParseNative(final Object o) throws Exception {
			return o instanceof String ? doParseString((String) o) : doToNative(o);
		}

		@Override
		public Integer doToNative(final Object o) {
			if (o == null) return null;
			return o instanceof Integer ? (Integer) o : ((Number) o).intValue();
		}

		@Override
		public Integer doParseString(final String s) {
			return s == null ? null : Integer.parseInt(s);
		}
	};

	public static PrimitiveType int64 = new PrimitiveType(TypeEnum.INT64, Long.class) {
		@Override
		public Long doParseNative(final Object o) throws Exception {
			return o instanceof String ? doParseString((String) o) : doToNative(o);
		}

		@Override
		public Long doToNative(final Object o) {
			if (o == null) return null;
			return o instanceof Long ? (Long) o : ((Number) o).longValue();
		}

		@Override
		public Long doParseString(final String s) {
			return s == null ? null : Long.parseLong(s);
		}
	};

	public static PrimitiveType float0 = new PrimitiveType(TypeEnum.FLOAT, Float.class) {
		@Override
		public Object doParseNative(final Object o) throws Exception {
			return o instanceof String ? doParseString((String) o) : doToNative(o);
		}

		@Override
		public Float doToNative(final Object o) {
			if (o == null) return null;
			return o instanceof Float ? (Float) o : ((Number) o).floatValue();
		}

		@Override
		public Float doParseString(final String s) {
			return s == null ? null : Float.parseFloat(s);
		}
	};

	public static PrimitiveType double0 = new PrimitiveType(TypeEnum.DOUBLE, Double.class) {
		@Override
		public Object doParseNative(final Object o) throws Exception {
			return o instanceof String ? doParseString((String) o) : doToNative(o);
		}

		@Override
		public Double doToNative(final Object o) {
			if (o == null) return null;
			return o instanceof Double ? (Double) o : ((Number) o).doubleValue();
		}

		@Override
		public Double doParseString(final String s) {
			return s == null ? null : Double.parseDouble(s);
		}
	};

	public static PrimitiveType string = new PrimitiveType(TypeEnum.STRING, String.class) {
		@Override
		public String doParseNative(final Object o) {
			return (String) o;
		}

		@Override
		public String doToNative(final Object o) {
			return (String) o;
		}

		@Override
		public String doParseString(final String s) {
			return s;
		}
	};

	public static DataType void0 = new DataType(TypeEnum.VOID, Void.class) {
		@Override
		public Object copy(final Object object) {
			return object;
		}

		@Override
		public Void doParseNative(final Object o) {
			return null;
		}

		@Override
		public Void doToNative(final Object o) {
			return null;
		}
	};

	public static DataType object = new DataType(TypeEnum.OBJECT, Object.class) {
		@Override
		public Object copy(final Object object) {
			return object;
		}

		@Override
		public Object doParseNative(final Object o) {
			return o;
		}

		@Override
		public Object doToNative(final Object o) {
			return o;
		}
	};

	public static DataType list(final DataType element) {
		return new ListType(element);
	}

	public static DataType set(final DataType element) {
		return new SetType(element);
	}

	public static DataType map(final DataType key, final DataType value) {
		return new MapType(key, value);
	}

	private static class ListType extends DataType {
		private final DataType element;

		public ListType(final DataType element) {
			super(TypeEnum.LIST, List.class);
			this.element = checkNotNull(element);
		}

		@Override
		public Object copy(final Object object) {
			if (object == null) {
				return null;
			}

			List<?> list = (List<?>) object;
			List<Object> copy = Lists.newArrayList();

			for (Object e : list) {
				Object copied = element.copy(e);
				copy.add(copied);
			}

			return copy;
		}

		@Override
		public List<?> doParseNative(final Object o) throws Exception {
			if (o == null) {
				return null;
			}

			Collection<?> collection = (Collection<?>) o;
			List<Object> result = Lists.newArrayList();

			for (Object e : collection) {
				Object r = element.doParseNative(e);
				result.add(r);
			}

			return ImmutableList.copyOf(result);
		}

		@Override
		public List<Object> doToNative(final Object o) throws Exception {
			if (o == null) return null;

			List<?> list = (List<?>) o;
			List<Object> result = Lists.newArrayList();
			for (Object e : list) {
				result.add(element.doToNative(e));
			}

			return result;
		}
	}

	private static class SetType extends DataType {
		private final DataType element;

		public SetType(final DataType element) {
			super(TypeEnum.SET, Set.class);
			this.element = checkNotNull(element);
		}

		@Override
		public Object copy(final Object object) {
			if (object == null) {
				return null;
			}

			Set<?> set = (Set<?>) object;
			Set<Object> copy = Sets.newHashSet();

			for (Object e : set) {
				Object copied = element.copy(e);
				copy.add(copied);
			}

			return copy;
		}

		@Override
		public Set<?> doParseNative(final Object o) throws Exception {
			if (o == null) {
				return null;
			}

			Collection<?> collection = (Collection<?>) o;
			List<Object> result = Lists.newArrayList();

			for (Object e : collection) {
				Object r = element.doParseNative(e);
				result.add(r);
			}

			return ImmutableSet.copyOf(result);
		}

		@Override
		public Set<Object> doToNative(final Object o) throws Exception {
			if (o == null) {
				return null;
			}

			Set<?> set = (Set<?>) o;
			Set<Object> result = Sets.newHashSet();
			for (Object e : set) {
				result.add(element.doToNative(e));
			}

			return result;
		}
	}

	private static class MapType extends DataType {
		private final DataType key;
		private final DataType value;

		public MapType(final DataType key, final DataType value) {
			super(TypeEnum.MAP, Map.class);
			this.key = checkNotNull(key);
			this.value = checkNotNull(value);
		}

		@Override
		public Object copy(final Object object) {
			if (object == null) {
				return null;
			}

			Map<?, ?> map = (Map<?, ?>) object;
			Map<Object, Object> copy = Maps.newHashMap();

			for (Map.Entry<?, ?> entry : map.entrySet()) {
				Object ck = key.copy(entry.getKey());
				Object cv = value.copy(entry.getValue());
				copy.put(ck, cv);
			}

			return copy;
		}

		@Override
		public Map<?, ?> doParseNative(final Object o) throws Exception {
			if (o == null) return null;

			Map<?, ?> map = (Map<?, ?>) o;
			Map<Object, Object> result = Maps.newHashMap();
			for (Map.Entry<?, ?> e : map.entrySet()) {
				Object k = key.doParseNative(e.getKey());
				Object v = value.doParseNative(e.getValue());
				result.put(k, v);
			}

			return ImmutableMap.copyOf(result);
		}

		@Override
		public Map<Object, Object> doToNative(final Object o) throws Exception {
			if (o == null) {
				return null;
			}

			Map<?, ?> map = (Map<?, ?>) o;
			Map<Object, Object> result = Maps.newHashMap();
			for (Map.Entry<?, ?> e : map.entrySet()) {
				Object k = key.doToNative(e.getKey());
				Object v = value.doToNative(e.getValue());
				result.put(k, v);
			}

			return result;
		}
	}
}
