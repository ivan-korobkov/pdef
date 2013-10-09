package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;

public abstract class MessageField<M, V> {
	private final String name;
	private final boolean discriminator;

	public MessageField(final String name, final boolean discriminator) {
		this.name = checkNotNull(name);
		this.discriminator = discriminator;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper("MessageField")
				.addValue(name)
				.toString();
	}

	public String getName() {
		return name;
	}

	public boolean isDiscriminator() {
		return discriminator;
	}

	public abstract DataType<V> getType();

	public abstract V get(final M message);

	public abstract void set(final M message, final V value);

	public void copy(final M src, final M dst) {
		V value = get(src);
		V copied = getType().copy(value);
		set(dst, copied);
	}

	/** Sets this field in a message to a value parsed from a native object. */
	public void setNative(final M message, final Object value) {
		if (value == null) {
			return;
		}

		V parsed = getType().parseNative(value);
		set(message, parsed);
	}

	/** Returns this field in a message converted to a native object. */
	public Object getNative(final M message) {
		V value = get(message);
		if (value == null) {
			return null;
		}

		return getType().toNative(value);
	}
}
