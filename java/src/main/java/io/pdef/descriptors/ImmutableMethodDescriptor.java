package io.pdef.descriptors;

import io.pdef.*;

import java.util.ArrayList;
import java.util.List;

/**
 * MethodDescriptor holds a method name, result, arguments, exception, flags, and its invoker.
 * */
public class ImmutableMethodDescriptor<T, R> implements MethodDescriptor<T,R> {
	private final String name;
	private final MethodInvoker<T, R> invoker;

	private final Provider<Descriptor<R>> resultProvider;
	private final List<ArgumentDescriptor<?>> args;
	private final MessageDescriptor<?> exc;

	private final boolean index;
	private final boolean post;

	private Descriptor<R> result;

	private ImmutableMethodDescriptor(final Builder<T, R> builder) {
		if (builder.name == null) throw new NullPointerException("name");
		if (builder.result == null) throw new NullPointerException("result");
		if (builder.invoker == null) throw new NullPointerException("invoker");

		name = builder.name;
		invoker = builder.invoker;

		resultProvider = builder.result;
		args = ImmutableCollections.list(builder.args);
		exc = builder.exc;

		index = builder.index;
		post = builder.post;
	}

	public static <T, R> Builder<T, R> builder() {
		return new Builder<T, R>();
	}

	@Override
	public String toString() {
		return "MethodDescriptor{" + name + '}';
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Descriptor<R> getResult() {
		return result != null ? result : (result = resultProvider.get());
	}

	@Override
	public List<ArgumentDescriptor<?>> getArgs() {
		return args;
	}

	@Override
	public MessageDescriptor<?> getExc() {
		return exc;
	}

	@Override
	public boolean isIndex() {
		return index;
	}

	@Override
	public boolean isPost() {
		return post;
	}

	@Override
	public boolean isRemote() {
		TypeEnum type = getResult().getType();
		return type != TypeEnum.INTERFACE;
	}

	@Override
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

		public Builder<T, R> setInterfaceResult(final Class<R> interfaceClass) {
			return setResult(new Provider<Descriptor<R>>() {
				@Override
				public Descriptor<R> get() {
					return Descriptors.findInterfaceDescriptor(interfaceClass);
				}
			});
		}

		public Builder<T, R> setResult(final Provider<Descriptor<R>> result) {
			this.result = result;
			return this;
		}

		public Builder<T, R> setExc(final MessageDescriptor<?> exc) {
			this.exc = exc;
			return this;
		}

		public <V> Builder<T, R> addArg(final String name, final ValueDescriptor<V> type) {
			this.args.add(new ImmutableArgumentDescriptor<V>(name, type));
			return this;
		}

		public Builder<T, R> setInvoker(final MethodInvoker<T, R> invoker) {
			this.invoker = invoker;
			return this;
		}

		public Builder<T, R> setReflexiveInvoker(final Class<T> interfaceClass) {
			this.invoker = MethodInvokers.reflexive(interfaceClass, name);
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
			return new ImmutableMethodDescriptor<T, R>(this);
		}
	}
}
