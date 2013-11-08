package io.pdef;

import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MethodDescriptor;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class InvocationProxy<T> implements InvocationHandler {
	private final InterfaceDescriptor<T> descriptor;
	private final Invoker handler;
	@Nullable
	private final Invocation parent;

	/** Creates a custom client. */
	public static <T> T create(final InterfaceDescriptor<T> descriptor, final Invoker invoker) {
		if (descriptor == null) throw new NullPointerException("descriptor");
		if (invoker == null) throw new NullPointerException("invocationHandler");

		InvocationProxy<T> invocationProxy = new InvocationProxy<T>(descriptor, invoker, null);
		return invocationProxy.toProxy();
	}

	private InvocationProxy(final InterfaceDescriptor<T> descriptor, final Invoker handler,
			final Invocation parent) {
		this.descriptor = descriptor;
		this.handler = handler;
		this.parent = parent;
	}

	private T toProxy() {
		Class<T> cls = descriptor.getJavaClass();
		Object proxy = Proxy.newProxyInstance(cls.getClassLoader(), new Class<?>[]{cls}, this);
		return cls.cast(proxy);
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args)
			throws Throwable {
		String name = method.getName();
		MethodDescriptor md = descriptor.getMethod(name);
		if (md == null) {
			// It must be the equals, hashCode, etc. method.
			return method.invoke(this, args);
		}

		Invocation invocation = capture(md, args);
		if (md.isRemote()) {
			return handle(invocation);
		} else {
			return nextProxy(invocation);
		}
	}

	private Invocation capture(final MethodDescriptor md, final Object[] args) {
		if (parent == null) {
			return Invocation.root(md, args);
		} else {
			return parent.next(md, args);
		}
	}

	private Object handle(final Invocation invocation) {
		try {
			return handler.invoke(invocation);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private Object nextProxy(final Invocation invocation) {
		MethodDescriptor<?, ?> method = invocation.getMethod();
		InterfaceDescriptor<Object> next = (InterfaceDescriptor<Object>) method.getResult();
		InvocationProxy<Object> nproxy = new InvocationProxy<Object>(next, handler, invocation);
		return nproxy.toProxy();
	}
}
