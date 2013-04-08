package pdef.rpc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.reflect.Reflection;
import pdef.InterfaceDescriptor;
import pdef.MethodDescriptor;
import pdef.TypeDescriptor;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class RpcInvocation implements InvocationHandler {
	@Nullable private final RpcInvocation parent;
	@Nullable private final Call call;
	private final RpcHandler handler;
	private final TypeDescriptor descriptor;

	public static RpcInvocation of(final InterfaceDescriptor descriptor, final RpcHandler handler) {
		return new RpcInvocation(null, null, handler, descriptor);
	}

	private RpcInvocation(@Nullable final RpcInvocation parent, @Nullable final Call call,
			final RpcHandler handler, final TypeDescriptor resultDescriptor) {
		this.parent = parent;
		this.call = call;
		this.handler = checkNotNull(handler);
		this.descriptor = checkNotNull(resultDescriptor);
	}

	@Override
	public Object invoke(final Object o, final Method method, final Object[] objects)
			throws Throwable {
		String name = method.getName();
		InterfaceDescriptor iface = (InterfaceDescriptor) descriptor;
		MethodDescriptor methodDescriptor = iface.getMethods().map().get(name);
		if (methodDescriptor == null) return method.invoke(this, objects);

		Call nextCall = toCall(methodDescriptor, objects);
		RpcInvocation nextInvoker = nextInvoker(methodDescriptor, nextCall);
		if (methodDescriptor.getResult() instanceof InterfaceDescriptor) {
			return nextInvoker.toProxy();
		}

		return nextInvoker.handle();
	}

	public Object toProxy() {
		Class<?> cls = ((InterfaceDescriptor) descriptor).getJavaClass();
		return Reflection.newProxy(cls, this);
	}

	private RpcInvocation nextInvoker(final MethodDescriptor methodDescriptor, final Call nextCall) {
		TypeDescriptor result = methodDescriptor.getResult();
		return new RpcInvocation(this, nextCall, handler, result);
	}

	private Call toCall(final MethodDescriptor methodDescriptor, final Object[] objects) {
		if (objects == null) return new Call(methodDescriptor, ImmutableMap.of());

		Iterator<Object> objectIterator = Iterators.forArray(objects);
		Iterator<String> argIterator = methodDescriptor.getArgs().keySet().iterator();

		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
		while (objectIterator.hasNext()) {
			Object object = objectIterator.next();
			String key = argIterator.next();
			builder.put(key, object);
		}

		ImmutableMap<String, Object> args = builder.build();
		return new Call(methodDescriptor, args);
	}

	private Object handle() {
		List<Call> calls = toCallList();
		return handler.handle(calls);
	}

	private List<Call> toCallList() {
		List<Call> calls = Lists.newLinkedList();
		RpcInvocation invoker = this;
		while (invoker != null && invoker.call != null) {
			calls.add(0, invoker.call);
			invoker = invoker.parent;
		}

		return ImmutableList.copyOf(calls);
	}
}
