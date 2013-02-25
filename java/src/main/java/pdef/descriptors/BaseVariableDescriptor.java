package pdef.descriptors;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;

import java.lang.reflect.TypeVariable;
import java.util.Map;

public class BaseVariableDescriptor extends AbstractTypeDescriptor implements VariableDescriptor {
	private final String name;

	public BaseVariableDescriptor(final String name) {
		super(TypeVariable.class);
		this.name = checkNotNull(name);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected void doLink() {}

	@Override
	public TypeDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		TypeDescriptor arg = argMap.get(this);
		checkState(arg != null, "Variable %s must be present in %s", this, argMap);
		return arg;
	}
}
