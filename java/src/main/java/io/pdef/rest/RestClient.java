package io.pdef.rest;

import io.pdef.descriptors.DataTypeDescriptor;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MessageDescriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.Invocation;
import io.pdef.InvocationProxy;
import io.pdef.Invoker;

import java.nio.charset.Charset;

public class RestClient<T> implements Invoker {
	protected static final Charset CHARSET = Charset.forName("UTF-8");
	private final InterfaceDescriptor<T> descriptor;
	private final RestSession session;
	private final RestProtocol protocol;

	public RestClient(final InterfaceDescriptor<T> descriptor, final String url) {
		this(descriptor, new HttpRestSession(url));
	}

	public RestClient(final InterfaceDescriptor<T> descriptor, final RestSession session) {
		if (descriptor == null) throw new NullPointerException("descriptor");
		if (session == null) throw new NullPointerException("session");

		this.descriptor = descriptor;
		this.session = session;
		protocol = new RestProtocol();
	}

	public T proxy() {
		return InvocationProxy.create(descriptor, this);
	}

	/**
	 * Serializes an invocation, sends a rest request, parses a rest response,
	 * and returns the result or raises an exception.
	 */
	@Override
	public Object invoke(final Invocation invocation) throws Exception {
		if (invocation == null) throw new NullPointerException("invocation");

		MethodDescriptor<?, ?> method = invocation.getMethod();
		if (!method.isRemote()) throw new IllegalArgumentException("Method must be remote");

		DataTypeDescriptor<?> resultDescriptor = (DataTypeDescriptor<?>) method.getResult();
		MessageDescriptor<?> excDescriptor = method.getExc();

		RestRequest request = protocol.getRequest(invocation);
		return session.send(request, resultDescriptor, excDescriptor);
	}
}
