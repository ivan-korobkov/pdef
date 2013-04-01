package pdef;

import java.util.Map;

public interface Bindable<T> {

	T bind(Map<VariableDescriptor, TypeDescriptor> argMap);
}
