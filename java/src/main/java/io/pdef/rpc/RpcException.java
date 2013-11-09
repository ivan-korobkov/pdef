package io.pdef.rpc;

import java.net.HttpURLConnection;

public class RpcException extends RuntimeException {
	private final int status;

	public RpcException(final int status) {
		this.status = status;
	}

	public RpcException(final int status, final String s) {
		super(s);
		this.status = status;
	}

	public RpcException(final int status, final Throwable throwable) {
		super(throwable);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public static RpcException methodNotAllowed(final String s) {
		return new RpcException(HttpURLConnection.HTTP_BAD_METHOD, s);
	}

	public static RpcException methodNotFound(final String s) {
		return new RpcException(HttpURLConnection.HTTP_NOT_FOUND, s);
	}
}
