package io.pdef;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.fluent.FluentFuture;
import io.pdef.fluent.FluentFutures;
import io.pdef.rpc.*;
import static io.pdef.rpc.RpcExceptions.*;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/** Pdef interface descriptor. */
public class PdefInterface extends PdefDescriptor {
	private final Set<PdefInterface> bases;
	private final Map<String, PdefMethod> declaredMethods;

	/** Methods are computed lazily because we cannot guarantee that the bases are in the
	 * initialized state. For example, A->B->C, A has method with C result. Start with B and C
	 * will see it uninitialized. */
	private Map<String, PdefMethod> methods;

	PdefInterface(final Class<?> cls, final Pdef pdef) {
		super(PdefType.INTERFACE, cls, pdef);

		bases = buildBases(cls, pdef);
		declaredMethods = buildDeclaredMethods(cls, this);
	}

	public Set<PdefInterface> getBases() {
		return bases;
	}

	public Collection<PdefMethod> getDeclaredMethods() {
		return declaredMethods.values();
	}

	public Collection<PdefMethod> getMethods() {
		return getMethodMap().values();
	}

	@VisibleForTesting
	Map<String, PdefMethod> getDeclaredMethodMap() {
		return declaredMethods;
	}

	@VisibleForTesting
	Map<String, PdefMethod> getMethodMap() {
		if (methods == null) methods = buildMethods(bases, declaredMethods);
		return methods;
	}

	/** Returns a declared method, case-insensitive. */
	@Nullable
	public PdefMethod getDeclaredMethod(final String name) {
		checkNotNull(name);
		return declaredMethods.get(name.toLowerCase());
	}

	/** Returns a declared or inherited method, case-insensitive. */
	@Nullable
	public PdefMethod getMethod(final String name) {
		checkNotNull(name);
		getMethods();
		return methods.get(name.toLowerCase());
	}

	/** Invokes a chain of method calls and returns the result. */
	public Object invoke(final Object object, final MethodCall... calls) throws RpcException {
		return invoke(object, ImmutableList.copyOf(calls));
	}

	/** Invokes a chain of method calls and returns the result. */
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

			PdefMethod method = descriptor.getMethod(name);
			if (method == null) throw methodNotFound(path);

			o = method.invoke(o, call.getArgs());
			if (method.isInterface()) {
				// The method returns an interface, there should be more calls.
				if (!iterator.hasNext()) throw dataMethodCallRequired(path);

				descriptor = (PdefInterface) method.getResult();
				continue;
			}

			// It must be the last data type method.
			if (iterator.hasNext()) throw dataMethodReachedNoMoCalls(path);
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
			b.putAll(base.getMethodMap());
		}
		b.putAll(declaredMethods);
		return b.build();
	}
}
