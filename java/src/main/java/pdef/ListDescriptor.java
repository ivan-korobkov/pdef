package pdef;

public interface ListDescriptor extends DataTypeDescriptor {

	TypeDescriptor getElement();

	@Override
	ListDescriptor parameterize(TypeDescriptor... args);
}
