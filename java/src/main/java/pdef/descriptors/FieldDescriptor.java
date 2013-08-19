package pdef.descriptors;

import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;

public class FieldDescriptor {
	private final MessageDescriptor message;
	private final String name;
	private final Supplier<Descriptor> type;
	private final boolean discriminator;
	private final Accessor accessor;

	private FieldDescriptor(final Builder builder, final MessageDescriptor message) {
		this.message = checkNotNull(message);
		name = checkNotNull(builder.name);
		type = checkNotNull(builder.type);
		accessor = checkNotNull(builder.accessor);
		discriminator = checkNotNull(builder.discriminator);
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
		return accessor.get(message);
	}

	public void set(final Object builder, final Object value) {
		accessor.set(builder, value);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String name;
		private Supplier<Descriptor> type;
		private boolean discriminator;
		private Accessor accessor;

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

		public Builder setAccessor(final Accessor accessor) {
			this.accessor = accessor;
			return this;
		}

		public FieldDescriptor build(final MessageDescriptor message) {
			return new FieldDescriptor(this, message);
		}
	}

	public static interface Accessor {
		Object get(Object message);

		void set(Object message, Object value);
	}
}
