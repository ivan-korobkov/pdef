package io.pdef.rest;

import com.google.common.base.Charsets;

import java.nio.charset.Charset;

/** REST constants and utility methods. */
public class Rest {
	public static final Charset CHARSET = Charsets.UTF_8;

	public static final String GET = "GET";
	public static final String POST = "POST";

	public static final String JSON_MIME_TYPE = "application/json";
	public static final String TEXT_MIME_TYPE = "text/plain";
	public static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
	public static final String TEXT_CONTENT_TYPE = "text/plain; charset=utf-8";

	private Rest() {}
}
