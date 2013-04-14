package pdef;

public interface MapDescriptor extends TypeDescriptor {

	TypeDescriptor getKey();

	TypeDescriptor getValue();

	@Override
	MapDescriptor parameterize(TypeDescriptor... args);
}
