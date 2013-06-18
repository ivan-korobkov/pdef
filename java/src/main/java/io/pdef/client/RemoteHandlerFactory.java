package io.pdef.client;

import com.google.common.base.Function;
import io.pdef.invocation.RemoteInvocation;

public interface RemoteHandlerFactory {
	Function<RemoteInvocation, Object> create(RemoteInvocation invocation);
}
