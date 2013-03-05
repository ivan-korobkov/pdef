package com.ivankorobkov.pdef.data;

import com.ivankorobkov.pdef.GenericType;

public interface EnumType<T extends Enum<T>> extends GenericType {

	

	@Override
	EnumDescriptor<T> getDescriptor();
}
