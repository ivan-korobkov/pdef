package pdef;

import java.util.Map;

public interface MapDescriptor extends TypeDescriptor {

	TypeDescriptor getKey();

	TypeDescriptor getValue();

	@Override
	MapDescriptor parameterize(TypeDescriptor... args);

	@Override
	Map<Object, Object> serialize(Object object);

	@Override
	Map<Object, Object> parse(Object object);
}
