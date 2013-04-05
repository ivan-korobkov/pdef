package pdef;

import java.util.Map;

public interface MethodDescriptor extends Symbol, Bindable<MethodDescriptor> {

	Map<String, TypeDescriptor> getArgs();

	Object call(Interface iface, Map<String, Object> args);
}
