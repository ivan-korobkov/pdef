package pdef.descriptors;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

public class SetDescriptor extends AbstractDescriptor {
	private final ParameterizedType setType;
	private final Type element;

	public SetDescriptor(final ParameterizedType setType, final DescriptorPool pool) {
		super(DescriptorType.SET, pool);
		this.setType = checkNotNull(setType);
		checkArgument(setType.getRawType() == Set.class);

		element = setType.getActualTypeArguments()[0];
	}

	public ParameterizedType getSetType() {
		return setType;
	}

	public Type getElement() {
		return element;
	}

	@Override
	protected void doLink() {}
}
