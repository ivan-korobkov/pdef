package pdef;

public interface EnumType extends Type {

	Enum<?> getEnum();

	@Override
	String serialize();

	@Override
	EnumDescriptor getDescriptor();

}
