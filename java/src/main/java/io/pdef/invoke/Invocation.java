package io.pdef.invoke;

import io.pdef.descriptors.DataDescriptor;
import io.pdef.descriptors.Descriptor;
import io.pdef.descriptors.MessageDescriptor;
import io.pdef.descriptors.MethodDescriptor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Invocation {
	private final MethodDescriptor<?, ?> method;
	private final Invocation parent;
	private final Object[] args;

	public static Invocation root() {
		return new Invocation(null, null, null);
	}

	private Invocation(final MethodDescriptor<?, ?> method, final Invocation parent,
			final Object[] args) {
		this.method = method;
		this.parent = parent;
		this.args = args == null ? new Object[0] : args.clone();

		if (method != null) {
			// It is not a root sentinel invocation.
			if (this.args.length != method.getArgs().size()) {
				throw new IllegalArgumentException(
						"Wrong number of arguments, " + method.getArgs().size() + " expected, got "
								+ this.args.length);
			}
		}
	}

	@Override
	public String toString() {
		return "Invocation{" + method.getName() + ", args=" + args + '}';
	}

	public boolean isRoot() {
		return method == null;
	}

	public Object[] getArgs() {
		return args;
	}

	public MethodDescriptor<?, ?> getMethod() {
		return method;
	}

	public Descriptor<?> getResult() {
		if (isRoot()) {
			return null;
		}
		return method.getResult();
	}

	public DataDescriptor<?> getDataResult() {
		if (isRoot()) {
			throw new UnsupportedOperationException();
		}

		return (DataDescriptor<?>) method.getResult();
	}

	/** Returns the method exception or the parent exception. */
	@Nullable
	public MessageDescriptor<?> getExc() {
		if (isRoot()) {
			return null;
		}

		MessageDescriptor<?> exc = method.getExc();
		if (exc == null) {
			exc = parent.getExc();
		}

		return exc;
	}

	/** Returns true when the method result is not an interface. */
	public boolean isRemote() {
		return !isRoot() && method.isRemote();
	}

	/** Creates a child invocation. */
	public Invocation next(final MethodDescriptor<?, ?> method, final Object[] args) {
		return new Invocation(method, this, args);
	}

	/** Returns a list of invocation. */
	public List<Invocation> toChain() {
		if (isRoot()) {
			return new ArrayList<Invocation>();
		}

		List<Invocation> chain = parent.toChain();
		chain.add(this);

		return chain;
	}

	/** Invokes this invocation chain on an object. */
	@SuppressWarnings("unchecked")
	public InvocationResult invoke(Object object) {
		if (object == null) throw new NullPointerException("object");

		for (Invocation invocation : toChain()) {
			try {
				object = invocation.invokeSingle(object);
			} catch (Exception e) {
				return handleException(e);
			}
		}

		return InvocationResult.ok(object);
	}

	// VisibleForTesting
	Object invokeSingle(final Object object) throws Exception {
		@SuppressWarnings("unchecked")
		MethodDescriptor<Object, Object> unchecked = (MethodDescriptor<Object, Object>) method;
		return unchecked.invoke(object, args);
	}

	private InvocationResult handleException(final Exception exc) {
		MessageDescriptor<?> excd = getExc();
		if (excd == null) {
			// It is not an expected application exception.
			throw propagate(exc);
		}

		Class<?> excClass = excd.getJavaClass();
		if (!excClass.isInstance(exc)) {
			throw propagate(exc);
		}

		// It is an expected application exception.
		// All application exceptions are runtime.
		return InvocationResult.exc((RuntimeException) exc);
	}

	private RuntimeException propagate(final Exception exc) {
		if (exc instanceof RuntimeException) {
			throw (RuntimeException) exc;
		} else {
			throw new RuntimeException(exc);
		}
	}
}
