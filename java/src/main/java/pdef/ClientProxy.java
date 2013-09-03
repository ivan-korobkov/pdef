package pdef;

import com.google.common.base.Function;
import pdef.descriptors.InterfaceDescriptor;
import pdef.descriptors.MethodDescriptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

class ClientProxy implements InvocationHandler {
	private final InterfaceDescriptor descriptor;
	private final Function<Invocation, Object> handler;
	private final Invocation parent;

	/** Creates a client proxy. */
	static <T> T create(final Class<T> cls, final InterfaceDescriptor descriptor,
			final Function<Invocation, Object> handler) {
		checkNotNull(cls);
		checkNotNull(descriptor);
		checkNotNull(handler);
		checkArgument(cls == descriptor.getCls(), "Class/descriptor do not match, %s, %s",
				cls, descriptor);

		ClientProxy clientProxy = new ClientProxy(descriptor, handler, Invocation.root());
		Object proxy = clientProxy.toProxy();
		return cls.cast(proxy);
	}

	private ClientProxy(final InterfaceDescriptor descriptor,
			final Function<Invocation, Object> handler,
			final Invocation parent) {
		this.descriptor = descriptor;
		this.handler = handler;
		this.parent = parent;
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

		return handleInvocation(md, args);
	}

	private Object handleInvocation(final MethodDescriptor md, final Object[] args) {
		Invocation invocation = parent.next(md, args);
		if (invocation.isRemote()) {
			return handler.apply(invocation);
		}

		InterfaceDescriptor ndescriptor = (InterfaceDescriptor) md.getResult();
		ClientProxy nproxy = new ClientProxy(ndescriptor, handler, invocation);
		return nproxy.toProxy();
	}

	private Object toProxy() {
		Class<?> type = descriptor.getCls();
		return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, this);
	}
}
