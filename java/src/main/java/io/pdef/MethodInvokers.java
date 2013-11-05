package io.pdef;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodInvokers {
	private MethodInvokers() {}

	/**
	 * Returns a reflection-based method invoker or throws {@link IllegalArgumentException}.
	 */
	public static <T, R> MethodInvoker<T, R> reflexive(final Class<T> interfaceClass,
			final String methodName) {
		return new ReflexMethodInvoker<T, R>(interfaceClass, methodName);
	}

	private static class ReflexMethodInvoker<T, R> implements MethodInvoker<T, R> {
		private final Method method;

		private ReflexMethodInvoker(final Class<T> cls, final String name) {
			if (cls == null) throw new NullPointerException("interfaceClass");
			if (name == null) throw new NullPointerException("methodName");

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
}
