package io.pdef;

import io.pdef.descriptors.MessageDescriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.descriptors.ValueDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Invocation {
	private final MethodDescriptor<?, ?> method;
	private final Invocation parent;
	private final Object[] args;

	public static Invocation root(final MethodDescriptor<?, ?> method, final Object[] args) {
		return new Invocation(method, args, null);
	}

	private Invocation(final MethodDescriptor<?, ?> method, final Object[] args,
			final Invocation parent) {
		if (method == null) throw new NullPointerException("method");
		this.method = method;
		this.parent = parent;
		this.args = copyArgs(args, method);
	}

	@Override
	public String toString() {
		return "Invocation{" + method.getName() + ", args=" + Arrays.toString(args) + '}';
	}

	public Object[] getArgs() {
		return args.clone();
	}

	public MethodDescriptor<?, ?> getMethod() {
		return method;
	}

	/** Returns the method result. */
	public ValueDescriptor<?> getResult() {
		if (!method.isRemote()) {
			throw new IllegalStateException("Cannot get a result when a method is not remote");
		}

		return (ValueDescriptor<?>) method.getResult();
	}

	/** Returns the method exception or the parent exception. */
	@Nullable
	public MessageDescriptor<? extends Message> getExc() {
		MessageDescriptor<? extends Message> exc = method.getExc();
		if (exc != null) {
			return exc;
		}

		return parent == null ? null : parent.getExc();
	}

	/** Creates a child invocation. */
	public Invocation next(final MethodDescriptor<?, ?> method, final Object[] args) {
		return new Invocation(method, args, this);
	}

	/** Returns a list of invocation. */
	public List<Invocation> toChain() {
		List<Invocation> chain = parent == null ? new ArrayList<Invocation>() : parent.toChain();
		chain.add(this);
		return chain;
	}
	
	/** Invokes this invocation chain on an object. */
	@SuppressWarnings("unchecked")
	public Object invoke(Object object) throws Exception {
		if (object == null) throw new NullPointerException("object");

		for (Invocation invocation : toChain()) {
			MethodDescriptor<Object, Object> unchecked =
					(MethodDescriptor<Object, Object>) invocation.method;
			object = unchecked.invoke(object, invocation.args);
		}

		return object;
	}

	@Nonnull
	private static Object[] copyArgs(@Nullable final Object[] args,
			final MethodDescriptor<?, ?> method) {
		int length = args == null ? 0 : args.length;
		int size = method.getArgs().size();

		if (length != size) {
			String msg = String.format("Wrong number of arguments for %s, %d expected, %d got",
					method, size, length);
			throw new IllegalArgumentException(msg);
		}

		Object[] copy = new Object[length];
		for (int i = 0; i < length; i++) {
			copy[i] = Values.copy(args[i]);
		}

		return copy;
	}
}
