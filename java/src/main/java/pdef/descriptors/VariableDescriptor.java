package pdef.descriptors;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.TypeVariable;

public class VariableDescriptor extends AbstractDescriptor {
	private final TypeVariable<?> variable;

	protected VariableDescriptor(final TypeVariable<?> variable, final DescriptorPool pool) {
		super(DescriptorType.VARIABLE, pool);
		this.variable = checkNotNull(variable);
	}

	public TypeVariable<?> getVariable() {
		return variable;
	}

	@Override
	protected void doLink() {}
}
