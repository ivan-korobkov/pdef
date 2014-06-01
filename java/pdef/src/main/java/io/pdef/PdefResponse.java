package io.pdef;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class PdefResponse<T> extends AbstractStruct {
	private T data;
	
	public PdefResponse() {}

	public PdefResponse(final PdefResponse<T> another) {
		this.data = PdefCopy.copy(another.data);
	}

	public T getData() {
		return data;
	}

	public PdefResponse<T> setData(final T data) {
		this.data = data;
		return this;
	}

	@Override
	public Struct copy() {
		return new PdefResponse<T>(this);
	}

	/** Returns a generic parameterized response type. */
	public static <T> ParameterizedType generic(final Class<T> data) {
		return generic((Type) data);
	}

	/** Returns a generic parameterized response type. */
	public static ParameterizedType generic(final Type data) {
		if (data == null) throw new NullPointerException("data");

		return new ParameterizedType() {
			private final Type[] actualTypeArgs = new Type[]{data};

			@Override
			public Type[] getActualTypeArguments() {
				return actualTypeArgs.clone();
			}

			@Override
			public Type getRawType() {
				return PdefResponse.class;
			}

			@Override
			public Type getOwnerType() {
				return null;
			}
		};
	}
}
