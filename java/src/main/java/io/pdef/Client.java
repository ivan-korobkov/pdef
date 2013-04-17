package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.Reflection;
import io.pdef.descriptors.Descriptor;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MethodDescriptor;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Client<T> {
	@Nullable private final Client parent;
	@Nullable private final Invocation invocation;
	private final InvocationsHandler handler;
	private final Descriptor descriptor;

	@SuppressWarnings("unchecked")
	public static <T> Client<T> of(final Class<T> cls, final DescriptorPool pool,
			final InvocationsHandler handler) {
		checkNotNull(cls);
		checkNotNull(pool);
		checkNotNull(handler);
		InterfaceDescriptor descriptor = (InterfaceDescriptor) pool.getDescriptor(cls);
		return new Client<T>(null, null, handler, descriptor);
	}

	private Client(@Nullable final Client parent, @Nullable final Invocation invocation,
			final InvocationsHandler handler, final Descriptor resultDescriptor) {
		this.parent = parent;
		this.invocation = invocation;
		this.handler = checkNotNull(handler);
		this.descriptor = checkNotNull(resultDescriptor);
	}

	@SuppressWarnings("unchecked")
	public T proxy() {
		Class<?> cls = ((InterfaceDescriptor) descriptor).getJavaType();
		return (T) Reflection.newProxy(cls, new Handler());
	}

	private Object doInvoke(final Object o, final Method method, final Object[] objects)
			throws Throwable {
		String name = method.getName();
		InterfaceDescriptor iface = (InterfaceDescriptor) descriptor;
		MethodDescriptor methodDescriptor = iface.getMethods().get(name);
		if (methodDescriptor == null) return method.invoke(this, objects);

		Invocation nextInvocation = toInvocation(methodDescriptor, objects);
		Client nextClient = nextInvoker(methodDescriptor, nextInvocation);
		if (methodDescriptor.getResult() instanceof InterfaceDescriptor) {
			return nextClient.proxy();
		}

		return nextClient.handle();
	}

	private Client<?> nextInvoker(final MethodDescriptor methodDescriptor,
			final Invocation nextInvocation) {
		Descriptor result = methodDescriptor.getResult();
		return new Client<Object>(this, nextInvocation, handler, result);
	}

	private Invocation toInvocation(final MethodDescriptor methodDescriptor,
			final Object[] objects) {
		if (objects == null) return new Invocation(methodDescriptor, ImmutableList.of());

		ImmutableList<Object> args = ImmutableList.copyOf(objects);
		return new Invocation(methodDescriptor, args);
	}

	private Object handle() {
		List<Invocation> invocations = toInvocations();
		return handler.handle(invocations);
	}

	private List<Invocation> toInvocations() {
		List<Invocation> calls = Lists.newLinkedList();
		Client client = this;
		while (client != null && client.invocation != null) {
			calls.add(0, client.invocation);
			client = client.parent;
		}

		return ImmutableList.copyOf(calls);
	}

	private class Handler implements InvocationHandler {
		@Override
		public Object invoke(final Object o, final Method method, final Object[] objects)
				throws Throwable {
			return doInvoke(o, method, objects);
		}
	}
}
