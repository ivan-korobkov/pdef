package pdef.rest;

import java.net.HttpURLConnection;

/** Simple REST response, which decouples the REST client/server from the transport.
 * The latter can be servlets, Netty, etc.
 *
 * The response contains an HTTP status code, a decoded content string, and a content type.
 */
public class RestResponse {
	private int status;
	private String content;
	private String contentType;

	public RestResponse() {}

	public int getStatus() {
		return status;
	}

	public RestResponse setStatus(final int status) {
		this.status = status;
		return this;
	}

	public RestResponse withOkStatus() {
		this.status = HttpURLConnection.HTTP_OK;
		return this;
	}

	public boolean hasOkStatus() {
		return status == HttpURLConnection.HTTP_OK;
	}

	public String getContent() {
		return content;
	}

	public RestResponse setContent(final String content) {
		this.content = content;
		return this;
	}

	public String getContentType() {
		return contentType;
	}

	public RestResponse setContentType(final String contentType) {
		this.contentType = contentType;
		return this;
	}

	public RestResponse withTextContentType() {
		this.contentType = RestConstants.TEXT_CONTENT_TYPE;
		return this;
	}

	public RestResponse withJsonContentType() {
		this.contentType = RestConstants.JSON_CONTENT_TYPE;
		return this;
	}

	public boolean hasJsonContentType() {
		return contentType != null && contentType.toLowerCase()
				.startsWith(RestConstants.JSON_MIME_TYPE);
	}
}
