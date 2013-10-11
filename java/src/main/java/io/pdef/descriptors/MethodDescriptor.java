package io.pdef.descriptors;

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
 * MethodDescriptor holds a method name, result, arguments, exception, flags, and its invoker.
 * */
public class MethodDescriptor {
	private final String name;
	private final Supplier<Descriptor> resultSupplier;
	private final ImmutableList<ArgumentDescriptor<?>> args;
	private final MessageDescriptor<?> exc;
	private final MethodInvoker invoker;
	private final boolean index;
	private final boolean post;

	private Descriptor result;

	public MethodDescriptor(final Builder builder) {
		this.name = checkNotNull(builder.name);
		this.resultSupplier = checkNotNull(builder.result);
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

	public Descriptor getResult() {
		if (result != null) {
			return result;
		}
		
		return (result = resultSupplier.get());
	}

	public List<ArgumentDescriptor<?>> getArgs() {
		return args;
	}

	public MessageDescriptor<?> getExc() {
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
		private Supplier<Descriptor> result;
		private List<ArgumentDescriptor<?>> args;
		private MessageDescriptor<?> exc;
		private MethodInvoker invoker;
		private boolean index;
		private boolean post;

		public Builder() {
			args = Lists.newArrayList();
		}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setResult(final Descriptor result) {
			checkNotNull(result);
			return setResult(Suppliers.<Descriptor>ofInstance(result));
		}

		public Builder setResult(final Supplier<Descriptor> result) {
			this.result = result;
			return this;
		}

		public Builder setExc(final MessageDescriptor<?> exc) {
			this.exc = exc;
			return this;
		}

		public <V> Builder addArg(final String name, final DataDescriptor<V> type) {
			this.args.add(new ArgumentDescriptor<V>(name, type));
			return this;
		}

		public Builder setInvoker(final MethodInvoker invoker) {
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

		public MethodDescriptor build() {
			return new MethodDescriptor(this);
		}
	}

	private static class ReflectionInvoker implements MethodInvoker {
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
