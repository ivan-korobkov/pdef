package io.pdef.descriptors;

import com.google.common.collect.*;
import io.pdef.TypeEnum;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/** Default primitive and collection descriptors. */
public class Descriptors {
	private Descriptors() {}

	public static PrimitiveDescriptor bool = new PrimitiveDescriptor(TypeEnum.BOOL) {
		@Override
		public Boolean parseObject(final Object o) {
			if (o instanceof String) return parseString((String) o);
			return (Boolean) o;
		}

		@Override
		public Boolean toObject(final Object o) {
			return (Boolean) o;
		}

		@Override
		public Boolean parseString(final String s) {
			return s != null && Boolean.parseBoolean(s);
		}
	};

	public static PrimitiveDescriptor int16 = new PrimitiveDescriptor(TypeEnum.INT16) {
		@Override
		public Short parseObject(final Object o) {
			if (o instanceof String) return parseString((String) o);
			return toObject(o);
		}

		@Override
		public Short toObject(final Object o) {
			if (o == null) return null;
			return o instanceof Short ? (Short) o : ((Number) o).shortValue();
		}

		@Override
		public Short parseString(final String s) {
			return s == null ? null : Short.parseShort(s);
		}
	};

	public static PrimitiveDescriptor int32 = new PrimitiveDescriptor(TypeEnum.INT32) {
		@Override
		public Integer parseObject(final Object o) {
			if (o instanceof String) return parseString((String) o);
			return toObject(o);
		}

		@Override
		public Integer toObject(final Object o) {
			if (o == null) return null;
			return o instanceof Integer ? (Integer) o : ((Number) o).intValue();
		}

		@Override
		public Integer parseString(final String s) {
			return s == null ? null : Integer.parseInt(s);
		}
	};

	public static PrimitiveDescriptor int64 = new PrimitiveDescriptor(TypeEnum.INT64) {
		@Override
		public Long parseObject(final Object o) {
			if (o instanceof String) return parseString((String) o);
			return toObject(o);
		}

		@Override
		public Long toObject(final Object o) {
			if (o == null) return null;
			return o instanceof Long ? (Long) o : ((Number) o).longValue();
		}

		@Override
		public Long parseString(final String s) {
			return s == null ? null : Long.parseLong(s);
		}
	};

	public static PrimitiveDescriptor float0 = new PrimitiveDescriptor(TypeEnum.FLOAT) {
		@Override
		public Float parseObject(final Object o) {
			if (o instanceof String) return parseString((String) o);
			return toObject(o);
		}

		@Override
		public Float toObject(final Object o) {
			if (o == null) return null;
			return o instanceof Float ? (Float) o : ((Number) o).floatValue();
		}

		@Override
		public Float parseString(final String s) {
			return s == null ? null : Float.parseFloat(s);
		}
	};

	public static PrimitiveDescriptor double0 = new PrimitiveDescriptor(TypeEnum.DOUBLE) {
		@Override
		public Double parseObject(final Object o) {
			if (o instanceof String) return parseString((String) o);
			return toObject(o);
		}

		@Override
		public Double toObject(final Object o) {
			if (o == null) return null;
			return o instanceof Double ? (Double) o : ((Number) o).doubleValue();
		}

		@Override
		public Double parseString(final String s) {
			return s == null ? null : Double.parseDouble(s);
		}
	};

	public static PrimitiveDescriptor string = new PrimitiveDescriptor(TypeEnum.STRING) {
		@Override
		public String parseObject(final Object o) {
			return (String) o;
		}

		@Override
		public String toObject(final Object o) {
			return (String) o;
		}

		@Override
		public String parseString(final String s) {
			return s;
		}
	};

	public static DataDescriptor void0 = new DataDescriptor(TypeEnum.VOID) {
		@Override
		public Void parseObject(final Object o) {
			return null;
		}

		@Override
		public Void toObject(final Object o) {
			return null;
		}
	};

	public static DataDescriptor object = new DataDescriptor(TypeEnum.OBJECT) {
		@Override
		public Object parseObject(final Object o) {
			return o;
		}

		@Override
		public Object toObject(final Object o) {
			return o;
		}
	};

	public static DataDescriptor list(final DataDescriptor element) {
		checkNotNull(element);
		return new DataDescriptor(TypeEnum.LIST) {
			@Override
			public List<?> parseObject(final Object o) {
				if (o == null) return null;

				Collection<?> collection = (Collection<?>) o;
				List<Object> result = Lists.newArrayList();
				for (Object e : collection) {
					Object r = element.parseObject(e);
					result.add(r);
				}

				return ImmutableList.copyOf(result);
			}

			@Override
			public List<Object> toObject(final Object o) {
				if (o == null) return null;

				List<?> list = (List<?>) o;
				List<Object> result = Lists.newArrayList();
				for (Object e : list) {
					result.add(element.toObject(e));
				}

				return result;
			}
		};
	}

	public static DataDescriptor set(final DataDescriptor element) {
		checkNotNull(element);
		return new DataDescriptor(TypeEnum.SET) {
			@Override
			public Set<?> parseObject(final Object o) {
				if (o == null) return null;

				Collection<?> collection = (Collection<?>) o;
				List<Object> result = Lists.newArrayList();
				for (Object e : collection) {
					if (e == null) continue;
					Object r = element.parseObject(e);
					result.add(r);
				}

				return ImmutableSet.copyOf(result);
			}

			@Override
			public Set<Object> toObject(final Object o) {
				if (o == null) return null;

				Set<?> set = (Set<?>) o;
				Set<Object> result = Sets.newHashSet();
				for (Object e : set) {
					if (e == null) continue;
					result.add(element.toObject(e));
				}

				return result;
			}
		};
	}

	public static DataDescriptor map(final DataDescriptor key, final DataDescriptor value) {
		checkNotNull(key);
		checkNotNull(value);
		return new DataDescriptor(TypeEnum.MAP) {
			@Override
			public Map<?, ?> parseObject(final Object o) {
				if (o == null) return null;

				Map<?, ?> map = (Map<?, ?>) o;
				Map<Object, Object> result = Maps.newHashMap();
				for (Map.Entry<?, ?> e : map.entrySet()) {
					Object k = key.parseObject(e.getKey());
					Object v = value.parseObject(e.getValue());
					if (k == null) continue;
					result.put(k, v);
				}

				return ImmutableMap.copyOf(result);
			}

			@Override
			public Map<Object, Object> toObject(final Object o) {
				if (o == null) return null;

				Map<?, ?> map = (Map<?, ?>) o;
				Map<Object, Object> result = Maps.newHashMap();
				for (Map.Entry<?, ?> e : map.entrySet()) {
					Object k = key.toObject(e.getKey());
					Object v = value.toObject(e.getValue());
					result.put(k, v);
				}

				return result;
			}
		};
	}
}
