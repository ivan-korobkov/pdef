package io.pdef.rpc;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.pdef.Descriptor;
import io.pdef.Invocation;
import io.pdef.ObjectInput;
import io.pdef.Reader;
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
		private final Descriptor<?> result;
		private final Descriptor<?> resultExc;

		public ResponseToResult(final Descriptor<?> result, final Descriptor<?> resultExc) {
			this.result = checkNotNull(result);
			this.resultExc = resultExc;
		}

		@Override
		public Object apply(final Response response) {
			ResponseStatus status = response.getStatus();

			Reader<?> reader;
			if (status == ResponseStatus.OK) reader = result;
			else if (status == ResponseStatus.ERROR) reader = null;
			else if (status == ResponseStatus.EXCEPTION) reader = resultExc;
			else throw new IllegalArgumentException("No status in response: " + response);

			ObjectInput input = new ObjectInput(response.getResult());
			Object result = reader.read(input);

			if (status == ResponseStatus.OK) return reader;
			throw (RuntimeException) result;
		}
	}
}
