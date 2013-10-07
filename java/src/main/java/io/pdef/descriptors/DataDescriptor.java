package io.pdef.descriptors;

import static com.google.common.base.Preconditions.*;
import io.pdef.TypeEnum;
import io.pdef.json.Json;

import javax.annotation.Nullable;

public abstract class DataDescriptor implements Descriptor {
	private final TypeEnum type;

	protected DataDescriptor(final TypeEnum type) {
		this.type = checkNotNull(type);
	}

	@Override
	public TypeEnum getType() {
		return type;
	}

	@Nullable
	public MessageDescriptor asMessageDescriptor() {
		return (this instanceof MessageDescriptor) ? (MessageDescriptor) this : null;
	}

	/** Parses a data type from a json string. */
	public Object parseJson(String s) {
		if (s == null) return null;
		Object object = Json.parse(s);
		return parseObject(object);
	}

	/** Serializes a data type into a json string. */
	public String toJson(Object o) {
		return toJson(o, true);
	}

	/** Serializes a data type into a json string. */
	public String toJson(Object o, boolean indent) {
		if (o == null) return "null";
		Object value = toObject(o);
		return Json.serialize(value, indent);
	}

	/** Parses a data type from an object. */
	public abstract Object parseObject(Object o);

	/** Serializes a data type into an object. */
	public abstract Object toObject(Object o);
}
