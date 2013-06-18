package io.pdef.invocation;

import java.lang.reflect.Type;

public interface RemoteInvocation extends Invocation {
	/** Returns the result invocation type. */
	Type getResultType();
}
