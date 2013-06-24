package io.pdef;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import io.pdef.rpc.MethodCall;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

class GeneratedMethod implements Method {
	private final String name;
	private final Map<String, Type<?>> argDescriptors;
	private final Invocable invocable;

	private final boolean remote;
	private final Type<?> result;
	private final Type<?> resultExc;
	private final InterfaceType resultInterface;

	private GeneratedMethod(final Builder builder) {
		name = checkNotNull(builder.name);
		remote = builder.remote;
		argDescriptors = ImmutableMap.copyOf(builder.args);
		invocable = checkNotNull(builder.invocable, "no invocable in a \"%s\" builder", name);

		if (remote){
			result = checkNotNull(builder.result, "no result in a remote method \"%s\"", name);
			resultExc = builder.resultExc;
			resultInterface = null;
			checkArgument(builder.resultInterface == null,
					"interface result is not possible in a remote method \"%s\"", name);
		} else {
			result = null;
			resultExc = builder.resultExc;
			resultInterface = checkNotNull(builder.resultInterface,
					"no result interface in an interface method \"%s\"", name);
			checkArgument(builder.result == null,
					"non-interface result is not possible in an interface method \"%s\"", name);
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isRemote() {
		return remote;
	}

	@Override
	public Map<String, Type<?>> getArgs() {
		return argDescriptors;
	}

	@Override
	public Type<?> getResult() {
		return result;
	}

	@Override
	public Type<?> getResultExc() {
		return resultExc;
	}

	@Override
	public InterfaceType getResultInterface() {
		return resultInterface;
	}

	@Override
	public Invocation capture(final Invocation parent, final Object... args) {
		checkArgument(args.length == argDescriptors.size(), "wrong number of arguments");
		return null;
	}

	@Override
	public Object invoke(final Object delegate, final Invocation invocation) {
		Object r = doInvoke(delegate, invocation);
		if (remote) return r;
		return resultInterface.server(delegate).apply(invocation);
	}

	private Object doInvoke(final Object delegate, final Invocation invocation) {
		checkNotNull(delegate);
		checkNotNull(invocation);
		checkArgument(name.equals(invocation.getMethod()));

		int i = 0;
		Object[] array = new Object[argDescriptors.size()];
		Map<String, Object> args = invocation.getArgs();
		for (Map.Entry<String, Type<?>> entry : argDescriptors.entrySet()) {
			String key = entry.getKey();
			Type<?> type = entry.getValue();
			Object arg = args.get(key);
			Object value = arg != null ? arg : type.getDefault();
			array[i++] = value;
		}

		return invocable.invoke(delegate, array);
	}

	@Override
	public MethodCall write(final Invocation invocation) {
		checkNotNull(invocation);
		Map<String, Object> args = invocation.getArgs();
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

		for (Map.Entry<String, Type<?>> entry : argDescriptors.entrySet()) {
			String key = entry.getKey();
			Object arg = args.get(key);
			ObjectOutput output = new ObjectOutput();

			@SuppressWarnings("unchecked")
			Type<Object> type = (Type<Object>) entry.getValue();
			type.write(arg, output);
			Object value = output.toObject();

			if (value == null) continue;
			builder.put(key, value);
		}

		return MethodCall.builder()
				.setMethod(name)
				.setArgs(builder.build())
				.build();
	}

	@Override
	public Invocation read(final MethodCall call) {
		checkNotNull(call);
		checkArgument(name.equals(call.getMethod()));

		Map<String, Object> args = call.getArgs();
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

		for (Map.Entry<String, Type<?>> entry : argDescriptors.entrySet()) {
			String key = entry.getKey();
			Object arg = args.get(key);
			ObjectInput input = new ObjectInput(arg);

			@SuppressWarnings("unchecked")
			Type<Object> type = (Type<Object>) entry.getValue();
			Object value = type.read(input);

			if (value == null) continue;
			builder.put(key, value);
		}

//		return Invocation.builder()
//				.setDescriptor(this)
//				.setArgs(builder.build())
//				.build();
		return null;
	}

	static Builder builder(final String name) {
		return new Builder(name);
	}

	public static class Builder {
		private final String name;
		private boolean remote;
		private LinkedHashMap<String, Type<?>> args;
		private Invocable invocable;
		private Type<?> result;
		private Type<?> resultExc;
		private InterfaceType<?> resultInterface;

		private Builder(final String name) {
			this.name = checkNotNull(name);
		}

		public Builder remote() {
			remote = true;
			return this;
		}

		public Builder result(final Type<?> result) {
			this.result = checkNotNull(result);
			return this;
		}

		public Builder result(final InterfaceType resultInterface) {
			this.resultInterface = checkNotNull(resultInterface);
			return this;
		}

		public Builder resultExc(final Type<?> resultExc) {
			this.resultExc = checkNotNull(resultExc);
			return this;
		}

		public Builder arg(final String name, final Type<?> type) {
			checkArgument(!args.containsKey(name), "duplicate argument %s", name);
			args.put(name, type);
			return this;
		}

		private Builder invocable(final Invocable invocable) {
			checkArgument(this.invocable == null, "duplicate invocable");
			this.invocable = checkNotNull(invocable);
			return this;
		}

		public Builder invocable(final Class<?> iface) {
			checkNotNull(iface);
			return invocable(ReflexInvocable.create(name, iface));
		}

		public Method build() {
			return new GeneratedMethod(this);
		}
	}

	static class ReflexInvocable implements Invocable {
		private final java.lang.reflect.Method method;

		public static ReflexInvocable create(final String name, final Class<?> iface) {
			try {
				java.lang.reflect.Method m = iface.getMethod(name);
				return new ReflexInvocable(m);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		ReflexInvocable(final java.lang.reflect.Method method) {
			this.method = checkNotNull(method);
		}

		@Override
		public Object invoke(final Object delegate, final Object[] input) {
			try {
				return method.invoke(delegate, input);
			} catch (IllegalAccessException e) {
				throw Throwables.propagate(e);
			} catch (InvocationTargetException e) {
				throw Throwables.propagate(e.getCause());
			}
		}
	}
}
