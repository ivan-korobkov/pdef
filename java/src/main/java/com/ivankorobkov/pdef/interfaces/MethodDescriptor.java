package com.ivankorobkov.pdef.interfaces;

import com.ivankorobkov.pdef.GenericDescriptor;
import com.ivankorobkov.pdef.data.DataTypeDescriptor;

public interface MethodDescriptor extends GenericDescriptor {

	String getName();

	DataTypeDescriptor getIn();

	DataTypeDescriptor getOut();

	Object invoke(Object iface, Object in);
}
