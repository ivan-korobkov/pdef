package io.pdef;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.pdef.rpc.MethodCall;
import io.pdef.rpc.RpcError;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class GeneratedMethodDescriptor implements MethodDescriptor {
	private final InterfaceDescriptor<?> iface;
	private final String name;
	private final Map<String, Descriptor<?>> args;
	private final Descriptor<?> result;
	private final Descriptor<?> exc;
	private final Supplier<InterfaceDescriptor<?>> next;
	private final Method method;

	GeneratedMethodDescriptor(final Builder builder) {
		iface = checkNotNull(builder.iface);
		name = checkNotNull(builder.name);
		args = ImmutableMap.copyOf(builder.args);
		result = builder.result;
		exc = builder.exc;
		next = builder.next;
		method = GeneratedInterfaceDescriptor.getMethodByName(iface.getJavaClass(), name);
		checkArgument((result != null ? 1 : 0) + (next != null ? 1 : 0) == 1,
				"either result or next must be defined");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Map<String, Descriptor<?>> getArgs() {
		return args;
	}

	@Override
	public boolean isRemote() {
		return result != null;
	}

	@Override
	public Descriptor<?> getResult() {
		return result;
	}

	@Override
	public Descriptor<?> getExc() {
		return exc;
	}

	@Override
	public InterfaceDescriptor<?> getNext() {
		return next.get();
	}

	@Override
	public Invocation capture(final Invocation parent, final Object... args) {
		checkNotNull(parent);
		checkArgument(this.args.size() == args.length, "wrong number of arguments");
		return parent.next(this, args);
	}

	@Override
	public Object invoke(final Object object, final Object... args) {
		checkArgument(args.length == this.args.size(), "wrong number of arguments");
		Object[] array = new Object[this.args.size()];

		int i = 0;
		for (Descriptor<?> descriptor : this.args.values()) {
			Object arg = args[i];
			array[i++] = arg == null ? descriptor.getDefault() : arg;
		}

		try {
			return method.invoke(object, array);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw Throwables.propagate(e.getCause());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public MethodCall serialize(final Object... args) {
		checkArgument(args.length == this.args.size(), "wrong number of args");
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

		int i = 0;
		for (Map.Entry<String, Descriptor<?>> entry : this.args.entrySet()) {
			String key = entry.getKey();
			Descriptor descriptor = entry.getValue();
			Object arg = descriptor.serialize(args[i++]);
			if (arg == null) continue;
			builder.put(key, arg);
		}

		return MethodCall.builder()
				.setMethod(name)
				.setArgs(builder.build())
				.build();
	}

	@Override
	public Invocation parse(final Invocation parent, final Map<String, Object> args)
			throws RpcError {
		checkNotNull(parent);
		checkNotNull(args);
		Object[] array = new Object[this.args.size()];

		int i = 0;
		for (Map.Entry<String, Descriptor<?>> entry : this.args.entrySet()) {
			String key = entry.getKey();
			Descriptor<?> descriptor = entry.getValue();
			array[i++] = descriptor.parse(args.get(key));
		}

		return parent.next(this, array);
	}

	public static Builder builder(final InterfaceDescriptor<?> iface, final String name) {
		return new Builder(iface, name);
	}

	public static class Builder {
		private final InterfaceDescriptor<?> iface;
		private final String name;
		private final Map<String, Descriptor<?>> args;
		private Descriptor<?> result;
		private Descriptor<?> exc;
		private Supplier<InterfaceDescriptor<?>> next;

		private Builder(final InterfaceDescriptor<?> iface, final String name) {
			this.iface = checkNotNull(iface);
			this.name = checkNotNull(name);
			args = Maps.newLinkedHashMap();
		}

		public Builder arg(final String name,
				final Descriptor<?> descriptor) {
			args.put(checkNotNull(name), checkNotNull(descriptor));
			return this;
		}

		public Builder result(final Descriptor<?> result) {
			this.result = checkNotNull(result);
			return this;
		}

		public Builder exc(final Descriptor<?> exc) {
			this.exc = checkNotNull(exc);
			return this;
		}

		public Builder next(final Supplier<InterfaceDescriptor<?>> next) {
			this.next = checkNotNull(next);
			return this;
		}

		public MethodDescriptor build() {
			return new GeneratedMethodDescriptor(this);
		}
	}
}
