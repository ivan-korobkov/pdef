package io.pdef;

import static com.google.common.base.Preconditions.*;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import io.pdef.rpc.MethodCall;
import io.pdef.rpc.RpcException;
import io.pdef.rpc.RpcExceptions;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

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
		name = method.getName();
		result = pdef.get(method.getGenericReturnType());
		args = buildArgs(method, pdef);
	}

	public String getName() {
		return name;
	}

	public Method getJavaMethod() {
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

	public int getArgNum() {
		return args.size();
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
	public MethodCall createCall(final Object[] objects) throws RpcException {
		Object[] oo = objects == null ? new Object[0] : objects;
		if (oo.length != args.size()) {
			throw RpcExceptions.wrongNumberOfMethodArgs(name, args.size(), oo.length);
		}

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

	/** Invokes this method; replaces nulls with defaults. */
	public Object invoke(final Object o, final Object... arguments) throws RpcException {
		return invoke(o, Arrays.asList(arguments));
	}

	/** Invokes this method; replaces nulls with defaults. */
	public Object invoke(final Object o, final Iterable<Object> arguments) throws RpcException {
		checkNotNull(o);
		checkNotNull(arguments);
		Object[] array = new Object[args.size()];

		int i = 0;
		Iterator<PdefDatatype> required = args.values().iterator();
		Iterator<Object> actual = arguments.iterator();

		while (actual.hasNext() && required.hasNext()) {
			Object arg = actual.next();
			PdefDatatype descriptor = required.next();
			array[i++] = arg != null ? arg : descriptor.getDefaultValue();
		}

		if (actual.hasNext() || required.hasNext()) {
			// Get the total number of provided arguments.
			while (actual.hasNext()) {
				actual.next(); i++;
			}

			throw RpcExceptions.wrongNumberOfMethodArgs(name, args.size(), i);
		}

		return doInvoke(o, array);
	}

	/** Invokes this method; replaces nulls with defaults, skips unknown args. */
	public Object invoke(final Object o, final Map<String, Object> argMap) {
		checkNotNull(o);
		checkNotNull(argMap);
		Object[] array = new Object[args.size()];

		int i = 0;
		for (Map.Entry<String, PdefDatatype> entry : args.entrySet()) {
			String name = entry.getKey();
			Object value = argMap.get(name);
			array[i++] = value != null ? value : entry.getValue().getDefaultValue();
		}

		return doInvoke(o, array);
	}

	private Object doInvoke(final Object o, final Object[] args) {
		try {
			return method.invoke(o, args);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw Throwables.propagate(e.getCause());
		}
	}

	static ImmutableMap<String, PdefDatatype> buildArgs(final Method method, final Pdef pdef) {
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
				name = ((Name) ann).value();
				break;
			}
		}

		checkArgument(name != null, "All params must be annotated with @Name(param) in " + method);
		return name;
	}

	static Map<String, Integer> buildArgPositions(final ImmutableMap<String, PdefDatatype> args) {
		ImmutableMap.Builder<String, Integer> b = ImmutableMap.builder();
		int i = 0;
		for (String name : args.keySet()) {
			b.put(name, i++);
		}
		return b.build();
	}
}
