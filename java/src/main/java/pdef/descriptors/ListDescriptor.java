package pdef.descriptors;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class ListDescriptor extends AbstractDescriptor {
	private final Type elementType;
	private Descriptor element;

	protected ListDescriptor(final ParameterizedType listType, final DescriptorPool pool) {
		super(listType, DescriptorType.LIST, pool);
		checkArgument(listType.getRawType() == List.class);
		elementType = listType.getActualTypeArguments()[0];
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
