package io.pdef;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.pdef.rpc.*;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Client implements Function<Invocation, Object> {
	private final Function<Request, Response> sender;

	public Client(final Function<Request, Response> sender) {
		this.sender = checkNotNull(sender);
	}

	@Override
	public Object apply(final Invocation invocation) {
		Request request = serializeInvocation(invocation);
		Response response = sender.apply(request);
		checkNotNull(response);

		if (response != null) {
			Object result = response.getResult();
			switch (response.getStatus()) {
				case OK:
					return invocation.getResult().parse(result);
				case EXCEPTION:
					return invocation.getExc().parse(result);
				case ERROR:
					return RpcError.parse(result);
			}
		}

		throw RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("No response status")
				.build();
	}

	public Request serializeInvocation(final Invocation remote) {
		checkArgument(remote.isRemote(), "must be a remote invocation, got %s", remote);
		List<Invocation> invocations = remote.toList();

		List<MethodCall> calls = Lists.newArrayList();
		for (Invocation invocation : invocations) {
			MethodCall call = invocation.serialize();
			calls.add(call);
		}

		return Request.builder()
				.setCalls(calls)
				.build();
	}
}
