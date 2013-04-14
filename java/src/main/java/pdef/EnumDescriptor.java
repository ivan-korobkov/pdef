package pdef;

import java.util.Map;

public interface EnumDescriptor extends TypeDescriptor {

	Map<String, Enum<?>> getValues();
}
