package pdef;

import java.util.Map;

public interface TypeDescriptor {

	TypeDescriptor parameterize(TypeDescriptor... args);

	TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);
}
