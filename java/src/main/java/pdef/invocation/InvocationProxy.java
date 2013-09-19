package pdef.invocation;

import com.google.common.base.Function;
import pdef.descriptors.InterfaceDescriptor;
import pdef.descriptors.MethodDescriptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class InvocationProxy implements InvocationHandler {
	private final InterfaceDescriptor descriptor;
	private final Function<Invocation, InvocationResult> handler;
	private final Invocation parent;

	/** Creates a Java invocation proxy. */
	public static <T> T create(final Class<T> cls, final InterfaceDescriptor descriptor,
			final Function<Invocation, InvocationResult> handler) {
		checkNotNull(cls);
		checkNotNull(descriptor);
		checkNotNull(handler);
		checkArgument(cls == descriptor.getCls(), "Class/descriptor do not match, %s, %s",
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
		Class<?> type = descriptor.getCls();
		return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, this);
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
		InterfaceDescriptor ndescriptor = (InterfaceDescriptor) invocation.getResult();
		InvocationProxy nproxy = new InvocationProxy(ndescriptor, handler, invocation);
		return nproxy.toProxy();
	}
}
