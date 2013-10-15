package io.pdef.descriptors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ReflexMethodInvoker<T, R> implements MethodInvoker<T, R> {
	private final Method method;

	ReflexMethodInvoker(final Class<T> cls, final String name) {
		Method m = null;
		for (Method method : cls.getMethods()) {
			if (method.getName().equals(name)) {
				m = method;
				break;
			}
		}

		if (m == null) {
			throw new IllegalArgumentException("Method is not found " + name);
		}

		method = m;
	}

	@Override
	public R invoke(final T object, final Object[] args) throws Exception {
		if (object == null) throw new NullPointerException("object");

		try {
			@SuppressWarnings("unchecked")
			R result = (R) method.invoke(object, args);
			return result;
		} catch (InvocationTargetException e) {
			Throwable t = e.getCause();
			if (t instanceof Error) {
				throw (Error) t;
			}
			throw (Exception) t;
		}
	}
}
