package io.pdef.rpc;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.pdef.ObjectInput;
import io.pdef.Pdef;
import io.pdef.Reader;
import io.pdef.fluent.FluentFunctions;
import io.pdef.invocation.Invocation;
import io.pdef.invocation.RemoteInvocation;

import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientHandler
		implements Function<RemoteInvocation, Function<RemoteInvocation, Object>> {
	private final Pdef pdef;
	private final Function<Request, Response> handler;

	public ClientHandler(final Pdef pdef, final Function<Request, Response> handler) {
		this.pdef = pdef;
		this.handler = handler;
	}

	@Override
	public Function<RemoteInvocation, Object> apply(final RemoteInvocation invocation) {
		Type resultType = invocation.getResultType();
		Type excType = invocation.getExcType();

		return FluentFunctions
				.of(new InvocationToRequest())
				.then(handler)
				.then(new ResponseToResult(resultType, excType, pdef));
	}

	public static class InvocationToRequest implements Function<RemoteInvocation, Request> {
		@Override
		public Request apply(final RemoteInvocation input) {
			List<Invocation> list = input.toList();
			List<MethodCall> calls = Lists.newArrayList();
			for (Invocation invocation : list) {
				calls.add(invocationToMethodCall(invocation));
			}

			return Request.builder()
					.setCalls(calls)
					.build();
		}

		public MethodCall invocationToMethodCall(final Invocation invocation) {
			return MethodCall.builder()
					.setMethod(invocation.getMethod())
					.setArgs(invocation.getArgs())
					.build();
		}
	}

	public static class ResponseToResult implements Function<Response, Object> {
		private final Type resultType;
		private final Type excType;
		private final Pdef pdef;

		public ResponseToResult(final Type resultType, final Type excType, final Pdef pdef) {
			this.pdef = pdef;
			this.resultType = checkNotNull(resultType);
			this.excType = excType;
		}

		@Override
		public Object apply(final Response response) {
			ResponseStatus status = response.getStatus();

			Reader<?> reader;
			if (status == ResponseStatus.OK) reader = pdef.getReader(resultType);
			else if (status == ResponseStatus.ERROR) reader = pdef.getReader(io.pdef.rpc.Error.class);
			else if (status == ResponseStatus.EXCEPTION) reader = pdef.getReader(excType);
			else throw new IllegalArgumentException("No status in response: " + response);

			ObjectInput input = new ObjectInput(response.getResult());
			Object result = reader.get(input);

			if (status == ResponseStatus.OK) return reader;
			throw (RuntimeException) result;
		}
	}
}
