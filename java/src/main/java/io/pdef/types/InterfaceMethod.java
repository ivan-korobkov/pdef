package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class InterfaceMethod {
	private final String name;
	private final Supplier<Type<?>> result;
	private final ImmutableList<InterfaceMethodArg> args;
	private final Supplier<MessageType<?>> exc;
	private final Invoker invoker;
	private final boolean index;
	private final boolean post;

	public InterfaceMethod(final Builder builder) {
		this.name = checkNotNull(builder.name);
		this.result = checkNotNull(builder.result);
		this.args = ImmutableList.copyOf(builder.args);
		this.exc = builder.exc;
		this.invoker = checkNotNull(builder.invoker);
		this.index = builder.index;
		this.post = builder.post;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.toString();
	}

	public String name() {
		return name;
	}

	public boolean isIndex() {
		return index;
	}

	public boolean isPost() {
		return post;
	}

	public Type<?> result() {
		return result.get();
	}

	public List<InterfaceMethodArg> args() {
		return args;
	}

	public MessageType<?> exc() {
		return exc.get();
	}

	public boolean isRemote() {
		return result().type().isDataType();
	}

	/** Invokes this method. */
	public Object invoke(final Object object, final Object[] args) throws Exception {
		return invoker.invoke(object, args);
	}

	public static interface Invoker {
		Object invoke(Object object, Object[] args) throws Exception;
	}

	public static class Builder {
		private String name;
		private Supplier<Type<?>> result;
		private List<InterfaceMethodArg> args;
		private Supplier<MessageType<?>> exc;
		private Invoker invoker;
		private boolean index;
		private boolean post;

		public Builder() {
			args = Lists.newArrayList();
		}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setResult(final Supplier<Type<?>> result) {
			this.result = result;
			return this;
		}

		public Builder addArg(final String name, final Supplier<DataType<?>> type) {
			this.args.add(new InterfaceMethodArg(name, type));
			return this;
		}

		public Builder setExc(final Supplier<MessageType<?>> exc) {
			this.exc = exc;
			return this;
		}

		public Builder setInvoker(final Invoker invoker) {
			this.invoker = invoker;
			return this;
		}

		public Builder reflectionInvoker(final Class<?> interfaceClass) {
			this.invoker = new ReflectionInvoker(interfaceClass, name);
			return this;
		}

		public Builder setIndex(final boolean index) {
			this.index = index;
			return this;
		}

		public Builder setPost(final boolean post) {
			this.post = post;
			return this;
		}

		public InterfaceMethod build() {
			return new InterfaceMethod(this);
		}
	}

	private static class ReflectionInvoker implements Invoker {
		private final Method method;

		private ReflectionInvoker(final Class<?> cls, final String name) {
			Method m = null;
			for (Method method : cls.getMethods()) {
				if (method.getName().equals(name)) {
					m = method;
					break;
				}
			}

			if (m == null) {
				throw new IllegalArgumentException("Method is not found " + name);
			}

			method = m;
		}

		@Override
		public Object invoke(final Object object, final Object[] args) throws Exception{
			checkNotNull(object);

			try {
				return method.invoke(object, args);
			} catch (InvocationTargetException e) {
				Throwable t = e.getCause();
				if (t instanceof Error) {
					throw (Error) t;
				}
				throw (Exception) t;
			}
		}
	}
}
