package io.pdef.invocation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.Reflection;
import io.pdef.descriptors.Descriptor;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MethodDescriptor;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Invoker implements InvocationHandler {
	@Nullable private final Invoker parent;
	@Nullable private final Invocation invocation;
	private final Handler handler;
	private final Descriptor descriptor;

	public static Invoker of(final InterfaceDescriptor descriptor, final Handler handler) {
		return new Invoker(null, null, handler, descriptor);
	}

	private Invoker(@Nullable final Invoker parent, @Nullable final Invocation invocation,
			final Handler handler, final Descriptor resultDescriptor) {
		this.parent = parent;
		this.invocation = invocation;
		this.handler = checkNotNull(handler);
		this.descriptor = checkNotNull(resultDescriptor);
	}

	@Override
	public Object invoke(final Object o, final Method method, final Object[] objects)
			throws Throwable {
		String name = method.getName();
		InterfaceDescriptor iface = (InterfaceDescriptor) descriptor;
		MethodDescriptor methodDescriptor = iface.getMethods().get(name);
		if (methodDescriptor == null) return method.invoke(this, objects);

		Invocation nextInvocation = toInvocation(methodDescriptor, objects);
		Invoker nextInvoker = nextInvoker(methodDescriptor, nextInvocation);
		if (methodDescriptor.getResult() instanceof InterfaceDescriptor) {
			return nextInvoker.toProxy();
		}

		return nextInvoker.handle();
	}

	public Object toProxy() {
		Class<?> cls = ((InterfaceDescriptor) descriptor).getJavaType();
		return Reflection.newProxy(cls, this);
	}

	private Invoker nextInvoker(final MethodDescriptor methodDescriptor,
			final Invocation nextInvocation) {
		Descriptor result = methodDescriptor.getResult();
		return new Invoker(this, nextInvocation, handler, result);
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
		Invoker invoker = this;
		while (invoker != null && invoker.invocation != null) {
			calls.add(0, invoker.invocation);
			invoker = invoker.parent;
		}

		return ImmutableList.copyOf(calls);
	}
}
