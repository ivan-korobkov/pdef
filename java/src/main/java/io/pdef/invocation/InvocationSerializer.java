package io.pdef.invocation;

import java.util.List;

public interface InvocationSerializer {

	Object serializer(List<Invocation> invocations);
}
