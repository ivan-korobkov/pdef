package io.pdef;

import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;

public abstract class AbstractStruct implements Struct, Serializable {
	@Override
	public String toJson() {
		return PdefJson.serialize(this);
	}

	@Override
	public String toJson(final boolean indent) {
		return PdefJson.serialize(this, indent);
	}

	@Override
	public void toJson(final Writer writer, final boolean indent) {
		PdefJson.serialize(this, writer, indent);
	}

	@Override
	public void toJson(final OutputStream stream, final boolean indent) {
		PdefJson.serialize(this, stream, indent);
	}
}
