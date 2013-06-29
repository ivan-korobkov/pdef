package io.pdef;

import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.pdef.rpc.MethodCall;
import io.pdef.rpc.RpcError;
import io.pdef.rpc.RpcErrors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public abstract class GeneratedInterfaceDescriptor<T> implements InterfaceDescriptor<T> {
	private final Class<T> javaClass;

	public GeneratedInterfaceDescriptor(final Class<T> javaClass) {
		this.javaClass = checkNotNull(javaClass);
	}

	@Override
	public Class<T> getJavaClass() {
		return javaClass;
	}

	public static GeneratedMethodDescriptorBuilder method(final InterfaceDescriptor<?> iface,
			final String name) {
		return new GeneratedMethodDescriptorBuilder(iface, name);
	}

	public static Map<String, MethodDescriptor> methods(final MethodDescriptor... methods) {
		ImmutableMap.Builder<String, MethodDescriptor> builder = ImmutableMap.builder();
		for (MethodDescriptor method : methods) builder.put(method.getName(), method);
		return builder.build();
	}

	static class GeneratedMethodDescriptor implements MethodDescriptor {
		private final InterfaceDescriptor<?> iface;
		private final String name;
		private final Map<String, Descriptor<?>> args;
		private final Descriptor<?> result;
		private final Descriptor<?> exc;
		private final Supplier<InterfaceDescriptor<?>> next;
		private final Method method;

		GeneratedMethodDescriptor(final GeneratedMethodDescriptorBuilder builder) {
			iface = checkNotNull(builder.iface);
			name = checkNotNull(builder.name);
			args = ImmutableMap.copyOf(builder.args);
			result = builder.result;
			exc = builder.exc;
			next = builder.next;
			method = getMethodByName(iface.getJavaClass(), name);
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
			checkArgument(this.args.size() == args.length, "Wrong number of arguments");

			int i = 0;
			ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
			for (String key : this.args.keySet()) {
				Object arg = args[i++];
				if (arg == null) continue;
				builder.put(key, arg);
			}

			return new Invocation(this, parent, builder.build());
		}

		@Override
		public Invocation parse(final Invocation parent, final Map<String, Object> args)
				throws RpcError {
			checkNotNull(parent);
			checkNotNull(args);

			ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
			for (Map.Entry<String, Descriptor<?>> entry : this.args.entrySet()) {
				String key = entry.getKey();
				Descriptor<?> descriptor = entry.getValue();
				Object arg = descriptor.parse(args.get(key));
				if (arg == null) continue;
				builder.put(key, arg);
			}

			return new Invocation(this, parent, builder.build());
		}

		@Override
		public Object invoke(final Object object, final Map<String, Object> args) {
			int i = 0;
			Object[] array = new Object[this.args.size()];
			for (Map.Entry<String, Descriptor<?>> entry : this.args.entrySet()) {
				String key = entry.getKey();
				Descriptor<?> descriptor = entry.getValue();
				Object arg = args.get(key);
				if (arg == null) arg = descriptor.getDefault();
				array[i++] = arg;
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
		public MethodCall serialize(final Map<String, Object> args) {
			ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
			for (Map.Entry<String, Descriptor<?>> entry : this.args.entrySet()) {
				String key = entry.getKey();
				Descriptor descriptor = entry.getValue();
				Object arg = descriptor.serialize(args.get(key));
			}
			return null;
		}
	}

	public static class GeneratedMethodDescriptorBuilder {
		private final InterfaceDescriptor<?> iface;
		private final String name;
		private final Map<String, Descriptor<?>> args;
		private Descriptor<?> result;
		private Descriptor<?> exc;
		private Supplier<InterfaceDescriptor<?>> next;

		public GeneratedMethodDescriptorBuilder(final InterfaceDescriptor<?> iface,
				final String name) {
			this.iface = checkNotNull(iface);
			this.name = checkNotNull(name);
			args = Maps.newLinkedHashMap();
		}

		public GeneratedMethodDescriptorBuilder arg(final String name,
				final Descriptor<?> descriptor) {
			args.put(checkNotNull(name), checkNotNull(descriptor));
			return this;
		}

		public GeneratedMethodDescriptorBuilder result(final Descriptor<?> result) {
			this.result = checkNotNull(result);
			return this;
		}

		public GeneratedMethodDescriptorBuilder exc(final Descriptor<?> exc) {
			this.exc = checkNotNull(exc);
			return this;
		}

		public GeneratedMethodDescriptorBuilder next(final Supplier<InterfaceDescriptor<?>> next) {
			this.next = checkNotNull(next);
			return this;
		}

		public MethodDescriptor build() {
			return new GeneratedMethodDescriptor(this);
		}
	}

	static Method getMethodByName(final Class<?> cls, final String name) {
		for (Method method : cls.getMethods()) if (method.getName().equals(name)) return method;
		throw new IllegalArgumentException("Method not found \"" + name + "\"");
	}
}
