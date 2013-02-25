package pdef.descriptors;

import java.util.Map;

public interface TypeDescriptor {

	Class<?> getJavaClass();

	void link();

	TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);

}
