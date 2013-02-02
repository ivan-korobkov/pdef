package com.ivankorobkov.pdef.data;

import com.ivankorobkov.pdef.GenericType;

public interface DataType extends GenericType {

	@Override
	DataTypeDescriptor getDescriptor();
}
