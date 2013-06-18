package io.pdef.invocation;

import java.lang.reflect.Type;
import java.util.Map;

public class DefaultInvocationFactory implements InvocationFactory {
	@Override
	public MediumInvocation createInvocation(final String method, final Map<String, Object> args) {
		return createInvocation(method, args, null);
	}

	@Override
	public MediumInvocation createInvocation(final String method, final Map<String, Object> args,
			final Type excType) {
		return new DefaultMediumInvocation(method, args, excType, null);
	}

	@Override
	public RemoteInvocation createRemoteInvocation(final String method,
			final Map<String, Object> args, final Type resultType) {
		return createRemoteInvocation(method, args, resultType, null);
	}

	@Override
	public RemoteInvocation createRemoteInvocation(final String method,
			final Map<String, Object> args, final Type resultType, final Type excType) {
		return new DefaultRemoteInvocation(method, args, resultType, excType, null);
	}
}
