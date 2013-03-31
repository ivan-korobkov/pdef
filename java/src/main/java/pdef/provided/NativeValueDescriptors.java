package pdef.provided;

import pdef.ValueDescriptor;

public class NativeValueDescriptors {
	private static final ValueDescriptor BOOL = new NativeValueDescriptor(boolean.class);
	private static final ValueDescriptor INT16 = new NativeValueDescriptor(short.class);
	private static final ValueDescriptor INT32 = new NativeValueDescriptor(int.class);
	private static final ValueDescriptor INT64 = new NativeValueDescriptor(long.class);
	private static final ValueDescriptor FLOAT0 = new NativeValueDescriptor(float.class);
	private static final ValueDescriptor DOUBLE0 = new NativeValueDescriptor(double.class);
	private static final ValueDescriptor STRING = new NativeValueDescriptor(String.class);
	private static final ValueDescriptor VOID = new NativeValueDescriptor(void.class);
	private static final ValueDescriptor OBJECT = new NativeValueDescriptor(Object.class);

	private NativeValueDescriptors() {}

	public static ValueDescriptor getBool() { return BOOL; }

	public static ValueDescriptor getInt16() { return INT16; }

	public static ValueDescriptor getInt32() { return INT32; }

	public static ValueDescriptor getInt64() { return INT64; }

	public static ValueDescriptor getFloat() { return FLOAT0; }

	public static ValueDescriptor getDouble() { return DOUBLE0; }

	public static ValueDescriptor getString() { return STRING; }

	public static ValueDescriptor getVoid() { return VOID; }

	public static ValueDescriptor getObject() { return OBJECT; }
}
