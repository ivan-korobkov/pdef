package io.pdef;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/** Pdef method descriptor. */
public class PdefMethod {
	private final Method method;
	private final PdefInterface iface;
	private final String name;
	private final PdefDescriptor result;
	private final ImmutableMap<String, PdefDescriptor> args;

	PdefMethod(final Method method, final PdefInterface iface) {
		this.method = checkNotNull(method);
		this.iface = checkNotNull(iface);

		name = method.getName().toLowerCase();
		result = iface.pdef.get(method.getGenericReturnType());

		Type[] params = method.getGenericParameterTypes();
		Annotation[][] annotations = method.getParameterAnnotations();
		ImmutableMap.Builder<String, PdefDescriptor> argBuilder = ImmutableMap.builder();
		for (int i = 0; i < params.length; i++) {
			Type arg = params[i];
			Annotation[] pannotations = annotations[i];

			String name = null;
			for (Annotation pannotation : pannotations) {
				if (pannotation.annotationType() == Name.class) {
					name = ((Name) pannotation).value().toLowerCase();
					break;
				}
			}

			if (name == null) throw new IllegalArgumentException(
					"All params must be annotated with @Name(param) in " + method);
			PdefDescriptor argInfo = iface.pdef.get(arg);
			argBuilder.put(name, argInfo);
		}
		args = argBuilder.build();
	}

	public String getName() { return name; }
	public Method getMethod() { return method; }
	public PdefInterface getIface() { return iface; }
	public PdefDescriptor getResult() { return result; }
	public Map<String, PdefDescriptor> getArgs() { return args; }

	private Object invoke(final Object o, final Object[] args) {
		try {
			return method.invoke(o, args);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public Object invoke(final Object o, final Map<String, Object> argMap) {
		checkNotNull(o);
		checkNotNull(argMap);
		Object[] array = new Object[args.size()];

		// Immutable map maintains the creation order.
		int i = 0;
		for (Map.Entry<String, PdefDescriptor> entry : args.entrySet()) {
			Object arg = args.get(entry.getKey());
			array[i++] = arg;
		}

		return invoke(o, array);
	}

	public Object invoke(final Object o, final Iterable<Object> args) {
		Object[] argArray = Iterables.toArray(args, Object.class);
		return invoke(o, argArray);
	}
}
