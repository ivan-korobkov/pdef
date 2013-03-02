package pdef.provided;

import pdef.ValueDescriptor;

public class NativeValueDescriptors {

	private static ValueDescriptor BOOL = new NativeValueDescriptor(Boolean.class);
	private static ValueDescriptor INT16 = new NativeValueDescriptor(Short.class);
	private static ValueDescriptor INT32 = new NativeValueDescriptor(Integer.class);
	private static ValueDescriptor INT64 = new NativeValueDescriptor(Long.class);
	private static ValueDescriptor FLOAT0 = new NativeValueDescriptor(Float.class);
	private static ValueDescriptor DOUBLE0 = new NativeValueDescriptor(Double.class);
	private static ValueDescriptor STRING = new NativeValueDescriptor(String.class);

	private NativeValueDescriptors() {}

	public static ValueDescriptor getBool() { return BOOL; }

	public static ValueDescriptor getInt16() { return INT16; }

	public static ValueDescriptor getInt32() { return INT32; }

	public static ValueDescriptor getInt64() { return INT64; }

	public static ValueDescriptor getFloat() { return FLOAT0; }

	public static ValueDescriptor getDouble() { return DOUBLE0; }

	public static ValueDescriptor getString() { return STRING; }
}
