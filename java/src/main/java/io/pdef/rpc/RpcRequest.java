package io.pdef.rpc;

import java.util.LinkedHashMap;
import java.util.Map;

/** Simple HTTP RPC request, which decouples the client/server from the transport.
 * The latter can be servlets, Netty, etc.
 *
 * The request contains an HTTP method, a url-encoded path, and two maps with query and post
 * params. The params must be unicode not url-encoded strings.
 * */
public class RpcRequest {
	public static final String GET = "GET";
	public static final String POST = "POST";

	private String method = GET;
	private String path = "";
	private Map<String, String> query = new LinkedHashMap<String, String>();
	private Map<String, String> post = new LinkedHashMap<String, String>();

	public RpcRequest() {}

	public RpcRequest(final String method) {
		setMethod(method);
	}

	@Override
	public String toString() {
		return "RpcRequest{" + method + ", path='" + path + '\'' + '}';
	}

	public boolean isPost() {
		return POST.equals(method);
	}

	public String getMethod() {
		return method;
	}

	public RpcRequest setMethod(final String method) {
		this.method = method;
		return this;
	}

	public String getPath() {
		return path;
	}

	public RpcRequest setPath(final String path) {
		this.path = path;
		return this;
	}

	public RpcRequest appendPath(final String s) {
		path += s;
		return this;
	}

	public Map<String, String> getQuery() {
		return query;
	}

	public RpcRequest setQuery(final Map<String, String> query) {
		this.query = query;
		return this;
	}

	public Map<String, String> getPost() {
		return post;
	}

	public RpcRequest setPost(final Map<String, String> post) {
		this.post = post;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final RpcRequest request = (RpcRequest) o;

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
