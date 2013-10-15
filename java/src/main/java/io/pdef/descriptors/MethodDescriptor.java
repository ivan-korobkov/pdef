package io.pdef.descriptors;

import io.pdef.Provider;
import io.pdef.Providers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MethodDescriptor holds a method name, result, arguments, exception, flags, and its invoker.
 * */
public class MethodDescriptor<T, R> {
	private final String name;
	private final Provider<Descriptor<R>> resultProvider;
	private final List<ArgumentDescriptor<?>> args;
	private final MessageDescriptor<?> exc;
	private final MethodInvoker<T, R> invoker;
	private final boolean index;
	private final boolean post;

	private Descriptor<R> result;

	public MethodDescriptor(final Builder<T, R> builder) {
		name = builder.name;
		resultProvider = builder.result;
		args = Collections.unmodifiableList(new ArrayList<ArgumentDescriptor<?>>(builder.args));
		exc = builder.exc;
		invoker = builder.invoker;
		index = builder.index;
		post = builder.post;

		if (name == null) throw new NullPointerException("name");
		if (resultProvider == null) throw new NullPointerException("result");
		if (invoker == null) throw new NullPointerException("invoker");
	}

	public static <T, R> Builder<T, R> builder() {
		return new Builder<T, R>();
	}

	@Override
	public String toString() {
		return "MethodDescriptor{" + name + '}';
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
		
		return (result = resultProvider.get());
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
		private Provider<Descriptor<R>> result;
		private List<ArgumentDescriptor<?>> args;
		private MessageDescriptor<?> exc;
		private MethodInvoker<T, R> invoker;
		private boolean index;
		private boolean post;

		public Builder() {
			args = new ArrayList<ArgumentDescriptor<?>>();
		}

		public Builder<T, R> setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder<T, R> setResult(final Descriptor<R> result) {
			if (result == null) throw new NullPointerException("result");
			return setResult(Providers.<Descriptor<R>>ofInstance(result));
		}

		public Builder<T, R> setResult(final Provider<Descriptor<R>> result) {
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
		public R invoke(final T object, final Object[] args) throws Exception {
			if (object == null) throw new NullPointerException("object");

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
