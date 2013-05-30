package io.pdef;

import com.google.common.base.Objects;

import java.lang.reflect.Type;

/** Pdef type descriptor. */
public class PdefDescriptor {
	protected final Type javaType;
	protected final PdefType type;
	protected final Pdef pdef;

	PdefDescriptor(final PdefType type, final Type javaType, final Pdef pdef) {
		this.javaType = javaType;
		this.type = type;
		this.pdef = pdef;
		this.pdef.add(javaType, this);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getJavaClass().getSimpleName())
				.toString();
	}

	public Pdef getPdef() {
		return pdef;
	}

	public Type getJavaType() {
		return javaType;
	}

	public Class<?> getJavaClass() {
		return (Class<?>) javaType;
	}

	public PdefType getType() {
		return type;
	}

	public PdefList asList() {
		return (PdefList) this;
	}

	public PdefSet asSet() {
		return (PdefSet) this;
	}

	public PdefMap asMap() {
		return (PdefMap) this;
	}

	public PdefMessage asMessage() {
		return (PdefMessage) this;
	}

	public PdefEnum asEnum() {
		return (PdefEnum) this;
	}

	/** Returns a descriptor for a java type. */
	protected PdefDescriptor descriptorOf(final Type javaType1) {
		return pdef.get(javaType1);
	}
}
