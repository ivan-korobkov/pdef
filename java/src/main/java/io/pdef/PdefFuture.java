package io.pdef;

import java.lang.reflect.Type;

/** Pdef future descriptor. */
public class PdefFuture extends PdefDescriptor {
	private final PdefDescriptor element;

	PdefFuture(final Type javaType, final Pdef pdef) {
		super(PdefType.FUTURE, javaType, pdef);
		element = pdef.get(Pdef.actualTypeArgs(javaType)[0]);
	}

	public PdefDescriptor getElement() {
		return element;
	}
}
