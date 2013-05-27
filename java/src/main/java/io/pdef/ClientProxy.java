package io.pdef;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientProxy<T> implements InvocationHandler {
	private final Class<T> cls;
	private final Pdef.InterfaceInfo info;
	private final Function<List<Pdef.Invocation>, Object> handler;

	@Nullable private final ClientProxy parent;
	@Nullable private final Pdef.Invocation invocation;

	private T proxy;

	public ClientProxy(final Class<T> cls, final Pdef pdef,
			final Function<List<Pdef.Invocation>, Object> handler) {
		this.cls = checkNotNull(cls);
		this.handler = checkNotNull(handler);
		info = (Pdef.InterfaceInfo) pdef.get(cls);
		parent = null;
		invocation = null;
	}

	@SuppressWarnings("unchecked")
	private ClientProxy(final ClientProxy parent, final Pdef.Invocation invocation,
			final Function<List<Pdef.Invocation>, Object> handler, final Pdef.InterfaceInfo info) {
		this.cls = (Class<T>) info.getJavaClass();
		this.handler = checkNotNull(handler);
		this.parent = checkNotNull(parent);
		this.invocation = checkNotNull(invocation);
		this.info = checkNotNull(info);
	}

	public T proxy() {
		if (proxy == null) proxy = info.pdef.proxy(cls, this);
		return proxy;
	}

	@Override
	public Object invoke(final Object o, final Method method, final Object[] objects)
			throws Throwable {
		String name = method.getName().toLowerCase();
		Pdef.MethodInfo minfo = info.getMethods().get(name);
		if (minfo == null) return method.invoke(this, objects);

		Pdef.Invocation nextInvocation = new Pdef.Invocation(minfo, objects);
		Pdef.TypeInfo resultInfo = minfo.getResult();
		if (resultInfo.getType() == Pdef.TypeEnum.INTERFACE) {
			ClientProxy<Object> clientProxy = new ClientProxy<Object>(this, nextInvocation,
					handler, (Pdef.InterfaceInfo) resultInfo);
			return clientProxy.proxy();
		}

		List<Pdef.Invocation> chain = getChain(nextInvocation);
		return handler.apply(chain);

	}

	private List<Pdef.Invocation> getChain(final Pdef.Invocation last) {
		List<Pdef.Invocation> calls = Lists.newLinkedList();
		calls.add(last);

		ClientProxy client = this;
		while (client != null && client.invocation != null) {
			calls.add(0, client.invocation);
			client = client.parent;
		}

		return calls;
	}
}
