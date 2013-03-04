package pdef.fixtures;

public enum Type implements pdef.EnumType {
	BASE, USER, PHOTO;

	@Override
	public pdef.EnumDescriptor getDescriptor() {
		return descriptor;
	}

	private static final pdef.EnumDescriptor descriptor =
			new pdef.generated.GeneratedEnumDescriptor(Type.class);
	public static pdef.EnumDescriptor getClassDescriptor() {
		return descriptor;
	}
}
