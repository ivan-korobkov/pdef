package io.pdef.meta;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;

/**
 * MetaType is a base class for Pdef metatypes. Metatype provides structural information about a
 * Java type in runtime. For example, a message metatype allows to explore declared fields,
 * inherited fields, etc.
 * */
public class MetaType {
	private final TypeEnum type;

	protected MetaType(final TypeEnum type) {
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
