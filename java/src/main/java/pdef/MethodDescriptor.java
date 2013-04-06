package pdef;

import java.util.Map;

public interface MethodDescriptor extends Symbol, Bindable<MethodDescriptor> {

	Map<String, TypeDescriptor> getArgs();

	Object call(Object iface, Map<String, Object> args);
}
