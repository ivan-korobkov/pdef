package pdef.descriptors;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class ListDescriptor extends AbstractDescriptor {
	private final ParameterizedType listType;
	private final Type element;

	protected ListDescriptor(final ParameterizedType listType, final DescriptorPool pool) {
		super(DescriptorType.LIST, pool);
		this.listType = checkNotNull(listType);
		checkArgument(listType.getRawType() == List.class);

		element = listType.getActualTypeArguments()[0];
	}

	public ParameterizedType getListType() {
		return listType;
	}

	public Type getElement() {
		return element;
	}

	@Override
	protected void doLink() {}
}
