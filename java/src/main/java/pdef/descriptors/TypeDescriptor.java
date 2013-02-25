package pdef.descriptors;

import java.util.Map;

public interface TypeDescriptor {

	Class<?> getType();

	void link();

	TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);

}
