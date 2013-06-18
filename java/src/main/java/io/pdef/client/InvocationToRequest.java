package io.pdef.client;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.pdef.invocation.Invocation;
import io.pdef.invocation.RemoteInvocation;
import io.pdef.rpc.MethodCall;
import io.pdef.rpc.Request;

import java.util.List;

public class InvocationToRequest implements Function<RemoteInvocation, Request> {

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
