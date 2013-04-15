package io.pdef.invocation;

import java.util.List;

public interface Handler {

	Object handle(List<Invocation> invocations);
}
