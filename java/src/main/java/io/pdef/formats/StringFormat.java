package io.pdef.formats;

import static com.google.common.base.Preconditions.*;
import io.pdef.Pdef;

import javax.annotation.Nonnull;

public class StringFormat {
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	private final Pdef pdef;

	public StringFormat() {
		this.pdef = new Pdef();
	}

	public StringFormat(final Pdef pdef) {
		this.pdef = checkNotNull(pdef);
	}

	@SuppressWarnings("unchecked")
	public <T> T read(final Class<T> cls, final String s) {
		checkNotNull(cls);
		PdefDescriptor info = pdef.get(cls);
		return (T) read(info, s);
	}

	public Object read(final PdefDescriptor descriptor, final String s) {
		checkNotNull(descriptor);

		try {
			switch (descriptor.getType()) {
				case BOOL: return s != null && TRUE.equals(s);
				case INT16: return s == null ? (short) 0 : Short.parseShort(s);
				case INT32: return s == null ? 0 : Integer.parseInt(s);
				case INT64: return s == null ? 0 : Long.parseLong(s);
				case FLOAT: return s == null ? 0.0f : Float.parseFloat(s);
				case DOUBLE: return s == null ? 0.0d : Double.parseDouble(s);
				case STRING: return s == null ? "" : s;
				case ENUM: {
					PdefEnum e = descriptor.asEnum();
					Enum<?> v = s == null ? null : e.getValues().get(s.toUpperCase());
					return v != null ? v : e.getDefaultValue();
				}
			}
		} catch (Exception e) {
			throw new FormatException(e);
		}

		throw new FormatException("Unsupported type " + descriptor);
	}

	public String write(@Nonnull final Object object) {
		checkNotNull(object);

		PdefDescriptor info = pdef.get(object.getClass());
		return write(info, object);
	}

	public String write(final PdefDescriptor descriptor, final Object o) {
		checkNotNull(descriptor);

		try {
			switch (descriptor.getType()) {
				case BOOL: return o == null ? FALSE : ((Boolean) o) ? TRUE : FALSE;
				case INT16: return o == null ? "0" : Short.toString((Short) o);
				case INT32: return o == null ? "0" : Integer.toString((Integer) o);
				case INT64: return o == null ? "0" : Long.toString((Long) o);
				case FLOAT: return o == null ? "0.0" : Float.toString((Float) o);
				case DOUBLE: return o == null ? "0.0" : Double.toString((Double) o);
				case STRING: return o == null ? "" : (String) o;
				case ENUM: {
					Enum<?> e = o != null ? (Enum<?>) o : descriptor.asEnum().getDefaultValue();
					return e.name().toLowerCase();
				}
			}
		} catch (Exception e) {
			throw new FormatException(e);
		}

		throw new FormatException("Unsupported type " + o);
	}
}
