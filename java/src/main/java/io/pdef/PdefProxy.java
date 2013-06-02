package io.pdef;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;
import io.pdef.rpc.MethodCall;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

public class PdefProxy<T> implements InvocationHandler {
	private final Class<T> cls;
	private final PdefInterface iface;
	private final Function<List<MethodCall>, Object> handler;

	@Nullable private final PdefProxy<?> parent;
	@Nullable private final MethodCall call;

	private T proxy;

	public PdefProxy(final Class<T> cls, final Pdef pdef,
			final Function<List<MethodCall>, Object> handler) {
		this.cls = checkNotNull(cls);
		this.handler = checkNotNull(handler);
		iface = (PdefInterface) pdef.get(cls);
		parent = null;
		call = null;
	}

	@SuppressWarnings("unchecked")
	private PdefProxy(final PdefProxy<?> parent, final MethodCall call,
			final Function<List<MethodCall>, Object> handler, final PdefInterface iface) {
		this.cls = (Class<T>) iface.getJavaClass();
		this.handler = checkNotNull(handler);
		this.parent = checkNotNull(parent);
		this.call = checkNotNull(call);
		this.iface = checkNotNull(iface);
	}

	public T proxy() {
		if (proxy == null) proxy = iface.pdef.proxy(cls, this);
		return proxy;
	}

	@Override
	public Object invoke(final Object o, final Method method, final Object[] objects)
			throws Throwable {
		String name = method.getName().toLowerCase();
		PdefMethod descriptor = iface.getMethod(name);
		if (descriptor == null) return method.invoke(this, objects);

		MethodCall nextCall = descriptor.createCall(objects);
		if (descriptor.isInterface()) {
			return new PdefProxy<Object>(this, nextCall, handler,
					descriptor.getResult().asInterface()).proxy();
		}

		List<MethodCall> chain = getChain(nextCall);
		return handler.apply(chain);

	}

	private List<MethodCall> getChain(final MethodCall last) {
		List<MethodCall> calls = Lists.newLinkedList();
		calls.add(last);

		PdefProxy<?> client = this;
		while (client != null && client.call != null) {
			calls.add(0, client.call);
			client = client.parent;
		}

		return calls;
	}
}
