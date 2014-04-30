/*
 * Copyright: 2013 Pdef <http://pdef.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pdef;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

public class PdefClient<T> {
	private final Class<T> iface;

	public PdefClient(final Class<T> iface, final String url) {
		this.iface = iface;
	}

	public PdefClient(final Class<T> iface, final String url, final Session session) {
		this.iface = iface;
	}

	public static interface Session {
		<T> void connection(HttpURLConnection connection, Request request,
				Class<T> result) throws IOException;

		<T> T error(HttpURLConnection connection, Request request, Class<T> result)
				throws IOException;
	}

	public static class Request implements Struct {
		private String method;
		private String relativePath;
		private Map<String, String> query = new LinkedHashMap<String, String>();
		private Map<String, String> post = new LinkedHashMap<String, String>();

		public Request() {}

		public Request(final Request another) {
			method = another.method;
			relativePath = another.relativePath;
			query = PdefCopy.copy(another.query);
			post = PdefCopy.copy(another.post);
		}

		public String getMethod() {
			return method;
		}

		public void setMethod(final String method) {
			this.method = method;
		}

		public String getRelativePath() {
			return relativePath;
		}

		public void setRelativePath(final String relativePath) {
			this.relativePath = relativePath;
		}

		public Map<String, String> getQuery() {
			return query;
		}

		public void setQuery(final Map<String, String> query) {
			this.query = query;
		}

		public Map<String, String> getPost() {
			return post;
		}

		public void setPost(final Map<String, String> post) {
			this.post = post;
		}

		@Override
		public Request copy() {
			return new Request(this);
		}
	}
}
