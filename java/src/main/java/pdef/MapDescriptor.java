package pdef;

public interface MapDescriptor extends DataTypeDescriptor {

	TypeDescriptor getKey();

	TypeDescriptor getValue();

	@Override
	MapDescriptor parameterize(TypeDescriptor... args);
}
