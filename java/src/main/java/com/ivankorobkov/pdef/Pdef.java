package com.ivankorobkov.pdef;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import static com.google.inject.util.Types.newParameterizedTypeWithOwner;
import com.ivankorobkov.pdef.data.BuiltinPackage;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

public class Pdef {

	private static final DescriptorPool POOL;
	static {
		POOL = new ConcurrentDescriptorPool();
		BuiltinPackage.getInstance();
	}

	private Pdef() {}

	public static DescriptorPool getPool() {
		return POOL;
	}

	public static ImmutableList<TypeVariable<?>> typeVariables(final TypeToken<?> token) {
		checkNotNull(token);
		return typeVariables(token.getType());
	}

	public static ImmutableList<TypeVariable<?>> typeVariables(final Type type) {
		if (type instanceof Class) {
			Class<?> cls = (Class<?>) type;
			TypeVariable<?>[] vars = cls.getTypeParameters();
			return ImmutableList.copyOf(vars);

		} else if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;

			List<TypeVariable<?>> vars = Lists.newArrayList();
			for (Type arg : ptype.getActualTypeArguments()) {
				if (arg instanceof TypeVariable) {
					vars.add((TypeVariable<?>) arg);
				}
			}

			return ImmutableList.copyOf(vars);
		}

		return ImmutableList.of();
	}

	public static ImmutableMap<TypeVariable<?>, TypeToken<?>> classVariablesAsMap(
			final Class<?> cls) {
		ImmutableMap.Builder<TypeVariable<?>, TypeToken<?>> builder = ImmutableMap.builder();
		for (TypeVariable<?> var : cls.getTypeParameters()) {
			builder.put(var, TypeToken.of(var));
		}

		return builder.build();
	}

	// This method is used internally during code generation which enforces its safety.
	@SuppressWarnings("unchecked")
	public static <T> TypeToken<T> parameterizeTypeUnchecked(final TypeToken<T> token,
			final Map<TypeVariable<?>, TypeToken<?>> argMap) {
		return (TypeToken<T>) parameterizeType(token, argMap);
	}

	public static TypeToken<?> parameterizeType(final TypeToken<?> token,
			final Map<TypeVariable<?>, TypeToken<?>> argMap) {
		ImmutableMap.Builder<TypeVariable<?>, Type> builder = ImmutableMap.builder();
		for (Map.Entry<TypeVariable<?>, TypeToken<?>> e : argMap.entrySet()) {
			builder.put(e.getKey(), e.getValue().getType());
		}

		Type type = token.getType();
		Map<TypeVariable<?>, Type> typeMap = builder.build();
		Type ptype = parameterizeType(type, typeMap);
		return TypeToken.of(ptype);
	}

	public static Type parameterizeType(final Type type,
			final Map<TypeVariable<?>, Type> arguments) {
		checkNotNull(type);
		checkNotNull(arguments);

		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			return parameterizeType(pt, arguments);

		} else if (type instanceof TypeVariable<?>) {
			TypeVariable<?> variable = (TypeVariable<?>) type;
			return parameterizeVariable(variable, arguments);

		} else {
			return type;
		}
	}

	/**
	 * Recursively replaces type variables in a parameterized type with the new types.
	 *
	 * @throws IllegalArgumentException if any type variable is not present in newArguments.
	 */
	public static ParameterizedType parameterizeType(final ParameterizedType type,
			final Map<TypeVariable<?>, Type> newArguments) {
		checkNotNull(type);
		checkNotNull(newArguments);

		Type[] args = type.getActualTypeArguments();
		if (args.length == 0) {
			return type;
		}

		Type[] paramArgs = new Type[args.length];
		for (int i = 0; i < args.length; i++) {
			Type arg = args[i];
			Type paramArg;

			if (arg instanceof TypeVariable) {
				TypeVariable var = (TypeVariable) arg;
				Type newType = newArguments.get(var);
				checkArgument(newType != null,
						"Type variable %s of %s is not present in %s",
						var, type, newArguments);
				paramArg = newType;

			} else if (arg instanceof ParameterizedType) {
				paramArg = parameterizeType((ParameterizedType) arg, newArguments);

			} else {
				paramArg = arg;
			}

			paramArgs[i] = paramArg;
		}

		return newParameterizedTypeWithOwner(type.getOwnerType(), type.getRawType(), paramArgs);
	}

	/**
	 * Gets a new argument from the map using the type variable as the key.
	 *
	 * @throws IllegalArgumentException if the argument is not found.
	 */
	public static Type parameterizeVariable(final TypeVariable<?> variable,
			final Map<TypeVariable<?>, Type> arguments) {
		checkNotNull(variable);
		checkNotNull(arguments);

		Type arg = arguments.get(variable);
		checkArgument(arg != null, "Type variable %s of %s is not present in %s",
				variable, variable.getGenericDeclaration(), arguments);

		return arg;
	}

	public static Map<TypeVariable<?>, TypeToken<?>> variablesToArgsMap(
			final Iterable<? extends TypeVariable<?>> vars,
			final Iterable<? extends Type> args) {
		ImmutableList<TypeVariable<?>> varList = ImmutableList.copyOf(vars);
		ImmutableList<Type> argList = ImmutableList.copyOf(args);
		checkArgument(varList.size() == argList.size(),
				"Wrong number of arguments for variables %s, got %s", varList, argList);

		ImmutableMap.Builder<TypeVariable<?>, TypeToken<?>> builder = ImmutableMap.builder();
		for (int i = 0; i < varList.size(); i++) {
			TypeVariable<?> var = varList.get(i);
			Type arg = argList.get(i);
			TypeToken<?> argToken = TypeToken.of(arg);
			builder.put(var, argToken);
		}

		return builder.build();
	}
}
