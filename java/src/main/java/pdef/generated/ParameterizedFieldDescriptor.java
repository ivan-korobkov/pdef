package pdef.generated;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import pdef.FieldDescriptor;
import pdef.Message;
import pdef.TypeDescriptor;
import pdef.VariableDescriptor;

import java.util.Map;

final class ParameterizedFieldDescriptor implements FieldDescriptor {
	private final FieldDescriptor raw;
	private final TypeDescriptor type;

	ParameterizedFieldDescriptor(final FieldDescriptor raw, final TypeDescriptor type) {
		this.raw = checkNotNull(raw);
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
	public boolean isTypeField() {
		return raw.isTypeField();
	}

	@Override
	public String getName() {
		return raw.getName();
	}

	@Override
	public Object get(final Message message) {
		return raw.get(message);
	}

	@Override
	public Object get(final Message.Builder builder) {
		return raw.get(builder);
	}

	@Override
	public boolean isSet(final Message message) {
		return raw.isSet(message);
	}

	@Override
	public boolean isSet(final Message.Builder builder) {
		return raw.isSet(builder);
	}

	@Override
	public void set(final Message.Builder builder, final Object value) {
		raw.set(builder, value);
	}

	@Override
	public void clear(final Message.Builder builder) {
		raw.clear(builder);
	}

	@Override
	public FieldDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		TypeDescriptor btype = type.bind(argMap);
		if (type == btype) return this;
		return new ParameterizedFieldDescriptor(raw, btype);
	}
}
