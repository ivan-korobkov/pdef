package pdef.generated;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.FieldDescriptor;
import pdef.TypeDescriptor;
import pdef.VariableDescriptor;

import java.util.Map;

public abstract class GeneratedFieldDescriptor implements FieldDescriptor {
	private final String name;
	private final TypeDescriptor type;

	protected GeneratedFieldDescriptor(final String name, final TypeDescriptor type) {
		this.name = checkNotNull(name);
		this.type = checkNotNull(type);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper("FieldDescriptor")
				.addValue(name)
				.addValue(type)
				.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public TypeDescriptor getType() {
		return type;
	}

	@Override
	public boolean isTypeField() {
		return false;
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
