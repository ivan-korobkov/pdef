package io.pdef.client;

import com.google.common.base.Function;
import io.pdef.Pdef;
import io.pdef.fluent.FluentFunctions;
import io.pdef.invocation.DefaultInvocationFactory;
import io.pdef.invocation.InvocationFactory;
import io.pdef.invocation.RemoteInvocation;
import io.pdef.rpc.Request;
import io.pdef.rpc.Response;

import java.lang.reflect.Type;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientBuilder<T> {
	private final Class<T> iface;
	private Function<Request, Response> handler;
	private InvocationFactory factory;
	private Pdef pdef;

	private ClientBuilder(final Class<T> iface) {
		this.iface = checkNotNull(iface);
		factory = new DefaultInvocationFactory();
	}

	public ClientBuilder<T> setPdef(final Pdef pdef) {
		this.pdef = pdef;
		return this;
	}

	public ClientBuilder<T> setHandler(final Function<Request, Response> handler) {
		this.handler = handler;
		return this;
	}

	public ClientBuilder<T> setFactory(final InvocationFactory factory) {
		this.factory = factory;
		return this;
	}

	public RemoteHandlerFactory handlerFactory() {
		return new RemoteHandlerFactory() {
			@Override
			public Function<RemoteInvocation, Object> create(final RemoteInvocation invocation) {
				Type resultType = invocation.getResultType();
				Type excType = invocation.getExcType();

				return FluentFunctions.of(new InvocationToRequest())
						.then(handler)
						.then(new ResponseToResult(resultType, excType, pdef));
			}
		};
	}

	public T build() {
		return new InvocationProxy<T>(iface, handlerFactory(), factory).proxy();
	}

	public static <T> ClientBuilder<T> create(final Class<T> iface) {
		return new ClientBuilder<T>(iface);
	}
}
