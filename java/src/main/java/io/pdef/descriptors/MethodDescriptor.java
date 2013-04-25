package io.pdef.descriptors;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import io.pdef.Name;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MethodDescriptor {
	private final Method method;
	private final String name;
	private final Type resultType;
	private final Descriptor result;
	private final Map<String, Descriptor> args;
	private final Map<String, Type> argTypes;

	public MethodDescriptor(final Method method, final DescriptorPool pool) {
		this.method = checkNotNull(method);
		name = method.getName().toLowerCase();
		resultType = method.getGenericReturnType();
		result = pool.getDescriptor(resultType);

		argTypes = parseArgTypes(method);
		args = buildArgs(argTypes, pool);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.toString();
	}

	public Method getMethod() {
		return method;
	}

	public String getName() {
		return name;
	}

	public Descriptor getResult() {
		return result;
	}

	public Type getResultType() {
		return resultType;
	}

	public Map<String, Descriptor> getArgs() {
		return args;
	}

	public Map<String, Type> getArgTypes() {
		return argTypes;
	}

	public Object invoke(final Object object, final Object[] args) {
		try {
			return method.invoke(object, args);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			throw Throwables.propagate(cause);
		}
	}

	private Map<String, Type> parseArgTypes(final Method method) {
		ImmutableMap.Builder<String, Type> args = ImmutableMap.builder();

		Type[] params = method.getGenericParameterTypes();
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < params.length; i++) {
			Type param = params[i];
			Annotation[] pannotations = annotations[i];

			String name = null;
			for (Annotation pannotation : pannotations) {
				if (pannotation.annotationType() == Name.class) {
					name = ((Name) pannotation).value();
					break;
				}
			}

			if (name == null) throw new IllegalArgumentException(
					"All params must be annotated with @Name(param) in " + method);
			args.put(name, param);
		}

		return args.build();
	}

	private Map<String, Descriptor> buildArgs(final Map<String, Type> argTypes,
			final DescriptorPool pool) {
		ImmutableMap.Builder<String, Descriptor> args = ImmutableMap.builder();
		for (Map.Entry<String, Type> entry : argTypes.entrySet()) {
			String name = entry.getKey();
			Type argType = entry.getValue();
			Descriptor arg = pool.getDescriptor(argType);
			args.put(name, arg);
		}
		return args.build();
	}
}
