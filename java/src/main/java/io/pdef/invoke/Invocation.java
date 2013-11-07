package io.pdef.invoke;

import io.pdef.*;

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

	@Nonnull
	private static Object[] copyArgs(@Nullable final Object[] args,
			final MethodDescriptor<?, ?> method) {
		int length = args == null ? 0 : args.length;
		int size = method.getArgs().size();
		if (length != size) {
			throw new IllegalArgumentException(
					"Wrong number of arguments, method " + method + ", " + size + " expected, got "
							+ length);
		}

		Object[] copy = new Object[length];
		for (int i = 0; i < length; i++) {
			copy[i] = DataTypes.copy(args[i]);
		}

		return copy;
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

	public Descriptor<?> getResult() {
		return method.getResult();
	}

	public DataTypeDescriptor<?> getDataResult() {
		return (DataTypeDescriptor<?>) method.getResult();
	}

	/** Returns the method exception or the parent exception. */
	@Nullable
	public MessageDescriptor<?> getExc() {
		MessageDescriptor<?> exc = method.getExc();
		if (exc != null) {
			return exc;
		}

		return parent == null ? null : parent.getExc();
	}

	/** Returns true when the method result is not an interface. */
	public boolean isRemote() {
		return method.isRemote();
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
	public InvocationResult invoke(Object object) throws Exception {
		if (object == null) throw new NullPointerException("object");

		try {
			for (Invocation invocation : toChain()) {
				object = invocation.invokeSingle(object);
			}
		} catch (Exception e) {
			MessageDescriptor<?> excd = getExc();
			Class<?> excClass = excd == null ? null : excd.getJavaClass();

			if (excd == null || !excClass.isInstance(e)) {
				throw e;
			}

			return InvocationResult.exc((RuntimeException) e);
		}

		return InvocationResult.ok(object);
	}

	// VisibleForTesting
	Object invokeSingle(final Object object) throws Exception {
		@SuppressWarnings("unchecked")
		MethodDescriptor<Object, Object> unchecked = (MethodDescriptor<Object, Object>) method;
		return unchecked.invoke(object, args);
	}
}
