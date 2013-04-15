package io.pdef.descriptors;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SetDescriptor extends AbstractDescriptor {
	private final ParameterizedType setType;
	private final Type elementType;
	private Descriptor element;

	public SetDescriptor(final ParameterizedType setType, final DescriptorPool pool) {
		super(setType, DescriptorType.SET, pool);
		this.setType = checkNotNull(setType);
		checkArgument(setType.getRawType() == Set.class);

		elementType = setType.getActualTypeArguments()[0];
	}

	public ParameterizedType getSetType() {
		return setType;
	}

	public Type getElementType() {
		return elementType;
	}

	public Descriptor getElement() {
		return element;
	}

	@Override
	protected void doLink() {
		element = pool.getDescriptor(elementType);
	}
}
