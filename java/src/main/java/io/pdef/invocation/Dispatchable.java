package io.pdef.invocation;

import java.util.List;

public interface Dispatchable {

	Object dispatch(List<Invocation> invocations);
}
