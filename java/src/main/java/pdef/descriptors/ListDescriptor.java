package pdef.descriptors;

public interface ListDescriptor extends TypeDescriptor {

	TypeDescriptor getElement();

	@Override
	ListDescriptor parameterize(TypeDescriptor... args);
}
