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
public class MethodDescriptor<T, R> {
	private final String name;
	private final Supplier<Descriptor<R>> resultSupplier;
	private final ImmutableList<ArgumentDescriptor<?>> args;
	private final MessageDescriptor<?> exc;
	private final MethodInvoker<T, R> invoker;
	private final boolean index;
	private final boolean post;

	private Descriptor<R> result;

	public MethodDescriptor(final Builder<T, R> builder) {
		this.name = checkNotNull(builder.name);
		this.resultSupplier = checkNotNull(builder.result);
		this.args = ImmutableList.copyOf(builder.args);
		this.exc = builder.exc;
		this.invoker = checkNotNull(builder.invoker);
		this.index = builder.index;
		this.post = builder.post;
	}

	public static <T, R> Builder<T, R> builder() {
		return new Builder<T, R>();
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

	public Descriptor<R> getResult() {
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
	public R invoke(final T object, final Object[] args) throws Exception {
		return invoker.invoke(object, args);
	}

	public static class Builder<T, R> {
		private String name;
		private Supplier<Descriptor<R>> result;
		private List<ArgumentDescriptor<?>> args;
		private MessageDescriptor<?> exc;
		private MethodInvoker<T, R> invoker;
		private boolean index;
		private boolean post;

		public Builder() {
			args = Lists.newArrayList();
		}

		public Builder<T, R> setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder<T, R> setResult(final Descriptor<R> result) {
			checkNotNull(result);
			return setResult(Suppliers.<Descriptor<R>>ofInstance(result));
		}

		public Builder<T, R> setResult(final Supplier<Descriptor<R>> result) {
			this.result = result;
			return this;
		}

		public Builder<T, R> setExc(final MessageDescriptor<?> exc) {
			this.exc = exc;
			return this;
		}

		public <V> Builder<T, R> addArg(final String name, final DataDescriptor<V> type) {
			this.args.add(new ArgumentDescriptor<V>(name, type));
			return this;
		}

		public Builder<T, R> setInvoker(final MethodInvoker<T, R> invoker) {
			this.invoker = invoker;
			return this;
		}

		public Builder<T, R> setInvokerFrom(final Class<T> interfaceClass) {
			this.invoker = new ReflectionInvoker<T, R>(interfaceClass, name);
			return this;
		}

		public Builder<T, R> setIndex(final boolean index) {
			this.index = index;
			return this;
		}

		public Builder<T, R> setPost(final boolean post) {
			this.post = post;
			return this;
		}

		public MethodDescriptor<T, R> build() {
			return new MethodDescriptor<T, R>(this);
		}
	}

	private static class ReflectionInvoker<T, R> implements MethodInvoker<T, R> {
		private final Method method;

		private ReflectionInvoker(final Class<T> cls, final String name) {
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
		public R invoke(final T object, final Object[] args) throws Exception{
			checkNotNull(object);

			try {
				@SuppressWarnings("unchecked")
				R result = (R) method.invoke(object, args);
				return result;
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
