package pdef.generated;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.descriptors.FieldDescriptor;
import pdef.descriptors.TypeDescriptor;
import pdef.descriptors.VariableDescriptor;

import java.util.Map;

public abstract class GeneratedFieldDescriptor implements FieldDescriptor {
	private final String name;
	private TypeDescriptor type;

	public GeneratedFieldDescriptor(final String name, final TypeDescriptor type) {
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
	public FieldDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		TypeDescriptor btype = type.bind(argMap);
		if (type == btype) {
			return this;
		}

		return new ParameterizedFieldDescriptor(this, btype);
	}
}
