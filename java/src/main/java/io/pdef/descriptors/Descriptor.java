package io.pdef.descriptors;

import java.lang.reflect.Type;

public interface Descriptor {

	Type getJavaType();

	DescriptorType getType();

	void link();
}
