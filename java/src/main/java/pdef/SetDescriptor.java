package pdef;

import java.util.Set;

public interface SetDescriptor extends TypeDescriptor {

	TypeDescriptor getElement();

	@Override
	SetDescriptor parameterize(TypeDescriptor... args);

	@Override
	Set<Object> serialize(Object object);

	@Override
	Set<Object> parse(Object object);
}
