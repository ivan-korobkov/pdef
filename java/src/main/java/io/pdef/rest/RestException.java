package io.pdef.rest;

import java.net.HttpURLConnection;

public class RestException extends RuntimeException {
	private final int status;

	public RestException(final int status) {
		this.status = status;
	}

	public RestException(final int status, final String s) {
		super(s);
		this.status = status;
	}

	public RestException(final int status, final String s, final Throwable throwable) {
		super(s, throwable);
		this.status = status;
	}

	public RestException(final int status, final Throwable throwable) {
		super(throwable);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public static RestException methodNotAllowed(final String s) {
		return new RestException(HttpURLConnection.HTTP_BAD_METHOD, s);
	}

	public static RestException methodNotFound(final String s) {
		return new RestException(HttpURLConnection.HTTP_NOT_FOUND, s);
	}

	public static RestException serverError(final String s) {
		return new RestException(HttpURLConnection.HTTP_INTERNAL_ERROR, s);
	}

	public static RestException serviceUnavailable(final String s) {
		return new RestException(HttpURLConnection.HTTP_UNAVAILABLE, s);
	}

	public static RestException badRequest(final String s) {
		return new RestException(HttpURLConnection.HTTP_BAD_REQUEST, s);
	}
}
