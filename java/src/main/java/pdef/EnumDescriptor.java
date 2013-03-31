package pdef;

import java.util.Map;

public interface EnumDescriptor extends DataTypeDescriptor {

	Map<String, Enum<?>> getValues();
}
