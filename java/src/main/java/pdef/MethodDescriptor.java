package pdef;

import java.util.List;

public interface MethodDescriptor extends Symbol, Bindable<MethodDescriptor> {

	List<TypeDescriptor> getArgs();

	Object call(Interface iface, List<Object> args);
}
