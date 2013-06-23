package io.pdef;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import io.pdef.rpc.MethodCall;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class MethodDescriptor {
	private final String name;
	private final Map<String, Descriptor<?>> argDescriptors;
	private final Invocable invocable;

	private final boolean remote;
	private final Descriptor<?> result;
	private final Descriptor<?> resultExc;
	private final InterfaceDescriptor resultInterface;

	private MethodDescriptor(final Builder builder) {
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

	public String getName() {
		return name;
	}

	public boolean isRemote() {
		return remote;
	}

	public Map<String, Descriptor<?>> getArgs() {
		return argDescriptors;
	}

	public Descriptor<?> getResult() {
		return result;
	}

	public Descriptor<?> getResultExc() {
		return resultExc;
	}

	public InterfaceDescriptor getResultInterface() {
		return resultInterface;
	}

	public Invocation capture(final Invocation parent, final Object... args) {
		checkArgument(args.length == argDescriptors.size(), "wrong number of arguments");
		return null;
	}

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
		for (Map.Entry<String, Descriptor<?>> entry : argDescriptors.entrySet()) {
			String key = entry.getKey();
			Descriptor<?> descriptor = entry.getValue();
			Object arg = args.get(key);
			Object value = arg != null ? arg : descriptor.getDefault();
			array[i++] = value;
		}

		return invocable.invoke(delegate, array);
	}

	public MethodCall write(final Invocation invocation) {
		checkNotNull(invocation);
		Map<String, Object> args = invocation.getArgs();
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

		for (Map.Entry<String, Descriptor<?>> entry : argDescriptors.entrySet()) {
			String key = entry.getKey();
			Object arg = args.get(key);
			ObjectOutput output = new ObjectOutput();

			@SuppressWarnings("unchecked")
			Descriptor<Object> descriptor = (Descriptor<Object>) entry.getValue();
			descriptor.write(arg, output);
			Object value = output.toObject();

			if (value == null) continue;
			builder.put(key, value);
		}

		return MethodCall.builder()
				.setMethod(name)
				.setArgs(builder.build())
				.build();
	}

	public Invocation read(final MethodCall call) {
		checkNotNull(call);
		checkArgument(name.equals(call.getMethod()));

		Map<String, Object> args = call.getArgs();
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

		for (Map.Entry<String, Descriptor<?>> entry : argDescriptors.entrySet()) {
			String key = entry.getKey();
			Object arg = args.get(key);
			ObjectInput input = new ObjectInput(arg);

			@SuppressWarnings("unchecked")
			Descriptor<Object> descriptor = (Descriptor<Object>) entry.getValue();
			Object value = descriptor.get(input);

			if (value == null) continue;
			builder.put(key, value);
		}

//		return Invocation.builder()
//				.setDescriptor(this)
//				.setArgs(builder.build())
//				.build();
		return null;
	}

	public static Builder builder(final String name) {
		return new Builder(name);
	}

	public static class Builder {
		private final String name;
		private boolean remote;
		private LinkedHashMap<String, Descriptor<?>> args;
		private Invocable invocable;
		private Descriptor<?> result;
		private Descriptor<?> resultExc;
		private InterfaceDescriptor resultInterface;

		private Builder(final String name) {
			this.name = checkNotNull(name);
		}

		public Builder remote() {
			remote = true;
			return this;
		}

		public Builder result(final Descriptor<?> result) {
			this.result = checkNotNull(result);
			return this;
		}

		public Builder resultExc(final Descriptor<?> resultExc) {
			this.resultExc = checkNotNull(resultExc);
			return this;
		}

		public Builder resultInterface(final InterfaceDescriptor resultInterface) {
			this.resultInterface = checkNotNull(resultInterface);
			return this;
		}

		public Builder arg(final String name, final Descriptor<?> descriptor) {
			checkArgument(!args.containsKey(name), "duplicate argument %s", name);
			args.put(name, descriptor);
			return this;
		}

		public Builder invocable(final Invocable invocable) {
			checkArgument(this.invocable == null, "duplicate invocable");
			this.invocable = checkNotNull(invocable);
			return this;
		}

		public Builder invocable(final Class<?> iface) {
			checkNotNull(iface);
			return invocable(ReflexInvocable.create(name, iface));
		}

		public MethodDescriptor build() {
			return new MethodDescriptor(this);
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
