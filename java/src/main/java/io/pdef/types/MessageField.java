package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;

public class MessageField {
	private final MessageType message;
	private final String name;
	private final Supplier<Type> type;
	private final boolean discriminator;
	private final Getter getter;
	private final Setter setter;

	private MessageField(final Builder builder, final MessageType message) {
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

	public MessageType getMessage() {
		return message;
	}

	public String getName() {
		return name;
	}

	public DataType getType() {
		return (DataType) type.get();
	}

	public boolean isDiscriminator() {
		return discriminator;
	}

	public Object get(final Object message) {
		return getter.get(message);
	}

	public void set(final Object message, final Object value) {
		setter.set(message, value);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static interface Getter {
		/** Gets this field value from a message. */
		Object get(Object message);
	}

	public static interface Setter {
		/** Sets this field value in a message. */
		void set(Object message, Object value);
	}

	public static class Builder {
		private String name;
		private Supplier<Type> type;
		private boolean discriminator;
		private Getter getter;
		private Setter setter;

		private Builder() {}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setType(final Supplier<Type> type) {
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

		public MessageField build(final MessageType message) {
			return new MessageField(this, message);
		}
	}
}
