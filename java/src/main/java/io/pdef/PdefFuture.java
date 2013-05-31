package io.pdef;

import java.lang.reflect.Type;

import static io.pdef.Pdef.actualTypeArgs;

/** Pdef future descriptor. */
public class PdefFuture extends PdefDescriptor {
	private final PdefDescriptor element;

	PdefFuture(final Type javaType, final Pdef pdef) {
		super(PdefType.FUTURE, javaType, pdef);
		element = pdef.get(actualTypeArgs(javaType)[0]);
	}

	public PdefDescriptor getElement() {
		return element;
	}
}
