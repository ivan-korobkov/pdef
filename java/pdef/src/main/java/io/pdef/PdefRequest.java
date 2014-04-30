package io.pdef;

import java.util.LinkedHashMap;
import java.util.Map;

public class PdefRequest extends AbstractStruct {
	private String method;
	private String relativePath;
	private Map<String, String> query = new LinkedHashMap<String, String>();
	private Map<String, String> post = new LinkedHashMap<String, String>();

	public PdefRequest() {}

	public PdefRequest(final PdefRequest another) {
		method = another.method;
		relativePath = another.relativePath;
		query = PdefCopy.copy(another.query);
		post = PdefCopy.copy(another.post);
	}

	public String getMethod() {
		return method;
	}

	public PdefRequest setMethod(final String method) {
		this.method = method;
		return this;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public PdefRequest setRelativePath(final String relativePath) {
		this.relativePath = relativePath;
		return this;
	}

	public Map<String, String> getQuery() {
		return query;
	}

	public PdefRequest setQuery(final Map<String, String> query) {
		this.query = query;
		return this;
	}

	public Map<String, String> getPost() {
		return post;
	}

	public PdefRequest setPost(final Map<String, String> post) {
		this.post = post;
		return this;
	}

	public boolean isPost() {
		return "POST".equals(method);
	}

	@Override
	public PdefRequest copy() {
		return new PdefRequest(this);
	}
}
