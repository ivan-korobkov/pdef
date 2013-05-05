package io.pdef;

import io.pdef.descriptors.Descriptor;

import java.util.List;

public interface Serializer {

	Object serialize(Object object);

	Object serialize(Descriptor descriptor, Object object);

	Object serializeInvocations(List<Invocation> invocations);
}
