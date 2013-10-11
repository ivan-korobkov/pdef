package io.pdef.descriptors;

public interface MethodInvoker {
	Object invoke(Object object, Object[] args) throws Exception;
}
