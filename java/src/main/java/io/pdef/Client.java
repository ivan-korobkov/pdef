package io.pdef;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import io.pdef.rpc.*;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** Client constructors and functions. */
public class Client {
	private Client() {}

	// === HTTP ===

	/** Creates an http client from a url and a descriptor. */
	public static <T> T http(final String url, final InterfaceDescriptor<T> descriptor) {
		return proxyFromRpcHandler(descriptor, httpRpcHandler(url));
	}

	/** Creates an http client from a url, a descriptor and an HttpClient instance. */
	public static <T> T http(final String url, final InterfaceDescriptor<T> descriptor,
			final HttpClient client) {
		return proxyFromRpcHandler(descriptor, httpRpcHandler(url, client));
	}

	/** Creates a new http rpc handler. */
	public static Function<Request, Response> httpRpcHandler(final String url) {
		HttpClient client = new DefaultHttpClient();
		return httpRpcHandler(url, client);
	}

	/** Creates a new http rpc handler. */
	public static Function<Request, Response> httpRpcHandler(final String url,
			final HttpClient httpClient) {
		checkNotNull(url);
		checkNotNull(httpClient);
		return new Function<Request, Response>() {
			@Override
			public Response apply(final Request request) {
				return httpHandleRequest(request, url, httpClient);
			}
		};
	}

	/** Handles an rpc request using an http client. */
	private static Response httpHandleRequest(final Request request, final String url,
			final HttpClient httpClient) {
		try {
			HttpRequestBase httpRequest = httpSerializeRpcRequest(url, request);
			String content;
			try {
				HttpEntity entity = httpClient.execute(httpRequest).getEntity();
				InputStreamReader reader = new InputStreamReader(entity.getContent(), Charsets.UTF_8);
				content = CharStreams.toString(reader);
				EntityUtils.consume(entity);
			} finally {
				httpRequest.releaseConnection();
			}

			return Response.parseFromJson(content);
		} catch (Exception e) {
			throw RpcError.builder()
					.setCode(RpcErrorCode.BAD_REQUEST)
					.setText("Client error " + e)
					.build();
		}
	}

	/** Serializes an rpc request into an HttpClient request. */
	private static HttpRequestBase httpSerializeRpcRequest(final String url, final Request request) {
		HttpGet get = new HttpGet(url);
		return null;
	}

	// === RPC ===

	/** Creates a client from a descriptor and an rpc handler. */
	public static <T> T proxyFromRpcHandler(final InterfaceDescriptor<T> descriptor,
			final Function<Request, Response> handler) {
		Function<Invocation, Object> invoker = invoker(handler);
		return proxy(descriptor, invoker);
	}

	/** Creates a proxy client from a descriptor and an invocation handler. */
	public static <T> T proxy(final InterfaceDescriptor<T> descriptor,
			final Function<Invocation, Object> handler) {
		return descriptor.proxy(ProxyHandler.root(descriptor, handler));
	}

	/** Creates a new invoker from a remote handler. */
	public static Function<Invocation, Object> invoker(final Function<Request, Response> handler) {
		return new Function<Invocation, Object>() {
			@Override
			public Object apply(final Invocation invocation) {
				return invoke(invocation, handler);
			}
		};
	}

	/** Invokes a remote proxy using a given handler and returns the result. */
	public static Object invoke(final Invocation remote, final Function<Request, Response> handler) {
		checkNotNull(remote);
		Request request = rpcSerializeInvocation(remote);
		Response response = handler.apply(request);
		checkNotNull(response);

		if (response != null) {
			Object result = response.getResult();
			switch (response.getStatus()) {
				case OK:
					return remote.getResult().parse(result);
				case EXCEPTION:
					throw (RuntimeException) remote.getExc().parse(result);
				case ERROR:
					throw RpcError.parse(result);
			}
		}

		throw RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("No response status")
				.build();
	}

	/** Serializes a remote proxy into an rpc request. */
	public static Request rpcSerializeInvocation(final Invocation remote) {
		checkArgument(remote.isRemote(), "must be a remote invocation, got %s", remote);
		List<Invocation> invocations = remote.toList();

		List<MethodCall> calls = Lists.newArrayList();
		for (Invocation invocation : invocations) calls.add(invocation.serialize());

		return Request.builder()
				.setCalls(calls)
				.build();
	}

	static class ProxyHandler implements java.lang.reflect.InvocationHandler {
		private final Invocation parent;
		private final InterfaceDescriptor<?> iface;
		private final Function<Invocation, Object> handler;

		public static ProxyHandler root(final InterfaceDescriptor<?> iface,
				final Function<Invocation, Object> handler) {
			Invocation root = Invocation.root();
			return new ProxyHandler(root, iface, handler);
		}

		private ProxyHandler(final Invocation parent, final InterfaceDescriptor<?> iface,
				final Function<Invocation, Object> handler) {
			this.parent = checkNotNull(parent);
			this.iface = checkNotNull(iface);
			this.handler = checkNotNull(handler);
		}

		@Override
		public Object invoke(final Object o, final Method method, final Object[] args)
				throws Throwable {
			Map<String, MethodDescriptor> methods = iface.getMethods();
			if (!methods.containsKey(method.getName())) return method.invoke(this, args);

			MethodDescriptor descriptor = methods.get(method.getName());
			Invocation invocation = descriptor.capture(parent, args != null ? args : new Object[0]);
			if (invocation.isRemote()) return handler.apply(invocation);

			return new ProxyHandler(parent, descriptor.getNext(), handler);
		}
	}
}
