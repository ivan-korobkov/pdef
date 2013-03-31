package pdef.provided;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;
import pdef.ValueDescriptor;

public class NativeValueDescriptors {
	private static final ValueDescriptor BOOL = new NativeValueDescriptor(boolean.class) {
		@Override
		public Boolean serialize(final Object object) {
			if (object == null) return false;
			return (Boolean) object;
		}

		@Override
		public Boolean parse(final Object object) {
			if (object == null) return false;
			if (object instanceof Boolean) return (Boolean) object;
			if (object instanceof Integer) return (Integer) object == 1;
			if (object instanceof Long) return (Long) object == 1;
			String s = ((String) object).toLowerCase();
			return s.equals("true");
		}
	};
	private static final ValueDescriptor INT16 = new NativeValueDescriptor(short.class) {
		@Override
		public Short serialize(final Object object) {
			if (object == null) return 0;
			return (Short) object;
		}

		@Override
		public Short parse(final Object object) {
			if (object == null) return (short) 0;
			if (object instanceof Short) return (Short) object;
			if (object instanceof Integer) return Shorts.checkedCast((Integer) object);
			if (object instanceof Long) return Shorts.checkedCast((Long) object);
			String s = (String) object;
			return Short.parseShort(s);
		}
	};
	private static final ValueDescriptor INT32 = new NativeValueDescriptor(int.class) {
		@Override
		public Integer serialize(final Object object) {
			if (object == null) return 0;
			return (Integer) object;
		}

		@Override
		public Integer parse(final Object object) {
			if (object == null) return 0;
			if (object instanceof Integer) return (Integer) object;
			if (object instanceof Long) return Ints.checkedCast((Long) object);
			String s = (String) object;
			return Integer.parseInt(s);
		}
	};
	private static final ValueDescriptor INT64 = new NativeValueDescriptor(long.class) {
		@Override
		public Long serialize(final Object object) {
			if (object == null) return 0L;
			return (Long) object;
		}

		@Override
		public Long parse(final Object object) {
			if (object == null) return 0L;
			if (object instanceof Long) return (Long) object;
			String s = (String) object;
			return Long.parseLong(s);
		}
	};
	private static final ValueDescriptor FLOAT0 = new NativeValueDescriptor(float.class) {
		@Override
		public Float serialize(final Object object) {
			if (object == null) return 0f;
			return (Float) object;
		}

		@Override
		public Float parse(final Object object) {
			if (object == null) return 0f;
			if (object instanceof Float) return (Float) object;
			if (object instanceof Double) return (float) (double) (Double) object;
			if (object instanceof Integer) return (float) (Integer) object;
			if (object instanceof Long) return (float) (Long) object;
			String s = (String) object;
			return Float.parseFloat(s);
		}
	};
	private static final ValueDescriptor DOUBLE0 = new NativeValueDescriptor(double.class) {
		@Override
		public Double serialize(final Object object) {
			if (object == null) return 0d;
			return (Double) object;
		}

		@Override
		public Double parse(final Object object) {
			if (object == null) return 0d;
			if (object instanceof Double) return (Double) object;
			if (object instanceof Float) return (double) (Float) object;
			if (object instanceof Integer) return (double) (Integer) object;
			if (object instanceof Long) return (double) (Long) object;
			String s = (String) object;
			return Double.parseDouble(s);
		}
	};
	private static final ValueDescriptor STRING = new NativeValueDescriptor(String.class) {
		@Override
		public String serialize(final Object object) {
			if (object == null) return null;
			return (String) object;
		}

		@Override
		public String parse(final Object object) {
			if (object == null) return null;
			return (String) object;
		}
	};
	private static final ValueDescriptor VOID = new NativeValueDescriptor(void.class) {
		@Override
		public Object serialize(final Object object) {
			return null;
		}

		@Override
		public Object parse(final Object object) {
			return null;
		}
	};
	private static final ValueDescriptor OBJECT = new NativeValueDescriptor(Object.class) {
		@Override
		public Object serialize(final Object object) {
			return object;
		}

		@Override
		public Object parse(final Object object) {
			return object;
		}
	};

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
