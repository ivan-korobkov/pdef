package pdef.descriptors;

import java.util.Map;

public interface EnumDescriptor extends TypeDescriptor {

	Map<String, Enum<?>> getValues();
}
