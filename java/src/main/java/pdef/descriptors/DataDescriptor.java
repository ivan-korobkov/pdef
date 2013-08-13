package pdef.descriptors;

import pdef.TypeEnum;
import pdef.json.Json;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class DataDescriptor implements Descriptor {
	private final TypeEnum type;

	protected DataDescriptor(final TypeEnum type) {
		this.type = checkNotNull(type);
	}

	@Override
	public TypeEnum getType() {
		return type;
	}

	/** Parses a data type from a json string. */
	public Object parseJson(String s) {
		if (s == null) return null;
		Object object = Json.parse(s);
		return parseObject(object);
	}

	/** Serializes a data type into a json string. */
	public String toJson(Object object) {
		if (object == null) return "null";
		Object value = toObject(object);
		return Json.serialize(value);
	}

	/** Parses a data type from an object. */
	public abstract Object parseObject(Object object);

	/** Serializes a data type into an object. */
	public abstract Object toObject(Object object);
}
