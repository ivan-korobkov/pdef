package pdef.rest;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import java.util.Map;

/** Simple REST request, which decouples the REST client/server from the transport.
 * The latter can be servlets, Netty, etc.
 *
 * The request contains an HTTP method, a url-encoded path, and two maps with query and post
 * params. The params must be unicode not url-encoded strings.
 * */
public class RestRequest {
	private String method;
	private String path = "";
	private Map<String, String> query = Maps.newLinkedHashMap();
	private Map<String, String> post = Maps.newLinkedHashMap();

	public RestRequest() {}

	public static RestRequest get() {
		return new RestRequest().setMethod(Rest.GET);
	}

	public static RestRequest post() {
		return new RestRequest().setMethod(Rest.POST);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(method)
				.addValue(path)
				.toString();
	}

	public boolean isPost() {
		return Objects.equal(method, Rest.POST);
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

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final RestRequest request = (RestRequest) o;

		if (method != null ? !method.equals(request.method) : request.method != null) return false;
		if (path != null ? !path.equals(request.path) : request.path != null) return false;
		if (post != null ? !post.equals(request.post) : request.post != null) return false;
		if (query != null ? !query.equals(request.query) : request.query != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = method != null ? method.hashCode() : 0;
		result = 31 * result + (path != null ? path.hashCode() : 0);
		result = 31 * result + (query != null ? query.hashCode() : 0);
		result = 31 * result + (post != null ? post.hashCode() : 0);
		return result;
	}
}
