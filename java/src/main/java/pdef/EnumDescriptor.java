package pdef;

import java.util.Map;

public interface EnumDescriptor extends TypeDescriptor {

	Map<String, Enum<?>> getValues();

	@Override
	String serialize(Object object);

	@Override
	Enum<?> parse(Object object);
}
