package pdef;

public interface EnumType extends DataType {

	Enum<?> getEnum();

	@Override
	EnumDescriptor getDescriptor();
}
