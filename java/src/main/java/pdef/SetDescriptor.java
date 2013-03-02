package pdef;

public interface SetDescriptor extends TypeDescriptor {

	TypeDescriptor getElement();

	@Override
	SetDescriptor parameterize(TypeDescriptor... args);
}
