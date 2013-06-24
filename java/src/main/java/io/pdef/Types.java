package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class Types {
	private Types() {}

	public static Type<Boolean> bool = new Type<Boolean>() {
		@Override
		public Boolean getDefault() {
			return false;
		}

		@Override
		public Boolean parse(final Object object) {
			return object == null ? false : (Boolean) object;
		}

		@Override
		public Boolean serialize(final Boolean value) {
			return value == null ? false : value;
		}
	};

	public static Type<Short> int16 = new Type<Short>() {
		@Override
		public Short getDefault() {
			return (short) 0;
		}

		@Override
		public Short parse(final Object object) {
			return object == null ? 0 : ((Number) object).shortValue();
		}

		@Override
		public Short serialize(final Short value) {
			return value == null ? (short) 0 : value;
		}
	};

	public static Type<Integer> int32 = new Type<Integer>() {
		@Override
		public Integer getDefault() {
			return 0;
		}

		@Override
		public Integer parse(final Object object) {
			return object == null ? 0 : ((Number) object).intValue();
		}

		@Override
		public Integer serialize(final Integer value) {
			return value == null ? 0 : value;
		}
	};

	public static Type<Long> int64 = new Type<Long>() {
		@Override
		public Long getDefault() {
			return 0L;
		}

		@Override
		public Long parse(final Object object) {
			return object == null ? 0 : ((Number) object).longValue();
		}

		@Override
		public Long serialize(final Long value) {
			return value == null ? 0L : value;
		}
	};

	public static Type<Float> float0 = new Type<Float>() {
		@Override
		public Float getDefault() {
			return 0f;
		}

		@Override
		public Float parse(final Object object) {
			return object == null ? 0 : ((Number) object).floatValue();
		}

		@Override
		public Float serialize(final Float value) {
			return value == null ? 0f : value;
		}
	};

	public static Type<Double> double0 = new Type<Double>() {
		@Override
		public Double getDefault() {
			return 0d;
		}

		@Override
		public Double parse(final Object object) {
			return object == null ? 0 : ((Number) object).doubleValue();
		}

		@Override
		public Double serialize(final Double value) {
			return value == null ? 0d : value;
		}
	};

	public static Type<String> string = new Type<String>() {
		@Override
		public String getDefault() {
			return null;
		}

		@Override
		public String parse(final Object object) {
			return (String) object;
		}

		@Override
		public String serialize(final String value) {
			return value;
		}
	};

	public static Type<Void> void0 = new Type<Void>() {
		@Override
		public Void getDefault() {
			return null;
		}

		@Override
		public Void parse(final Object object) {
			return null;
		}

		@Override
		public Void serialize(final Void value) {
			return null;
		}
	};

	public static Type<Object> object = new Type<Object>() {
		@Override
		public Object getDefault() {
			return null;
		}

		@Override
		public Object parse(final Object object) {
			return object;
		}

		@Override
		public Object serialize(final Object value) {
			return object;
		}
	};

	public static <T> Type<T> enum0(final T defaultValue) {
		return null;
	}

	public static <T> Type<List<T>> list(final Type<T> element) {
		checkNotNull(element);
		return new Type<List<T>>() {
			@Override
			public List<T> getDefault() {
				return ImmutableList.of();
			}

			@Override
			public List<T> parse(final Object object) {
				if (object == null) return ImmutableList.of();

				List<?> list = (List<?>) object;
				List<T> result = Lists.newArrayList();
				for (Object e : list) {
					T r = element.parse(e);
					result.add(r);
				}

				return ImmutableList.copyOf(result);
			}

			@Override
			public Object serialize(final List<T> value) {
				return null;
			}
		};
	}

	public static <T> Type<Set<T>> set(final Type<T> element) {
		checkNotNull(element);
		return new Type<Set<T>>() {
			@Override
			public Set<T> getDefault() {
				return ImmutableSet.of();
			}

			@Override
			public Set<T> parse(final Object object) {
				if (object == null) return ImmutableSet.of();

				List<?> list = (List<?>) object;
				List<T> result = Lists.newArrayList();
				for (Object e : list) {
					T r = element.parse(e);
					result.add(r);
				}

				return ImmutableSet.copyOf(result);
			}

			@Override
			public Object serialize(final Set<T> value) {
				return null;
			}
		};
	}

	public static <K, V> Type<Map<K, V>> map(final Type<K> key,
			final Type<V> value) {
		checkNotNull(key);
		checkNotNull(value);
		return new Type<Map<K, V>>() {
			@Override
			public Map<K, V> getDefault() {
				return ImmutableMap.of();
			}

			@Override
			public Map<K, V> parse(final Object object) {
				return null;
			}

			@Override
			public Object serialize(final Map<K, V> value) {
				return null;
			}
		};
	}
}
