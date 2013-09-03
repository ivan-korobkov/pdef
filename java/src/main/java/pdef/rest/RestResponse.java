package pdef.rest;

public class RestResponse {
	private final int status;
	private final String content;
	private final String contentType;

	public RestResponse(final int status, final String content, final String contentType) {
		this.status = status;
		this.content = content;
		this.contentType = contentType.toLowerCase();
	}

	public int getStatus() {
		return status;
	}

	public String getContent() {
		return content;
	}

	public String getContentType() {
		return contentType;
	}

	public boolean isOK() {
		return status == 200;
	}

	public boolean isApplicationJson() {
		return contentType.equals("application/json");
	}
}
