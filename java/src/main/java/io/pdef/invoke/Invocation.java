package io.pdef.invoke;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.pdef.meta.DataType;
import io.pdef.meta.InterfaceMethod;
import io.pdef.meta.MessageType;
import io.pdef.meta.MetaType;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

public class Invocation {
	private final InterfaceMethod method;
	private final Invocation parent;
	private final Object[] args;

	public static Invocation root() {
		return new Invocation(null, null, null);
	}

	private Invocation(final InterfaceMethod method, final Invocation parent,
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

	public InterfaceMethod getMethod() {
		return method;
	}

	public MetaType getResult() {
		return method == null ? null : method.getResult();
	}

	public DataType<?> getDataResult() {
		return method == null ? null : (DataType<?>) method.getResult();
	}

	/** Returns the method exception or the parent exception. */
	@Nullable
	public MessageType<?> getExc() {
		if (method != null) return method.getExc();
		if (parent != null) return parent.getExc();
		return null;
	}

	/** Returns true when the method result is not an interface. */
	public boolean isRemote() {
		return !isRoot() && method.isRemote();
	}

	/** Creates a child invocation. */
	public Invocation next(final InterfaceMethod method, final Object[] args) {
		return new Invocation(method, this, args);
	}

	/** Returns a list of invocation. */
	public List<Invocation> toChain() {
		List<Invocation> chain = parent != null ? parent.toChain() : Lists.<Invocation>newArrayList();
		if (!isRoot()) chain.add(this);

		return chain;
	}

	/** Invokes this invocation chain on an object. */
	public InvocationResult invoke(Object object) {
		checkNotNull(object);

		List<Invocation> chain = toChain();
		for (Invocation invocation : chain) {
			try {
				object = invocation.invokeSingle(object);
			} catch (Throwable t) {
				return handleException(t);
			}
		}

		return InvocationResult.ok(object);
	}

	/** Invokes only this invocation (not a chain) on an object. */
	public Object invokeSingle(final Object object) throws Throwable {
		try {
			return method.invoke(object, args);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private InvocationResult handleException(final Throwable t) {
		MessageType<?> excd = getExc();
		if (excd == null || !excd.getJavaClass().isInstance(t)) {
			// It is not an expected application exception.
			throw Throwables.propagate(t);
		}

		// It is an expected application exception.
		// All application exceptions are runtime.
		return InvocationResult.exc((RuntimeException) t);
	}
}
