package com.ivankorobkov.pdef.interfaces;

import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.GenericDescriptor;

import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

public interface InterfaceDescriptor extends GenericDescriptor {

	List<TypeToken<?>> getBaseTypes();

	List<InterfaceDescriptor> getBases();

	List<TypeVariable<?>> getVariables();

	List<MethodDescriptor> getMethods();

	Map<String, MethodDescriptor> getMethodMap();

	InterfaceDescriptor parameterize(TypeToken<?> parameterizedType);

}
