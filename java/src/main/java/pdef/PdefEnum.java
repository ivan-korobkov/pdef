package pdef;

import pdef.descriptors.BaseEnumDescriptor;

public interface PdefEnum extends PdefType {

	@Override
	BaseEnumDescriptor getPdefDescriptor();
}
