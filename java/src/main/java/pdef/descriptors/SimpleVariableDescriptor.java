package pdef.descriptors;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;

import java.lang.reflect.TypeVariable;

public class SimpleVariableDescriptor extends AbstractTypeDescriptor implements VariableDescriptor {
	private final String name;

	public SimpleVariableDescriptor(final String name) {
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
}
