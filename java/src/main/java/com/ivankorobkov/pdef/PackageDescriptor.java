package com.ivankorobkov.pdef;

import com.ivankorobkov.pdef.data.DataTypeDescriptor;

import java.util.Map;
import java.util.Set;

public interface PackageDescriptor extends GenericDescriptor {

	String getName();

	Set<Class<? extends PackageDescriptor>> getDependencies();

	Map<Class<?>, DataTypeDescriptor> getDefinitions();
}
