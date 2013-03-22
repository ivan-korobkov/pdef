package pdef;

import java.util.List;

public interface ListDescriptor extends TypeDescriptor {

	TypeDescriptor getElement();

	@Override
	ListDescriptor parameterize(TypeDescriptor... args);

	@Override
	List<Object> serialize(Object object);

	@Override
	List<Object> parse(Object object);
}
