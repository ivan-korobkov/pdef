package io.pdef;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.pdef.rpc.MethodCall;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Invocation {
	private final MethodDescriptor descriptor;
	private final Map<String, Object> args;
	private final Invocation parent;

	public static Invocation root() {
		return new Invocation(null, ImmutableMap.<String, Object>of(), null);
	}

	private Invocation(final MethodDescriptor descriptor, final Map<String, Object> args,
			final Invocation parent) {
		this.descriptor = descriptor;
		this.args = ImmutableMap.copyOf(args);
		this.parent = parent;
	}

	/** Creates a new invocation with a parent set to this one. */
	public Invocation next(final MethodDescriptor descriptor, final Map<String, Object> args) {
		checkNotNull(descriptor);
		checkNotNull(args);
		return new Invocation(descriptor, args, this);
	}

	/** Return a method descriptor, or null when a root invocation. */
	public MethodDescriptor getMethod() {
		return descriptor;
	}

	/** Returns a argument map, or an empty map. */
	public Map<String, Object> getArgs() {
		return args;
	}

	/** Returns a parent or null when a root invocation. */
	public Invocation getParent() {
		return parent;
	}

	/** Returns true when this invocation expected result is data or void. */
	public boolean isRemote() {
		return descriptor.isRemote();
	}

	/** Returns whether this invocation is a root one, i.e. has no method and no arguments. */
	public boolean isRoot() {
		return descriptor == null;
	}

	/** Returns the expected result descriptor. */
	public Descriptor<?> getResult() {
		return descriptor.getResult();
	}

	/** Returns the expected exception descriptor. */
	public Descriptor<?> getExc() {
		return descriptor.getExc();
	}

	/** Returns the next interface descriptor. */
	public InterfaceDescriptor<?> getNext() {
		return descriptor.getNext();
	}

	/** Returns a list of invocations from the root to this one except for the root. */
	public List<Invocation> toList() {
		List<Invocation> result = Lists.newArrayList();
		for (Invocation iv = this; !iv.isRoot(); iv = iv.parent) result.add(iv);
		Collections.reverse(result);
		return result;
	}

	/** Executes this invocation on an object. */
	public Object invoke(final Object object) {
		return descriptor.invoke(object, args);
	}

	/** Serializes this invocation to a method call, serializes all arguments to objects. */
	public MethodCall serialize() {
		return descriptor.serialize(args);
	}
}
