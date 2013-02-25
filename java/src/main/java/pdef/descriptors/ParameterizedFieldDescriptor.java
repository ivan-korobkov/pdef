package pdef.descriptors;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.PdefMessage;

import java.util.Map;

class ParameterizedFieldDescriptor implements FieldDescriptor {
	private final FieldDescriptor rawField;
	private final TypeDescriptor type;

	ParameterizedFieldDescriptor(final FieldDescriptor rawField, final TypeDescriptor type) {
		this.rawField = checkNotNull(rawField);
		this.type = checkNotNull(type);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getName())
				.addValue(type)
				.toString();
	}

	@Override
	public TypeDescriptor getType() {
		return type;
	}

	@Override
	public String getName() {
		return rawField.getName();
	}

	@Override
	public Object get(final PdefMessage message) {
		return rawField.get(message);
	}

	@Override
	public void set(final PdefMessage message, final Object value) {
		rawField.set(message, value);
	}

	@Override
	public FieldDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		TypeDescriptor btype = type.bind(argMap);
		if (type == btype) {
			return this;
		}

		return new ParameterizedFieldDescriptor(this, btype);
	}
}
