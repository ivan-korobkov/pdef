package com.ivankorobkov.pdef.data;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.DescriptorPool;
import com.ivankorobkov.pdef.Pdef;

import javax.annotation.Nullable;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

public class ListDescriptor<E> extends AbstractDataTypeDescriptor {

	public static ListDescriptor<?> getInstance() {
		return BuiltinPackage.getInstance().getList();
	}

	private final TypeToken<E> elementType;
	private DataTypeDescriptor element;

	/**
	 * Creates a new declaration of the descriptor with generic type variables.
	 */
	ListDescriptor() {
		this(new TypeToken<List<E>>() {}, Pdef.classVariablesAsMap(ListDescriptor.class));
	}

	/**
	 * Creates a parameterized descriptor.
	 */
	private ListDescriptor(final TypeToken<?> type, final Map<TypeVariable<?>, TypeToken<?>> args) {
		super(type, args);
		this.elementType = Pdef.parameterizeTypeUnchecked(new TypeToken<E>(getClass()) {}, args);
	}

	@Override
	public String getName() {
		return "List";
	}

	public TypeToken<E> getElementType() {
		return elementType;
	}

	public DataTypeDescriptor getElement() {
		return element;
	}

	@Override
	protected ListDescriptor parameterize(final TypeToken<?> ptoken,
			final Map<TypeVariable<?>, TypeToken<?>> args) {
		return new ListDescriptor<E>(ptoken, args);
	}

	@Override
	public void link(final DescriptorPool pool) {
		element = pool.get(elementType);
	}

	@Override
	public Object merge(@Nullable final Object object, @Nullable final Object another) {
		if (another == null) {
			return object;
		}

		List<?> list = (List<?>) another;
		List<Object> merged = Lists.newArrayList();
		for (Object element : list) {
			Object copy = this.element.deepCopy(element);
			merged.add(copy);
		}

		return merged;
	}

	@Override
	public Object deepCopy(@Nullable final Object object) {
		if (object == null) {
			return null;
		}

		List<?> list = (List<?>) object;
		return Lists.newArrayList(list);
	}
}
