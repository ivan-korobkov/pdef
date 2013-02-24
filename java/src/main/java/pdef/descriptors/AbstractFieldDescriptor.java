package pdef.descriptors;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;

public abstract class AbstractFieldDescriptor implements FieldDescriptor {
	private final String name;
	private TypeDescriptor type;

	public AbstractFieldDescriptor(final String name, final TypeDescriptor type) {
		this.name = checkNotNull(name);
		this.type = checkNotNull(type);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
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
}
