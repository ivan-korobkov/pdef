package io.pdef;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.pdef.descriptors.Descriptor;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.rpc.RpcCall;

import java.util.Collections;
import java.util.List;

/** Immutable chained invocation of an interface method. */
public class Invocation {
	private final Object[] args;
	private final Invocation parent;
	private final Descriptor<?> exc;
	private final MethodDescriptor method;

	public static Invocation root() {
		return new Invocation(null, null);
	}

	private Invocation(final MethodDescriptor method, final Invocation parent,
			final Object... args) {
		this.method = method;
		this.parent = parent;
		this.exc =
				method == null ? null : method.getExc() != null ? method.getExc() : parent.getExc();
		this.args = args.clone();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		buildPath(sb);

		return Objects.toStringHelper(this)
				.addValue(sb.toString())
				.addValue(args)
				.toString();
	}

	private void buildPath(final StringBuilder sb) {
		if (isRoot()) return;
		if (parent != null) parent.buildPath(sb);
		if (sb.length() != 0) sb.append(".");
		sb.append(getMethod().getName());
	}

	/** Creates a new proxy with a parent set to this one. */
	public Invocation next(final MethodDescriptor descriptor, final Object... args) {
		checkNotNull(descriptor);
		checkNotNull(args);
		return new Invocation(descriptor, this, args);
	}

	/** Return a method descriptor, or null when a root proxy. */
	public MethodDescriptor getMethod() {
		return method;
	}

	/** Returns a parent or null when a root proxy. */
	public Invocation getParent() {
		return parent;
	}

	public Descriptor<?> getExc() {
		return exc;
	}

	/** Returns an array of arguments. */
	public Object[] getArgs() {
		return args.clone();
	}

	/** Returns true when this proxy expected result is data or void. */
	public boolean isRemote() {
		return method.isRemote();
	}

	/** Returns whether this proxy is a root one, i.e. has no method and no arguments. */
	public boolean isRoot() {
		return method == null;
	}

	/** Returns the next interface descriptor. */
	public InterfaceDescriptor<?> getNext() {
		return method.getNext();
	}

	/** Returns a list of invocations from the root to this one except for the root. */
	public List<Invocation> toList() {
		List<Invocation> result = Lists.newArrayList();
		for (Invocation iv = this; !iv.isRoot(); iv = iv.parent) result.add(iv);
		Collections.reverse(result);
		return result;
	}

	/** Serializes this proxy to a method call, serializes all arguments to objects. */
	public RpcCall serialize() {
		return method.serialize(args);
	}

	/** Invokes this invocation on an object, also see #invokeChainOn. */
	public Object invoke(final Object object) {
		return method.invoke(object, args);
	}

	/** Converts this invocation to a list of chained ones and invokes them on a service. */
	public InvocationResult invokeChainOn(final Object service) {
		checkNotNull(service);

		try {
			Object object = service;
			List<Invocation> invocations = toList();
			for (Invocation invocation : invocations) object = invocation.invoke(object);
			return InvocationResult.success(object, method);
		} catch (Exception e) {
			if (exc == null) throw Throwables.propagate(e);

			Class<?> excClass = exc.getJavaClass();
			if (!excClass.isInstance(e)) throw Throwables.propagate(e);

			return InvocationResult.exc(e, method);
		}
	}
}
