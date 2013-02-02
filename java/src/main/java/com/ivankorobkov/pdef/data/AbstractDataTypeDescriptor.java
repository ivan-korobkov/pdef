package com.ivankorobkov.pdef.data;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.Pdef;
import static com.ivankorobkov.pdef.Pdef.typeVariables;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

public abstract class AbstractDataTypeDescriptor implements DataTypeDescriptor {
	private final TypeToken<?> type;
	private final List<TypeVariable<?>> variables;

	public AbstractDataTypeDescriptor(final TypeToken<?> type,
			final Map<TypeVariable<?>, TypeToken<?>> argMap) {
		this.type = checkNotNull(type);
		variables = ImmutableList.copyOf(typeVariables(type));
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(type)
				.toString();
	}

	@Override
	public List<TypeVariable<?>> getVariables() {
		return variables;
	}

	@Override
	public DataTypeDescriptor parameterize(final TypeToken<?> ptoken) {
		checkArgument(type.getRawType().equals(ptoken.getRawType()));

		Type type = ptoken.getType();
		if (type instanceof Class) {
			return this;

		} else if (type instanceof ParameterizedType) {
			Type[] actualArgs = ((ParameterizedType) type).getActualTypeArguments();
			ImmutableList<Type> args = ImmutableList.copyOf(actualArgs);
			List<TypeVariable<?>> vars = getVariables();
			checkArgument(vars.size() == actualArgs.length,
					"Wrong number of arguments for %s, got %s", this, args);

			Map<TypeVariable<?>, TypeToken<?>> argMap = Pdef.variablesToArgsMap(vars, args);
			return parameterize(ptoken, argMap);

		} else {
			throw new IllegalArgumentException(
					"Parameterized type requried, got " + ptoken);
		}
	}

	protected abstract DataTypeDescriptor parameterize(TypeToken<?> ptoken,
			Map<TypeVariable<?>, TypeToken<?>> args);
}
