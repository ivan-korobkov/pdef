package io.pdef.meta;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * InterfaceMethod holds a method name, result, arguments, exception, flags, and its invoker.
 * */
public class InterfaceMethod {
	private final String name;
	private final Supplier<MetaType> result;
	private final ImmutableList<InterfaceMethodArg<?>> args;
	private final MessageType<?> exc;
	private final InterfaceMethodInvoker invoker;
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

	public String getName() {
		return name;
	}

	public boolean isIndex() {
		return index;
	}

	public boolean isPost() {
		return post;
	}

	public MetaType getResult() {
		return result.get();
	}

	public List<InterfaceMethodArg<?>> getArgs() {
		return args;
	}

	public MessageType<?> getExc() {
		return exc;
	}

	public boolean isRemote() {
		TypeEnum type = getResult().getType();
		return type.isDataType() || type == TypeEnum.VOID;
	}

	/** Invokes this method. */
	public Object invoke(final Object object, final Object[] args) throws Exception {
		return invoker.invoke(object, args);
	}

	public static class Builder {
		private String name;
		private Supplier<MetaType> result;
		private List<InterfaceMethodArg<?>> args;
		private MessageType<?> exc;
		private InterfaceMethodInvoker invoker;
		private boolean index;
		private boolean post;

		public Builder() {
			args = Lists.newArrayList();
		}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setResult(final MetaType result) {
			checkNotNull(result);
			return setResult(Suppliers.<MetaType>ofInstance(result));
		}

		public Builder setResult(final Supplier<MetaType> result) {
			this.result = result;
			return this;
		}

		public Builder setExc(final MessageType<?> exc) {
			this.exc = exc;
			return this;
		}

		public <V> Builder addArg(final String name, final DataType<V> type) {
			this.args.add(new InterfaceMethodArg<V>(name, type));
			return this;
		}

		public Builder setInvoker(final InterfaceMethodInvoker invoker) {
			this.invoker = invoker;
			return this;
		}

		public Builder setInvokerFrom(final Class<?> interfaceClass) {
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

	private static class ReflectionInvoker implements InterfaceMethodInvoker {
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
