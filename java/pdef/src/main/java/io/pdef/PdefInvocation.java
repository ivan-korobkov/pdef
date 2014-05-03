package io.pdef;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class PdefInvocation {
	private final Method method;
	private final Object[] args;

	public PdefInvocation(final Method method, final Object[] args) {
		if (method == null) throw new NullPointerException("method");
		
		this.method = method;
		this.args = args == null ? null : args.clone();
	}

	public Method getMethod() {
		return method;
	}

	public Object[] getArgs() {
		return args;
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
	
	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final PdefInvocation that = (PdefInvocation) o;

		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if (!Arrays.equals(args, that.args)) return false;
		if (!method.equals(that.method)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = method.hashCode();
		result = 31 * result + Arrays.hashCode(args);
		return result;
	}
}
