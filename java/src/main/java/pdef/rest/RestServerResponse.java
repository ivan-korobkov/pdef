package pdef.rest;

public class RestServerResponse {
	private final int status;
	private final String content;

	public RestServerResponse(final int status, final String content) {
		this.status = status;
		this.content = content;
	}
}
