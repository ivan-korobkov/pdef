package com.ivankorobkov.pdef;

import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.data.DataTypeDescriptor;

public interface DescriptorPool {

	<T> DataTypeDescriptor get(TypeToken<T> token);

	void add(PackageDescriptor pkg);

	<T extends PackageDescriptor> T getPackage(Class<T> cls);
}
