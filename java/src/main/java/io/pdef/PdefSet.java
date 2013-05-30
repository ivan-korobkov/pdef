package io.pdef;

import com.google.common.base.Objects;

import java.lang.reflect.Type;
import java.util.Set;

/** Pdef set descriptor. */
public class PdefSet extends PdefDescriptor {
	private final PdefDescriptor element;

	PdefSet(final Type javaType, final Pdef pdef) {
		super(PdefType.SET, javaType, pdef);
		element = descriptorOf(Pdef.actualTypeArgs(javaType)[0]);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(element)
				.toString();
	}

	@Override
	public Class<?> getJavaClass() {
		return Set.class;
	}

	public PdefDescriptor getElement() {
		return element;
	}
}
