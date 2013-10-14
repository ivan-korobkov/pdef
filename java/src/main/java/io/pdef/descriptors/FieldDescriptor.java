package io.pdef.descriptors;

import io.pdef.Provider;
import io.pdef.Providers;

import java.lang.reflect.Field;

/**
 * FieldDescriptor holds a field name, type, setters and getters.
 * @param <M> Message class.
 * @param <V> Field value class.
 */
public class FieldDescriptor<M, V> {
	private final String name;
	private final Provider<DataDescriptor<V>> typeProvider;
	private final FieldGetter<M, V> getter;
	private final FieldSetter<M, V> setter;
	private final boolean discriminator;
	private DataDescriptor<V> type;

	protected FieldDescriptor(final Builder<M, V> builder) {
		name = builder.name;
		typeProvider = builder.type;
		getter = builder.getter;
		setter = builder.setter;
		discriminator = builder.discriminator;

		if (name == null) throw new NullPointerException("name");
		if (typeProvider == null) throw new NullPointerException("type");
		if (getter == null) throw new NullPointerException("getter");
		if (setter == null) throw new NullPointerException("setter");
	}

	public static <M, V> Builder<M, V> builder() {
		return new Builder<M, V>();
	}

	@Override
	public String toString() {
		return "FieldDescriptor{'" + name + '\'' + '}';
	}

	public String getName() {
		return name;
	}

	public boolean isDiscriminator() {
		return discriminator;
	}

	public DataDescriptor<V> getType() {
		if (type != null) {
			return type;
		}

		return (type = typeProvider.get());
	}

	public V get(final M message) {
		return getter.get(message);
	}

	public void set(final M message, final V value) {
		setter.set(message, value);
	}

	public void copy(final M src, final M dst) {
		V value = get(src);
		V copied = getType().copy(value);
		set(dst, copied);
	}

	public static class Builder<M, V> {
		private String name;
		private boolean discriminator;
		private Provider<DataDescriptor<V>> type;
		private FieldGetter<M, V> getter;
		private FieldSetter<M, V> setter;

		protected Builder() {}

		public Builder<M, V> setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder<M, V> setDiscriminator(final boolean discriminator) {
			this.discriminator = discriminator;
			return this;
		}

		public Builder<M, V> setType(final Provider<DataDescriptor<V>> type) {
			this.type = type;
			return this;
		}

		public Builder<M, V> setType(final DataDescriptor<V> type) {
			if (type == null) throw new NullPointerException("type");
			this.type = Providers.ofInstance(type);
			return this;
		}

		public Builder<M, V> setGetter(final FieldGetter<M, V> getter) {
			this.getter = getter;
			return this;
		}

		public Builder<M, V> setSetter(final FieldSetter<M, V> setter) {
			this.setter = setter;
			return this;
		}

		public Builder<M, V> setAccessor(final FieldAccessor<M, V> accessor) {
			setGetter(accessor);
			setSetter(accessor);
			return this;
		}

		public Builder<M, V> setReflexAccessor(final Class<M> cls) {
			if (name == null) {
				throw new NullPointerException("Name must be set before the reflex accessor");
			}
			FieldAccessor<M, V> accessor = new ReflexAccessor<M, V>(name, cls);
			return setAccessor(accessor);
		}

		public FieldDescriptor<M, V> build() {
			return new FieldDescriptor<M, V>(this);
		}
	}

	private static class ReflexAccessor<M, V> implements FieldAccessor<M, V> {
		private final Field field;

		private ReflexAccessor(final String name, final Class<M> cls) {
			Field field1 = null;
			for (Field field0 : cls.getDeclaredFields()) {
				if (field0.getName().equals(name)) {
					field1 = field0;
					break;
				}
			}

			if (field1 == null) throw new IllegalArgumentException("Field not found: " + name);
			field = field1;
			field.setAccessible(true);
		}

		@SuppressWarnings("unchecked")
		@Override
		public V get(final M message) {
			try {
				return (V) field.get(message);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void set(final M message, final V value) {
			try {
				field.set(message, value);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
