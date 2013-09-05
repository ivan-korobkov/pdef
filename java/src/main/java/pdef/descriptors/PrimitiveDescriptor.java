package pdef.descriptors;

import pdef.TypeEnum;

public abstract class PrimitiveDescriptor extends DataDescriptor {
	protected PrimitiveDescriptor(final TypeEnum type) {
		super(type);
	}

	/** Parses a primitive from a string. */
	public abstract Object parseString(String s);

	/** Serializes a primitive to a string. */
	public String toString(Object o) {
		if (o == null) return null;
		return toObject(o).toString();
	}
}
