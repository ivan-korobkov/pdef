package io.pdef.invoke;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.pdef.descriptors.DataDescriptor;
import io.pdef.descriptors.Descriptor;
import io.pdef.descriptors.MessageDescriptor;
import io.pdef.descriptors.MethodDescriptor;

import javax.annotation.Nullable;
import java.util.Arrays;
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
			checkArgument(this.args.length == method.getArgs().size(), "Wrong number of args");
		}
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(method)
				.add("args", Arrays.toString(args))
				.toString();
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
			return Lists.newArrayList();
		}

		List<Invocation> chain = parent.toChain();
		chain.add(this);

		return chain;
	}

	/** Invokes this invocation chain on an object. */
	@SuppressWarnings("unchecked")
	public InvocationResult invoke(Object object) {
		checkNotNull(object);

		for (Invocation invocation : toChain()) {
			try {
				object = invocation.invokeSingle(object);
			} catch (Exception e) {
				return handleException(e);
			}
		}

		return InvocationResult.ok(object);
	}

	@VisibleForTesting
	Object invokeSingle(final Object object) throws Exception {
		@SuppressWarnings("unchecked")
		MethodDescriptor<Object, Object> unchecked = (MethodDescriptor<Object, Object>) method;
		return unchecked.invoke(object, args);
	}

	private InvocationResult handleException(final Exception exc) {
		MessageDescriptor<?> excd = getExc();
		if (excd == null) {
			// It is not an expected application exception.
			throw Throwables.propagate(exc);
		}

		Class<?> excClass = excd.getJavaClass();
		if (!excClass.isInstance(exc)) {
			throw Throwables.propagate(exc);
		}

		// It is an expected application exception.
		// All application exceptions are runtime.
		return InvocationResult.exc((RuntimeException) exc);
	}
}
