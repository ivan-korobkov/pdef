package io.pdef.descriptors;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static com.google.common.base.Preconditions.checkArgument;

public class FutureDescriptor extends AbstractDescriptor {
	private final Type elementType;
	private Descriptor element;

	protected FutureDescriptor(final ParameterizedType javaType, final DescriptorPool pool) {
		super(javaType, DescriptorType.FUTURE, pool);
		checkArgument(ListenableFuture.class == javaType.getRawType());
		elementType = javaType.getActualTypeArguments()[0];
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
