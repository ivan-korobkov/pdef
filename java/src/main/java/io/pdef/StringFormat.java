package io.pdef;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

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

	public Object read(final Class<?> cls, final String s) {
		checkNotNull(cls);
		Pdef.TypeInfo info = pdef.get(cls);
		return read(info, s);
	}

	public Object read(final Pdef.TypeInfo info, final String s) {
		checkNotNull(info);

		try {
			switch (info.getType()) {
				case BOOL: return s != null && TRUE.equals(s);
				case INT16: return s == null ? (short) 0 : Short.parseShort(s);
				case INT32: return s == null ? 0 : Integer.parseInt(s);
				case INT64: return s == null ? 0 : Long.parseLong(s);
				case FLOAT: return s == null ? 0.0 : Float.parseFloat(s);
				case DOUBLE: return s == null ? 0.0 : Double.parseDouble(s);
				case STRING: return s;
				case ENUM: return s == null ? null : info.asEnum().getValues().get(s.toLowerCase());
			}
		} catch (Exception e) {
			throw new FormatException(e);
		}

		throw new FormatException("Unsupported type " + info);
	}

	public String write(@Nonnull final Object object) {
		checkNotNull(object);

		Pdef.TypeInfo info = pdef.get(object.getClass());
		return write(info, object);
	}

	public String write(final Pdef.TypeInfo info, final Object object) {
		checkNotNull(info);

		try {
			switch (info.getType()) {
				case BOOL: return ((Boolean) object) ? TRUE : FALSE;
				case INT16: return Short.toString((Short) object);
				case INT32: return Integer.toString((Integer) object);
				case INT64: return Long.toString((Long) object);
				case FLOAT: return Float.toString((Float) object);
				case DOUBLE: return Double.toString((Double) object);
				case STRING: return object == null ? "" : (String) object;
				case ENUM: return ((Enum<?>) object).name().toLowerCase();
			}
		} catch (Exception e) {
			throw new FormatException(e);
		}

		throw new FormatException("Unsupported type " + object);
	}
}
