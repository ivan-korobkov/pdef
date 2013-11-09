package io.pdef.rpc;

import io.pdef.Invocation;
import io.pdef.InvocationProxy;
import io.pdef.Invoker;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MessageDescriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.descriptors.ValueDescriptor;

public class RpcClient<T> implements Invoker {
	private final InterfaceDescriptor<T> descriptor;
	private final RpcSession session;
	private final RpcProtocol protocol;

	public RpcClient(final InterfaceDescriptor<T> descriptor, final String url) {
		this(descriptor, new HttpUrlConnectionRpcSession(url));
	}

	public RpcClient(final InterfaceDescriptor<T> descriptor, final RpcSession session) {
		if (descriptor == null) throw new NullPointerException("descriptor");
		if (session == null) throw new NullPointerException("session");

		this.descriptor = descriptor;
		this.session = session;
		protocol = new RpcProtocol();
	}

	public T proxy() {
		return InvocationProxy.create(descriptor, this);
	}

	/**
	 * Serializes an invocation, sends an rpc request and returns the result.
	 */
	@Override
	public Object invoke(final Invocation invocation) throws Exception {
		if (invocation == null) throw new NullPointerException("invocation");

		MethodDescriptor<?, ?> method = invocation.getMethod();
		if (!method.isRemote()) throw new IllegalArgumentException("Method must be remote");

		ValueDescriptor<?> resultDescriptor = (ValueDescriptor<?>) method.getResult();
		MessageDescriptor<?> excDescriptor = method.getExc();

		RpcRequest request = protocol.getRequest(invocation);
		return session.send(request, resultDescriptor, excDescriptor);
	}
}
