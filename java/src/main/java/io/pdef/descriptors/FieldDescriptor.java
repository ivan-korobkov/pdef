package io.pdef.descriptors;

import io.pdef.Provider;
import io.pdef.Providers;

/**
 * FieldDescriptor holds a field name, type, setters and getters.
 * @param <M> Message class.
 * @param <V> Field value class.
 */
public class FieldDescriptor<M, V> {
	private final String name;
	private final Provider<DataDescriptor<V>> typeProvider;
	private final FieldAccessor<M, V> accessor;
	private final boolean discriminator;
	private DataDescriptor<V> type;

	protected FieldDescriptor(final Builder<M, V> builder) {
		name = builder.name;
		typeProvider = builder.type;
		accessor = builder.accessor;
		discriminator = builder.discriminator;

		if (name == null) throw new NullPointerException("name");
		if (typeProvider == null) throw new NullPointerException("type");
		if (accessor == null) throw new NullPointerException("accessor");
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
		return accessor.get(message);
	}

	public void set(final M message, final V value) {
		accessor.set(message, value);
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
		private FieldAccessor<M, V> accessor;

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

		public Builder<M, V> setAccessor(final FieldAccessor<M, V> accessor) {
			if (accessor == null) throw new NullPointerException("accessor");
			this.accessor = accessor;
			return this;
		}

		public Builder<M, V> setReflexAccessor(final Class<M> cls) {
			if (name == null) {
				throw new NullPointerException("Name must be set before the reflex accessor");
			}
			FieldAccessor<M, V> accessor = new ReflexFieldAccessor<M, V>(name, cls);
			return setAccessor(accessor);
		}

		public FieldDescriptor<M, V> build() {
			return new FieldDescriptor<M, V>(this);
		}
	}
}
