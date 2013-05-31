package io.pdef;

import com.google.common.collect.ImmutableMap;
import io.pdef.rpc.MethodCall;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** Pdef method descriptor. */
public class PdefMethod {
	private final String name;
	private final Method method;
	private final PdefInterface iface;
	private final PdefDescriptor result;
	private final ImmutableMap<String, PdefDatatype> args;

	PdefMethod(final Method method, final PdefInterface iface) {
		this.method = checkNotNull(method);
		this.iface = checkNotNull(iface);

		Pdef pdef = iface.getPdef();
		name = method.getName().toLowerCase();
		result = pdef.get(method.getGenericReturnType());
		args = buildArgs(method, pdef);
	}

	public String getName() {
		return name;
	}

	public Method getMethod() {
		return method;
	}

	public PdefInterface getInterface() {
		return iface;
	}

	public PdefDescriptor getResult() {
		return result;
	}

	public Map<String, PdefDatatype> getArgs() {
		return args;
	}

	/** Returns whether this method result is void. */
	public boolean isVoid() {
		return result.isVoid();
	}

	/** Returns whether this method result is an interface. */
	public boolean isInterface() {
		return result.isInterface();
	}

	/** Returns whether this method result is a datatype. */
	public boolean isDatatype() {
		return result.isDatatype();
	}

	/** Creates a new method call, skips all null values in arguments. */
	public MethodCall createCall(final Object[] objects) {
		Object[] oo = objects == null ? new Object[0] : objects;
		checkArgument(oo.length == args.size(),
				"Wrong number of arguments, %s expected, %s provided: %s",
				args.size(), oo.length, this);

		Iterator<String> iterator = args.keySet().iterator();
		ImmutableMap.Builder<String, Object> callArgs = ImmutableMap.builder();
		for (Object o : oo) {
			if (o == null) continue;

			callArgs.put(iterator.next(), o);
		}

		return MethodCall.builder()
				.setMethod(name)
				.setArgs(callArgs.build())
				.build();
	}

	/** Invokes this method, replaces all null and not present args with the default values. */
	public Object invoke(final Object o, final Map<String, Object> argMap) {
		checkNotNull(o);
		checkNotNull(argMap);
		Object[] array = new Object[args.size()];

		int i = 0;
		for (Map.Entry<String, PdefDatatype> entry : args.entrySet()) {
			Object arg = args.get(entry.getKey());
			array[i++] = arg != null ? arg : entry.getValue().defaultValue();
		}

		return invoke(o, array);
	}

	/** Invokes this method, replaces all null args with the default values. */
	public Object invoke(final Object o, final Iterable<Object> arguments) {
		checkNotNull(o);
		checkNotNull(arguments);
		Object[] array = new Object[args.size()];

		int i = 0;
		Iterator<Object> iterator0 = arguments.iterator();
		Iterator<PdefDatatype> iterator1 = args.values().iterator();

		while (iterator0.hasNext() && iterator1.hasNext()) {
			Object arg = iterator0.next();
			PdefDatatype descriptor = iterator1.next();
			array[i++] = arg != null ? arg : descriptor.defaultValue();
		}

		checkArgument(!iterator0.hasNext() && !iterator1.hasNext(),
				"Wrong number of arguments, %s expected: %s", args.size(), this);
		return invoke(o, array);
	}

	private Object invoke(final Object o, final Object[] args) {
		try {
			return method.invoke(o, args);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	static ImmutableMap<String, PdefDatatype> buildArgs(final Method method,
			final Pdef pdef) {
		Type[] params = method.getGenericParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();

		ImmutableMap.Builder<String, PdefDatatype> b = ImmutableMap.builder();
		for (int i = 0; i < params.length; i++) {
			Type arg = params[i];
			Annotation[] panns = anns[i];
			String name = buildArgName(panns, method);

			PdefDatatype descriptor = (PdefDatatype) pdef.get(arg);
			b.put(name, descriptor);
		}

		return b.build();
	}

	static String buildArgName(final Annotation[] anns, final Method method) {
		String name = null;
		for (Annotation ann : anns) {
			if (ann.annotationType() == Name.class) {
				name = ((Name) ann).value().toLowerCase();
				break;
			}
		}

		checkArgument(name != null, "All params must be annotated with @Name(param) in " + method);
		return name;
	}
}
