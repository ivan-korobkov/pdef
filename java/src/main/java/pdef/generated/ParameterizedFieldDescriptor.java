package pdef.generated;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.Message;
import pdef.FieldDescriptor;
import pdef.TypeDescriptor;
import pdef.VariableDescriptor;

import java.util.Map;

final class ParameterizedFieldDescriptor implements FieldDescriptor {
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
	public Object get(final Message message) {
		return rawField.get(message);
	}

	@Override
	public Object get(final Message.Builder builder) {
		return rawField.get(builder);
	}

	@Override
	public boolean isSet(final Message message) {
		return rawField.isSet(message);
	}

	@Override
	public boolean isSet(final Message.Builder builder) {
		return rawField.isSet(builder);
	}

	@Override
	public void set(final Message.Builder builder, final Object value) {
		rawField.set(builder, value);
	}

	@Override
	public void clear(final Message.Builder builder) {
		rawField.clear(builder);
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
