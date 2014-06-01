package io.pdef;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class PdefProxy<T> implements InvocationHandler {
	private static final Class[] constructorParams = new Class[]{InvocationHandler.class};
	private static final WeakHashMap<Class<?>, Class<?>> proxyClasses =
			new WeakHashMap<Class<?>, Class<?>>();

	private final Class<T> iface;
	private final PdefClient<?> client;
	private final List<PdefInvocation> parent;

	/** Creates a custom client. */
	static <T> T create(final Class<T> iface, final PdefClient<?> client) {
		PdefProxy<T> proxy = new PdefProxy<T>(iface, new ArrayList<PdefInvocation>(), client);
		return proxy.toProxy();
	}

	private PdefProxy(final Class<T> iface, final List<PdefInvocation> parent,
			final PdefClient<?> client) {
		if (iface == null) throw new NullPointerException("iface");
		if (parent == null) throw new NullPointerException("parent");
		if (client == null) throw new NullPointerException("client");

		this.iface = iface;
		this.client = client;
		this.parent = parent;
	}

	private T toProxy() {
		return getProxyInstance(iface, this);
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args)
			throws Throwable {
		boolean isPdefMethod = method.isAnnotationPresent(GET.class)
				|| method.isAnnotationPresent(POST.class);

		// It is equals, hashCode, etc. method.
		if (!isPdefMethod) {
			return method.invoke(this, args);
		}
		
		List<PdefInvocation> invocations = new ArrayList<PdefInvocation>(parent);
		invocations.add(new PdefInvocation(method, args));

		if (PdefHandler.hasDataTypeResult(method)) {
			return client.handle(invocations);

		} else {
			@SuppressWarnings("unchecked")
			Class<Object> nextIface = (Class<Object>) method.getReturnType();
			PdefProxy<?> next = new PdefProxy<Object>(nextIface, invocations, client);
			return next.toProxy();
		}
	}

	private static <T> T getProxyInstance(final Class<T> cls, final InvocationHandler handler) {
		Class<T> proxyClass = getProxyClass(cls);
		Constructor<T> constructor;
		try {
			constructor = proxyClass.getConstructor(constructorParams);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		try {
			return constructor.newInstance(handler);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> getProxyClass(final Class<T> cls) {
		Class<?> proxyClass;
		synchronized (proxyClasses) {
			proxyClass = proxyClasses.get(cls);

			if (proxyClass == null) {
				proxyClass = Proxy.getProxyClass(cls.getClassLoader(), cls);
				proxyClasses.put(cls, proxyClass);
			}
		}

		return (Class<T>) proxyClass;
	}
}
