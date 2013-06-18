package io.pdef.invocation;

import java.lang.reflect.Type;
import java.util.Map;

public interface InvocationFactory {
	/** Creates a new chain invocation. */
	MediumInvocation createInvocation(String method, Map<String, Object> args);

	/** Creates a new chain invocation. */
	MediumInvocation createInvocation(String method, Map<String, Object> args, Type excType);

	/** Creates a new remote invocation. */
	RemoteInvocation createRemoteInvocation(String method, Map<String, Object> args,
			Type resultType);

	/** Creates a new remote invocation. */
	RemoteInvocation createRemoteInvocation(String method, Map<String, Object> args,
			Type resultType, Type excType);
}
