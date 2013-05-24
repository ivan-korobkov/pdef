package io.pdef;

import java.util.List;

public interface InvocationChainHandler {
	Object handle(List<Pdef.Invocation> invocations);
}
