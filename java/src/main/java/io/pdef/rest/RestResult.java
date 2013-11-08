package io.pdef.rest;

import io.pdef.descriptors.DataTypeDescriptor;

public class RestResult<T> {
	private final boolean ok;
	private final T data;
	private final DataTypeDescriptor<T> descriptor;

	private RestResult(final boolean ok, final T data, final DataTypeDescriptor<T> descriptor) {
		this.ok = ok;
		this.data = data;
		this.descriptor = descriptor;
	}

	public static <T> RestResult<T> ok(final T data, final DataTypeDescriptor<T> descriptor) {
		return new RestResult<T>(true, data, descriptor);
	}

	public static <E> RestResult<E> exc(final E exception, final DataTypeDescriptor<E> descriptor) {
		return new RestResult<E>(false, exception, descriptor);
	}

	public boolean isOk() {
		return ok;
	}

	public T getData() {
		return data;
	}

	public DataTypeDescriptor<T> getDescriptor() {
		return descriptor;
	}
}
