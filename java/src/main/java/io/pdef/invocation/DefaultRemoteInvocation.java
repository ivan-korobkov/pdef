package io.pdef.invocation;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Map;

public class DefaultRemoteInvocation extends AbstractInvocation implements RemoteInvocation {
	private final Type resultType;


	public DefaultRemoteInvocation(final String method, final Map<String, Object> args,
			final Type resultType, @Nullable final Type excType,
			@Nullable final Invocation parent) {
		super(method, args, excType, parent);
		this.resultType = resultType;
	}

	@Override
	public Type getResultType() {
		return resultType;
	}
}
