package io.pdef.invoke;

import io.pdef.Func;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MethodDescriptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class InvocationClient<T> implements InvocationHandler {
	private final InterfaceDescriptor<T> descriptor;
	private final Func<Invocation, InvocationResult> handler;
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

		return InvocationClient.create(cls, descriptor, invocationHandler);
	}

	/** Creates a Java invocation proxy. */
	public static <T> T create(final Class<T> cls, final InterfaceDescriptor<T> descriptor,
			final Func<Invocation, InvocationResult> handler) {
		if (cls == null) throw new NullPointerException("cls");
		if (descriptor == null) throw new NullPointerException("descriptor");
		if (handler == null) throw new NullPointerException("handler");

		InvocationClient<T> invocationClient = new InvocationClient<T>(descriptor, handler,
				Invocation.root());
		return invocationClient.toProxy();
	}

	private InvocationClient(final InterfaceDescriptor<T> descriptor,
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
		return parent.next(md, args);
	}

	private Object handle(final Invocation invocation) throws Exception {
		InvocationResult result = handler.apply(invocation);
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
		InvocationClient<Object> nproxy = new InvocationClient<Object>(next, handler, invocation);
		return nproxy.toProxy();
	}
}
