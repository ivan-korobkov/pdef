package pdef;

public interface SetDescriptor extends DataTypeDescriptor {

	TypeDescriptor getElement();

	@Override
	SetDescriptor parameterize(TypeDescriptor... args);
}
