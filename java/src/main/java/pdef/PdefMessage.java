package pdef;

import pdef.descriptors.MessageDescriptor;

public interface PdefMessage extends PdefType {

	@Override
	MessageDescriptor getPdefDescriptor();
}
