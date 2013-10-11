package io.pdef.invoke;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MethodDescriptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class InvocationProxy implements InvocationHandler {
	private final InterfaceDescriptor<?> descriptor;
	private final Function<Invocation, InvocationResult> handler;
	private final Invocation parent;

	/** Creates a Java invocation proxy. */
	public static <T> T create(final Class<T> cls, final InterfaceDescriptor<?> descriptor,
			final Function<Invocation, InvocationResult> handler) {
		checkNotNull(cls);
		checkNotNull(descriptor);
		checkNotNull(handler);
		checkArgument(cls == descriptor.getJavaClass(), "Class/descriptor do not match, %s, %s",
				cls, descriptor);

		InvocationProxy invocationProxy = new InvocationProxy(descriptor, handler, Invocation.root());
		Object proxy = invocationProxy.toProxy();
		return cls.cast(proxy);
	}

	private InvocationProxy(final InterfaceDescriptor descriptor,
			final Function<Invocation, InvocationResult> handler,
			final Invocation parent) {
		this.descriptor = descriptor;
		this.handler = handler;
		this.parent = parent;
	}

	private Object toProxy() {
		Class<?> cls = descriptor.getJavaClass();
		return Proxy.newProxyInstance(cls.getClassLoader(), new Class<?>[]{cls}, this);
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

	private Object handle(final Invocation invocation) {
		InvocationResult result = handler.apply(invocation);
		assert result != null;

		if (result.isOk()) {
			return result.getData();
		} else {
			throw (RuntimeException) result.getData();
		}
	}

	private Object nextProxy(final Invocation invocation) {
		InterfaceDescriptor<?> next = (InterfaceDescriptor<?>) invocation.getResult();
		InvocationProxy nproxy = new InvocationProxy(next, handler, invocation);
		return nproxy.toProxy();
	}
}
