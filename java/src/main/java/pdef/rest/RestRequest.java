package pdef.rest;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import java.util.Map;

public class RestRequest {
	private String method;
	private String path = "/";
	private Map<String, String> query = Maps.newLinkedHashMap();
	private Map<String, String> post = Maps.newLinkedHashMap();

	public RestRequest() {}

	public RestRequest(final String method, final String path, final Map<String, String> query,
			final Map<String, String> post) {
		this.method = method;
		this.path = path;

		if (query != null) {
			this.query.putAll(query);
		}

		if (post != null) {
			this.post.putAll(post);
		}
	}

	public static RestRequest post() {
		return new RestRequest().setMethod("POST");
	}

	public static RestRequest get() {
		return new RestRequest().setMethod("GET");
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

	public RestRequest setMethod(final String method) {
		this.method = method;
		return this;
	}

	public String getPath() {
		return path;
	}

	public RestRequest setPath(final String path) {
		this.path = path;
		return this;
	}

	public RestRequest appendPath(final String s) {
		path += s;
		return this;
	}

	public Map<String, String> getQuery() {
		return query;
	}

	public RestRequest setQuery(final Map<String, String> query) {
		this.query = query;
		return this;
	}

	public Map<String, String> getPost() {
		return post;
	}

	public RestRequest setPost(final Map<String, String> post) {
		this.post = post;
		return this;
	}

	public boolean isPost() {
		return method.toUpperCase().equals("POST");
	}
}
