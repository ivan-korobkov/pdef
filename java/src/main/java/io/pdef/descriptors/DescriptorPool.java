package io.pdef.descriptors;

import java.lang.reflect.Type;

public interface DescriptorPool {
	Descriptor getDescriptor(Type type);
}
