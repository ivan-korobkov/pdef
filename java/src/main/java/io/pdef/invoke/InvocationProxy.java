package io.pdef.invoke;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import io.pdef.meta.InterfaceMethod;
import io.pdef.meta.InterfaceType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class InvocationProxy implements InvocationHandler {
	private final InterfaceType type;
	private final Function<Invocation, InvocationResult> handler;
	private final Invocation parent;

	/** Creates a Java invocation proxy. */
	public static <T> T create(final Class<T> cls, final InterfaceType type,
			final Function<Invocation, InvocationResult> handler) {
		checkNotNull(cls);
		checkNotNull(type);
		checkNotNull(handler);
		checkArgument(cls == type.getJavaClass(), "Class/type do not match, %s, %s", cls, type);

		InvocationProxy invocationProxy = new InvocationProxy(type, handler, Invocation.root());
		Object proxy = invocationProxy.toProxy();
		return cls.cast(proxy);
	}

	private InvocationProxy(final InterfaceType type,
			final Function<Invocation, InvocationResult> handler,
			final Invocation parent) {
		this.type = type;
		this.handler = handler;
		this.parent = parent;
	}

	private Object toProxy() {
		Class<?> cls = type.getJavaClass();
		return Proxy.newProxyInstance(cls.getClassLoader(), new Class<?>[]{cls}, this);
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args)
			throws Throwable {
		String name = method.getName();
		InterfaceMethod md = type.findMethod(name);
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
		InterfaceType ntype = (InterfaceType) invocation.getResult();
		InvocationProxy nproxy = new InvocationProxy(ntype, handler, invocation);
		return nproxy.toProxy();
	}
}
