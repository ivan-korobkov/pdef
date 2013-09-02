package pdef.rest;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class RestServerRequest {
	private final String method;
	private final String path;
	private final Map<String, String> query;
	private final Map<String, String> post;

	public RestServerRequest(final String method, final String path,
			final Map<String, String> query, final Map<String, String> post) {
		this.method = method;
		this.path = path;
		this.query = query == null ? ImmutableMap.<String, String>of() : ImmutableMap.copyOf(query);
		this.post = post == null ? ImmutableMap.<String, String>of() : ImmutableMap.copyOf(post);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(method)
				.addValue(path)
				.toString();
	}

	public String getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

	public Map<String, String> getQuery() {
		return query;
	}

	public Map<String, String> getPost() {
		return post;
	}

	public boolean isPost() {
		return method.toUpperCase().equals("POST");
	}
}
