package io.pdef.rest;

import io.pdef.descriptors.ValueDescriptor;

public class RestResult<T> {
	private final boolean ok;
	private final T data;
	private final ValueDescriptor<T> descriptor;

	private RestResult(final boolean ok, final T data, final ValueDescriptor<T> descriptor) {
		this.ok = ok;
		this.data = data;
		this.descriptor = descriptor;
	}

	public static <T> RestResult<T> ok(final T data, final ValueDescriptor<T> descriptor) {
		return new RestResult<T>(true, data, descriptor);
	}

	public static <E> RestResult<E> exc(final E exception, final ValueDescriptor<E> descriptor) {
		return new RestResult<E>(false, exception, descriptor);
	}

	public boolean isOk() {
		return ok;
	}

	public T getData() {
		return data;
	}

	public ValueDescriptor<T> getDescriptor() {
		return descriptor;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final RestResult result = (RestResult) o;

		if (ok != result.ok) return false;
		if (data != null ? !data.equals(result.data) : result.data != null) return false;
		if (descriptor != null ? !descriptor.equals(result.descriptor) : result.descriptor != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (ok ? 1 : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		result = 31 * result + (descriptor != null ? descriptor.hashCode() : 0);
		return result;
	}
}
