package io.pdef.invocation;

import java.util.List;

public interface InvocationSerializer {

	Object serializeInvocations(List<Invocation> invocations);
}
