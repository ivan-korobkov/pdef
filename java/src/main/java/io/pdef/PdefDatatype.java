package io.pdef;

import java.lang.reflect.Type;

import static com.google.common.base.Preconditions.checkArgument;

public class PdefDatatype extends PdefDescriptor {

	PdefDatatype(final PdefType type, final Type javaType, final Pdef pdef) {
		super(type, javaType, pdef);
		checkArgument(type.isDatatype());
	}
}
