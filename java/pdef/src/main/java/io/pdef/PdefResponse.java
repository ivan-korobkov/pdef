package io.pdef;

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
}
