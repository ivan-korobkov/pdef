package io.pdef.invocation;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import io.pdef.types.InterfaceMethod;
import io.pdef.types.InterfaceType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class InvocationProxy implements InvocationHandler {
	private final InterfaceType descriptor;
	private final Function<Invocation, InvocationResult> handler;
	private final Invocation parent;

	/** Creates a Java invocation proxy. */
	public static <T> T create(final Class<T> cls, final InterfaceType descriptor,
			final Function<Invocation, InvocationResult> handler) {
		checkNotNull(cls);
		checkNotNull(descriptor);
		checkNotNull(handler);
		checkArgument(cls == descriptor.getJavaClass(), "Class/type do not match, %s, %s", cls,
				descriptor);

		InvocationProxy invocationProxy = new InvocationProxy(descriptor, handler, Invocation.root());
		Object proxy = invocationProxy.toProxy();
		return cls.cast(proxy);
	}

	private InvocationProxy(final InterfaceType descriptor,
			final Function<Invocation, InvocationResult> handler,
			final Invocation parent) {
		this.descriptor = descriptor;
		this.handler = handler;
		this.parent = parent;
	}

	private Object toProxy() {
		Class<?> type = descriptor.getJavaClass();
		return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, this);
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args)
			throws Throwable {
		String name = method.getName();
		InterfaceMethod md = descriptor.findMethod(name);
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

	private Invocation capture(final InterfaceMethod md, final Object[] args) {
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
		InterfaceType ndescriptor = (InterfaceType) invocation.getResult();
		InvocationProxy nproxy = new InvocationProxy(ndescriptor, handler, invocation);
		return nproxy.toProxy();
	}
}
