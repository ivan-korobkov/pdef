package io.pdef.descriptors;

import io.pdef.Message;
import io.pdef.Provider;
import io.pdef.Providers;
import io.pdef.TypeEnum;

import java.util.ArrayList;
import java.util.List;

/** MethodDescriptor holds a method name, result, arguments, exception, flags, and its invoker. */
public class MethodDescriptor<T, R> implements MethodInvoker<T, R> {
	private final String name;
	private final MethodInvoker<T, R> invoker;

	private final Provider<Descriptor<R>> resultProvider;
	private final List<ArgumentDescriptor<?>> args;
	private final MessageDescriptor<? extends Message> exc;
	private final boolean post;

	private Descriptor<R> result;

	private MethodDescriptor(final Builder<T, R> builder) {
		if (builder.name == null) throw new NullPointerException("name");
		if (builder.result == null) throw new NullPointerException("result");
		if (builder.invoker == null) throw new NullPointerException("invoker");

		name = builder.name;
		invoker = builder.invoker;

		resultProvider = builder.result;
		args = ImmutableCollections.list(builder.args);
		exc = builder.exc;
		post = builder.post;
	}

	public static <T, R> Builder<T, R> builder() {
		return new Builder<T, R>();
	}

	@Override
	public String toString() {
		return "MethodDescriptor{" + name + '}';
	}

	/** Returns a pdef method name. */
	public String getName() {
		return name;
	}

	/**
	 * Returns this method result descriptor.
	 * <p/>
	 * It can be a {@link io.pdef.descriptors.ValueDescriptor} if this method is terminal or {@link
	 * io.pdef.descriptors.InterfaceDescriptor} otherwise.
	 */
	public Descriptor<R> getResult() {
		return result != null ? result : (result = resultProvider.get());
	}

	/** Returns a list of argument descriptors or an empty list. */
	public List<ArgumentDescriptor<?>> getArgs() {
		return args;
	}

	/**
	 * Returns a method exception descriptor or {@literal null}.
	 * <p/>
	 * In default pdef implementation all interface methods share the same exception.
	 */
	public MessageDescriptor<? extends Message> getExc() {
		return exc;
	}

	/** Returns whether this method is a post method (annotated with @post annotation). */
	public boolean isPost() {
		return post;
	}

	/** Returns whether this method returns a value type or void (not an interface). */
	public boolean isTerminal() {
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
		private MessageDescriptor<? extends Message> exc;
		private MethodInvoker<T, R> invoker;
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

		public Builder<T, R> setExc(final MessageDescriptor<? extends Message> exc) {
			this.exc = exc;
			return this;
		}

		public <V> Builder<T, R> addArg(final String name, final ValueDescriptor<V> type,
				final boolean isQuery, final boolean isPost) {
			this.args.add(new ArgumentDescriptor<V>(name, type, isQuery, isPost));
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

		public Builder<T, R> setPost(final boolean post) {
			this.post = post;
			return this;
		}

		public MethodDescriptor<T, R> build() {
			return new MethodDescriptor<T, R>(this);
		}
	}
}
