package io.pdef;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.Type;

/** Abstract pdef datatype. */
public abstract class PdefDatatype extends PdefDescriptor {

	PdefDatatype(final PdefType type, final Type javaType, final Pdef pdef) {
		super(type, javaType, pdef);
		checkArgument(type.isDatatype());
	}

	/** Returns the default value for this datatype. */
	public abstract Object getDefaultValue();
}
