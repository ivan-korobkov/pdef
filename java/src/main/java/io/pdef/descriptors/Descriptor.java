package io.pdef.descriptors;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;

/**
 * Descriptor is a base class for Pdef descriptors. Descriptors provides information about
 * a Java type in runtime. For example, a message descriptor allows to explore declared fields,
 * inherited fields, etc.
 * */
public class Descriptor {
	private final TypeEnum type;

	protected Descriptor(final TypeEnum type) {
		this.type = checkNotNull(type);
	}

	/** Returns this type type. */
	public TypeEnum getType() {
		return type;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(type)
				.toString();
	}
}
