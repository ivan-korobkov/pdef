package io.pdef;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PdefInvocation {
	private final Method method;
	private final Object[] args;
	private final PdefInvocation parent;

	public PdefInvocation(final Method method, final Object[] args) {
		this(method, args, null);
	}

	public PdefInvocation(final Method method, final Object[] args, final PdefInvocation parent) {
		this.method = method;
		this.args = args == null ? null : args.clone();
		this.parent = parent;

		if (method == null) {
			throw new NullPointerException("method == null");
		}
	}

	public Method getMethod() {
		return method;
	}

	public Object[] getArgs() {
		return args;
	}

	public PdefInvocation getParent() {
		return parent;
	}

	public PdefInvocation next(final Method method, final Object[] args) {
		return new PdefInvocation(method, args, this);
	}

	public List<PdefInvocation> toChain() {
		List<PdefInvocation> chain = new ArrayList<PdefInvocation>();

		PdefInvocation invocation = this;
		while (invocation != null) {
			chain.add(invocation);
			invocation = invocation.parent;
		}

		Collections.reverse(chain);
		return chain;
	}

	public Object invoke(final Object o) {
		try {
			return method.invoke(o, args);
		} catch (IllegalAccessException e) {
			throw new PdefException("Illegal access", e);
		} catch (InvocationTargetException e) {
			throw new PdefException("Invocation target exception", e.getCause());
		}
	}
}
