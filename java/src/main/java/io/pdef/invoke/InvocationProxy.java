package io.pdef.invoke;

import io.pdef.Func;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MethodDescriptor;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class InvocationProxy<T> implements InvocationHandler {
	private final InterfaceDescriptor<T> descriptor;
	private final Func<Invocation, InvocationResult> handler;
	@Nullable
	private final Invocation parent;

	/** Creates a custom client. */
	public static <T> T create(final Class<T> cls,
			final Func<Invocation, InvocationResult> invocationHandler) {
		if (cls == null) throw new NullPointerException("cls");
		if (invocationHandler == null) throw new NullPointerException("invocationHandler");

		InterfaceDescriptor<T> descriptor = InterfaceDescriptor.findDescriptor(cls);
		if (descriptor == null) {
			throw new IllegalArgumentException("Cannot find an interface descriptor in " + cls);
		}

		InvocationProxy<T> invocationProxy = new InvocationProxy<T>(descriptor, invocationHandler,
				null);
		return invocationProxy.toProxy();
	}

	private InvocationProxy(final InterfaceDescriptor<T> descriptor,
			final Func<Invocation, InvocationResult> handler,
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
		MethodDescriptor md = descriptor.findMethod(name);
		if (md == null) {
			// It must be the equals, hashCode, etc. method.
			return method.invoke(this, args);
		}

		Invocation invocation = capture(md, args);
		if (invocation.isRemote()) {
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
		InvocationResult result;
		try {
			result = handler.apply(invocation);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		assert result != null;

		if (result.isOk()) {
			return result.getData();
		} else {
			throw result.getExc();
		}
	}

	private Object nextProxy(final Invocation invocation) {
		@SuppressWarnings("unchecked")
		InterfaceDescriptor<Object> next = (InterfaceDescriptor<Object>) invocation.getResult();
		InvocationProxy<Object> nproxy = new InvocationProxy<Object>(next, handler, invocation);
		return nproxy.toProxy();
	}
}
