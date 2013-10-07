package io.pdef.descriptors;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;

public class FieldDescriptor {
	private final MessageDescriptor message;
	private final String name;
	private final Supplier<Descriptor> type;
	private final boolean discriminator;
	private final Getter getter;
	private final Setter setter;

	private FieldDescriptor(final Builder builder, final MessageDescriptor message) {
		this.message = checkNotNull(message);
		name = checkNotNull(builder.name);
		type = checkNotNull(builder.type);
		getter = checkNotNull(builder.getter);
		setter = checkNotNull(builder.setter);
		discriminator = checkNotNull(builder.discriminator);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.addValue(type)
				.toString();
	}

	public MessageDescriptor getMessage() {
		return message;
	}

	public String getName() {
		return name;
	}

	public DataDescriptor getType() {
		return (DataDescriptor) type.get();
	}

	public boolean isDiscriminator() {
		return discriminator;
	}

	public Object get(final Object message) {
		return getter.get(message);
	}

	public void set(final Object builder, final Object value) {
		setter.set(builder, value);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String name;
		private Supplier<Descriptor> type;
		private boolean discriminator;
		private Getter getter;
		private Setter setter;

		private Builder() {}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setType(final Supplier<Descriptor> type) {
			this.type = type;
			return this;
		}

		public Builder setDiscriminator(final boolean discriminator) {
			this.discriminator = discriminator;
			return this;
		}

		public Builder setGetter(final Getter getter) {
			this.getter = getter;
			return this;
		}

		public Builder setSetter(final Setter setter) {
			this.setter = setter;
			return this;
		}

		public FieldDescriptor build(final MessageDescriptor message) {
			return new FieldDescriptor(this, message);
		}
	}

	public static interface Getter {
		Object get(Object message);
	}

	public static interface Setter {
		void set(Object builder, Object value);
	}
}
