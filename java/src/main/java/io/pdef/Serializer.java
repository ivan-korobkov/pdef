package io.pdef;

import java.util.List;

public interface Serializer {

	Object serialize(Object object);

	Object serializeInvocations(List<Invocation> invocations);
}
