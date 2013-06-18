package io.pdef.invocation;

public interface RemoteHandler {
	<T> T apply(RemoteInvocation invocation);
}
