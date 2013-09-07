package pdef.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/** REST constants and utility methods. */
public class Rest {
	public static final String CHARSET = "UTF-8";

	public static final String GET = "GET";
	public static final String POST = "POST";

	public static final String JSON_MIME_TYPE = "application/json";
	public static final String TEXT_MIME_TYPE = "text/plain";
	public static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
	public static final String TEXT_CONTENT_TYPE = "text/plain; charset=utf-8";

	private Rest() {}

	/** Url-encodes a string. */
	static String urlencode(final String s) {
		try {
			return URLEncoder.encode(s, CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/** Url-decodes a string. */
	static String urldecode(final String s) {
		try {
			return URLDecoder.decode(s, CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
