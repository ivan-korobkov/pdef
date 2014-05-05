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
	public void toJson(final Writer writer) {
		PdefJson.serialize(this, writer);
	}

	@Override
	public void toJson(final OutputStream stream) {
		PdefJson.serialize(this, stream);
	}
}
