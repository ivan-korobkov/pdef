package io.pdef;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.fluent.FluentFuture;
import io.pdef.fluent.FluentFutures;
import io.pdef.rpc.*;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.pdef.rpc.RpcExceptions.*;

/** Pdef interface descriptor. */
public class PdefInterface extends PdefDescriptor {
	private final Set<PdefInterface> bases;
	private final Map<String, PdefMethod> methods;
	private final Map<String, PdefMethod> declaredMethods;

	PdefInterface(final Class<?> cls, final Pdef pdef) {
		super(PdefType.INTERFACE, cls, pdef);

		bases = buildBases(cls, pdef);
		declaredMethods = buildDeclaredMethods(cls, this);
		methods = buildMethods(bases, declaredMethods);
	}

	public Set<PdefInterface> getBases() {
		return bases;
	}

	public Map<String, PdefMethod> getMethods() {
		return methods;
	}

	public Map<String, PdefMethod> getDeclaredMethods() {
		return declaredMethods;
	}

	/** Use invokeRequest, invokeRequestAsync. Invokes a chain of calls, and returns the result. */
	public Object invoke(final Object object, final Iterable<MethodCall> calls)
			throws RpcException {
		checkNotNull(object);
		checkNotNull(calls);

		Object o = object;
		PdefInterface descriptor = this;
		StringBuilder path = new StringBuilder();

		Iterator<MethodCall> iterator = calls.iterator();
		if (!iterator.hasNext()) throw methodCallsRequired();

		while (iterator.hasNext()) {
			MethodCall call = iterator.next();
			String name = call.getMethod();
			path.append(name);

			PdefMethod method = descriptor.getMethods().get(name);
			if (method == null) throw methodNotFound(path);

			o = method.invoke(o, call.getArgs());
			if (method.isInterface()) {
				// The method returns an interface, there should be more calls.
				if (!iterator.hasNext()) throw moreMethodCallsRequired(path);

				descriptor = (PdefInterface) method.getResult();
				continue;
			}

			// It must be the last data type method.
			if (iterator.hasNext()) throw methodCallNotSupported(path);
		}
		return o;
	}

	/** Invokes an rpc request, and returns an rpc response, blocks on async calls,
	 * catches all exceptions. */
	public Response invokeRequest(final Object object, final Request request) {
		try {
			checkNotNull(object);
			checkNotNull(request);
			Object result = invoke(object, request.getCalls());
			if (result instanceof Future) result = ((Future) result).get();

			return Response.builder()
					.setStatus(ResponseStatus.OK)
					.setResult(result)
					.build();
		} catch (Exception e) {
			return errorResponse(e);
		}
	}

	/** Invokes an rpc request, and returns a fluent future rpc response, catches all exceptions. */
	public FluentFuture<Response> invokeRequestAsync(final Object object, final Request request) {
		try {
			checkNotNull(object);
			checkNotNull(request);
			Object result = invoke(object, request.getCalls());

			@SuppressWarnings("unchecked") // Cast to FluentFuture<Object> is safe.
			FluentFuture<Object> future = (result instanceof FluentFuture)
					? (FluentFuture<Object>) result : FluentFutures.of(result);
			return future.then(new Function<Object, Response>() {
				@Override
				public Response apply(final Object result) {
					return Response.builder()
							.setStatus(ResponseStatus.OK)
							.setResult(result)
							.build();
				}
			}).onErrorReturn(new Function<Exception, Response>() {
				@Override
				public Response apply(final Exception e) {
					return errorResponse(e);
				}
			});
		} catch (Exception e) {
			return FluentFutures.of(errorResponse(e));
		}
	}

	private Response errorResponse(final Exception e) {
		if (e instanceof RpcException) {
			return Response.builder()
					.setStatus(ResponseStatus.RPC_ERROR)
					.setRpcExc((RpcException) e)
					.build();
		}

		return Response.builder()
				.setStatus(ResponseStatus.RPC_ERROR)
				.setRpcExc(RpcException.builder()
						.setCode(RpcExceptionCode.SERVER_ERROR)
						.setText("Internal server error")
						.build())
				.build();
	}

	static ImmutableSet<PdefInterface> buildBases(final Class<?> cls, final Pdef pdef) {
		ImmutableSet.Builder<PdefInterface> b = ImmutableSet.builder();
		for (Class<?> base : cls.getInterfaces()) {
			PdefInterface descriptor = (PdefInterface) pdef.get(base);
			b.add(descriptor);
		}
		return b.build();
	}

	static ImmutableMap<String, PdefMethod> buildDeclaredMethods(final Class<?> cls,
			final PdefInterface iface) {
		ImmutableMap.Builder<String, PdefMethod> b = ImmutableMap.builder();
		for (Method method : cls.getDeclaredMethods()) {
			PdefMethod descriptor = new PdefMethod(method, iface);
			b.put(descriptor.getName(), descriptor);
		}
		return b.build();
	}

	static ImmutableMap<String, PdefMethod> buildMethods(final Set<PdefInterface> bases,
			final Map<String, PdefMethod> declaredMethods) {
		ImmutableMap.Builder<String, PdefMethod> b = ImmutableMap.builder();
		for (PdefInterface base : bases) {
			b.putAll(base.methods);
		}
		b.putAll(declaredMethods);
		return b.build();
	}
}
