package pdef.provided;

import pdef.descriptors.ValueDescriptor;

public class NativePrimitiveDescriptors {

	private static ValueDescriptor BOOL = new NativePrimitiveDescriptor(Boolean.class);
	private static ValueDescriptor INT16 = new NativePrimitiveDescriptor(Short.class);
	private static ValueDescriptor INT32 = new NativePrimitiveDescriptor(Integer.class);
	private static ValueDescriptor INT64 = new NativePrimitiveDescriptor(Long.class);
	private static ValueDescriptor FLOAT0 = new NativePrimitiveDescriptor(Float.class);
	private static ValueDescriptor DOUBLE0 = new NativePrimitiveDescriptor(Double.class);
	private static ValueDescriptor STRING = new NativePrimitiveDescriptor(String.class);

	private NativePrimitiveDescriptors() {}

	public static ValueDescriptor getBool() { return BOOL; }

	public static ValueDescriptor getInt16() { return INT16; }

	public static ValueDescriptor getInt32() { return INT32; }

	public static ValueDescriptor getInt64() { return INT64; }

	public static ValueDescriptor getFloat() { return FLOAT0; }

	public static ValueDescriptor getDouble() { return DOUBLE0; }

	public static ValueDescriptor getString() { return STRING; }
}
