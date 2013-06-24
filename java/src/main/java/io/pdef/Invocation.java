package io.pdef;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Invocation {
	private final Method descriptor;
	private final Invocation parent;
	private final Map<String, Object> args;

	public static Invocation root() {
		return new Invocation(null, null, ImmutableMap.<String, Object>of());
	}

	Invocation(final Method descriptor, final Invocation parent,
			final Map<String, Object> args) {
		this.descriptor = descriptor;
		this.parent = parent;
		this.args = ImmutableMap.copyOf(args);
	}

	public String getMethod() {
		return descriptor.getName();
	}

	public Method getDescriptor() {
		return descriptor;
	}

	public Invocation getParent() {
		return parent;
	}

	public Map<String, Object> getArgs() {
		return args;
	}

	public boolean isRoot() {
		return descriptor == null;
	}

	public List<Invocation> toList() {
		List<Invocation> result = Lists.newArrayList();
		Invocation iv = this;

		while (!iv.isRoot()) {
			result.add(iv);
			iv = iv.parent;
		}

		Collections.reverse(result);
		return result;
	}
}
