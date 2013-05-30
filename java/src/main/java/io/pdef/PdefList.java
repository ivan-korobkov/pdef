package io.pdef;

import com.google.common.base.Objects;

import java.lang.reflect.Type;
import java.util.List;

import static io.pdef.Pdef.actualTypeArgs;

/** Pdef list descriptor. */
public class PdefList extends PdefDescriptor {
	private final PdefDescriptor element;

	PdefList(final Type javaType, final Pdef pdef) {
		super(PdefType.LIST, javaType, pdef);
		element = descriptorOf(actualTypeArgs(javaType)[0]);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).addValue(element).toString();
	}

	@Override
	public Class<?> getJavaClass() {
		return List.class;
	}

	public PdefDescriptor getElement() {
		return element;
	}
}
