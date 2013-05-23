package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.Reflection;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientProxy<T> implements InvocationHandler {
	private final Pdef.InterfaceInfo info;
	private final InvocationsHandler handler;

	@Nullable private final ClientProxy parent;
	@Nullable private final Pdef.Invocation invocation;

	public ClientProxy(final Class<T> cls, final InvocationsHandler handler, final Pdef pdef) {
		this.handler = checkNotNull(handler);
		info = (Pdef.InterfaceInfo) pdef.get(cls);
		parent = null;
		invocation = null;
	}

	private ClientProxy(final ClientProxy parent, final Pdef.Invocation invocation,
			final InvocationsHandler handler, final Pdef.InterfaceInfo info) {
		this.parent = checkNotNull(parent);
		this.invocation = checkNotNull(invocation);
		this.handler = checkNotNull(handler);
		this.info = checkNotNull(info);
	}

	@SuppressWarnings("unchecked")
	public T proxy() {
		Class<?> cls = info.getJavaClass();
		return (T) Reflection.newProxy(cls, this);
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
			return new ClientProxy<Object>(this, nextInvocation, handler,
					(Pdef.InterfaceInfo) resultInfo);
		}

		List<Pdef.Invocation> chain = getChain(nextInvocation);
		return handler.handle(chain);

	}

	private List<Pdef.Invocation> getChain(final Pdef.Invocation last) {
		List<Pdef.Invocation> calls = Lists.newLinkedList();
		calls.add(last);

		ClientProxy client = this;
		while (client != null && client.invocation != null) {
			calls.add(0, client.invocation);
			client = client.parent;
		}

		return ImmutableList.copyOf(calls);
	}
}
