package io.pdef;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Type;
import java.util.Map;

import static io.pdef.Pdef.actualTypeArgs;

/** Pdef map descriptor. */
public class PdefMap extends PdefDatatype {
	private final PdefDescriptor key;
	private final PdefDescriptor value;

	PdefMap(final Type javaType, final Pdef pdef) {
		super(PdefType.MAP, javaType, pdef);
		key = descriptorOf(actualTypeArgs(javaType)[0]);
		value = descriptorOf(actualTypeArgs(javaType)[1]);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(key)
				.addValue(value)
				.toString();
	}

	@Override
	public Type getJavaType() {
		return Map.class;
	}

	public PdefDescriptor getKey() {
		return key;
	}

	public PdefDescriptor getValue() {
		return value;
	}

	@Override
	public ImmutableMap<Object, Object> defaultValue() {
		return ImmutableMap.of();
	}
}
