package io.pdef;

import java.util.List;

public interface Dispatchable {

	Object dispatch(List<Invocation> invocations);
}
