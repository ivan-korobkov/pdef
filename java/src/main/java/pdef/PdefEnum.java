package pdef;

import pdef.generated.GeneratedEnumDescriptor;

public interface PdefEnum extends PdefType {

	@Override
	GeneratedEnumDescriptor getPdefDescriptor();
}
