package com.ivankorobkov.pdef.data;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.DescriptorPool;
import com.ivankorobkov.pdef.Pdef;

import javax.annotation.Nullable;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Set;

public class SetDescriptor<E> extends AbstractDataTypeDescriptor {

	public static SetDescriptor<?> getInstance() {
		return BuiltinPackage.getInstance().getSet();
	}

	private final TypeToken<E> elementType;
	private DataTypeDescriptor element;

	/**
	 * Creates a new declaration of the descriptor with generic type variables.
	 */
	SetDescriptor() {
		this(new TypeToken<Set<E>>() {}, Pdef.classVariablesAsMap(SetDescriptor.class));
	}

	/**
	 * Creates a parameterized descriptor.
	 */
	private SetDescriptor(final TypeToken<?> type, final Map<TypeVariable<?>, TypeToken<?>> args) {
		super(type, args);

		this.elementType = Pdef.parameterizeTypeUnchecked(new TypeToken<E>(getClass()) {}, args);
	}

	@Override
	public String getName() {
		return "Set";
	}

	public TypeToken<E> getElementType() {
		return elementType;
	}

	public DataTypeDescriptor getElement() {
		return element;
	}

	@Override
	protected SetDescriptor parameterize(final TypeToken<?> ptoken,
			final Map<TypeVariable<?>, TypeToken<?>> args) {
		return new SetDescriptor<E>(ptoken, args);
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

		Set<?> set = (Set<?>) another;
		Set<Object> merged = Sets.newLinkedHashSet();
		for (Object element : set) {
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

		Set<?> set = (Set<?>) object;
		return Sets.newLinkedHashSet(set);
	}
}
