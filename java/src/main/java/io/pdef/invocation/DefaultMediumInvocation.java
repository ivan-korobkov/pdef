package io.pdef.invocation;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Map;

public class DefaultMediumInvocation extends AbstractInvocation implements MediumInvocation {
	public DefaultMediumInvocation(final String method, final Map<String, Object> args,
			@Nullable final Type excType, @Nullable final Invocation parent) {
		super(method, args, excType, parent);
	}

	@Override
	public MediumInvocation createInvocation(final String method, final Map<String, Object> args) {
		return new DefaultMediumInvocation(method, args, getExcType(), this);
	}

	@Override
	public MediumInvocation createInvocation(final String method, final Map<String, Object> args,
			final Type excType) {
		return new DefaultMediumInvocation(method, args, excType, this);
	}

	@Override
	public RemoteInvocation createRemoteInvocation(final String method,
			final Map<String, Object> args, final Type resultType) {
		return new DefaultRemoteInvocation(method, args, resultType, getExcType(), this);
	}

	@Override
	public RemoteInvocation createRemoteInvocation(final String method,
			final Map<String, Object> args, final Type resultType, final Type excType) {
		return new DefaultRemoteInvocation(method, args, resultType, excType, this);
	}
}
