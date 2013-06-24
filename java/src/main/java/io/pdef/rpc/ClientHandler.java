package io.pdef.rpc;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.pdef.Type;
import io.pdef.Invocation;
import io.pdef.fluent.FluentFunctions;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientHandler
		implements Function<Invocation, Function<Invocation, Object>> {
	private final Function<Request, Response> handler;

	public ClientHandler(final Function<Request, Response> handler) {
		this.handler = handler;
	}

	@Override
	public Function<Invocation, Object> apply(final Invocation invocation) {
		return FluentFunctions
				.of(new InvocationToRequest())
				.then(handler)
				.then(new ResponseToResult(null, null));
	}

	public static class InvocationToRequest implements Function<Invocation, Request> {
		@Override
		public Request apply(final Invocation input) {
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
		private final Type<?> result;
		private final Type<?> resultExc;

		public ResponseToResult(final Type<?> result, final Type<?> resultExc) {
			this.result = checkNotNull(result);
			this.resultExc = resultExc;
		}

		@Override
		public Object apply(final Response response) {
			ResponseStatus status = response.getStatus();

			Parser<?> parser;
			if (status == ResponseStatus.OK) parser = result;
			else if (status == ResponseStatus.ERROR) parser = null;
			else if (status == ResponseStatus.EXCEPTION) parser = resultExc;
			else throw new IllegalArgumentException("No status in response: " + response);

			ObjectInput input = new ObjectInput(response.getResult());
			Object result = parser.read(input);

			if (status == ResponseStatus.OK) return parser;
			throw (RuntimeException) result;
		}
	}
}
