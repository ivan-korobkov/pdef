package io.pdef;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PdefClient<T> {
	static final String GET = "GET";
	static final String POST = "POST";
	static final String UTF8_NAME = "UTF-8";
	static final Charset UTF8 = Charset.forName(UTF8_NAME);
	static final String CONTENT_TYPE_HEADER = "Content-Type";
	static final String CONTENT_LENGTH_HEADER = "Content-Length";
	static final String APPLICATION_X_WWW_FORM_URLENCODED =
			"application/x-www-form-urlencoded;charset=utf-8";
	static final int MAX_RPC_EXCEPTION_MESSAGE_LEN = 256;

	private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			return dateFormat;
		}
	};

	private final String url;
	private final Class<T> iface;
	private final PdefClientSession session;

	public PdefClient(final String url, final Class<T> iface) {
		this(url, iface, new DefaultSession());
	}

	public PdefClient(final String url, final Class<T> iface, final PdefClientSession session) {
		if (url == null) throw new NullPointerException("url");
		if (iface == null) throw new NullPointerException("iface");
		if (session == null) throw new NullPointerException("session");

		this.url = url;
		this.iface = iface;
		this.session = session;
	}

	public String getUrl() {
		return url;
	}

	public Class<T> getIface() {
		return iface;
	}

	public T proxy() {
		return PdefProxy.create(iface, this);
	}

	public Object handle(final List<PdefInvocation> invocations) {
		PdefRequest request = serializeInvocations(invocations);
		PdefInvocation last = invocations.get(invocations.size() - 1);
		Type resultType = last.getMethod().getGenericReturnType();
		return handle(request, resultType);
	}

	public Object handle(final PdefRequest request, final Type resultType) {
		try {
			URL url = buildUrl(this.url, request);
			HttpURLConnection connection = openConnection(url, request);
			session.connectionOpened(connection);

			try {
				if (request.isPost()) {
					sendPostData(connection, request);
				}

				connection.connect();
				int status = connection.getResponseCode();
				session.responseReceived(connection);

				if (status == HttpURLConnection.HTTP_OK) {
					// It's a successful response, try to read the result.
					InputStream stream = new BufferedInputStream(connection.getInputStream());
					Type type = PdefResponse.generic(resultType);
					PdefResponse<?> response = (PdefResponse<?>) PdefJson.parse(stream, type);
					return response == null ? null : response.getData();
				}
				
				session.handleError(connection);
				InputStream input = connection.getErrorStream();
				try {
					String message = input == null ? "No error description" : readString
							(connection,
									input);
					if (message.length() > MAX_RPC_EXCEPTION_MESSAGE_LEN) {
						message = message.substring(0, MAX_RPC_EXCEPTION_MESSAGE_LEN) + "...";
					}
					message = message.replace("\n", " ");
					message = message.replace("\r", " ");

					throw new PdefClientException("Status: " + status + ", message=" + message);
				} finally {
					closeLogExc(input);
				}
			} finally {
				connection.disconnect();
			}
		} catch (IOException e) {
			throw new PdefClientException(e);
		}
	}

	private URL buildUrl(final String url, final PdefRequest request)
			throws MalformedURLException, UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder(url);
		if (!url.endsWith("/")) {
			sb.append("/");
		}

		String relPath = request.getRelativePath();
		if (relPath.startsWith("/")) {
			relPath = relPath.substring(1);
		}
		sb.append(relPath);

		Map<String, String> query = request.getQuery();
		if (!query.isEmpty()) {
			sb.append("?");
			sb.append(buildParamsQuery(query));
		}

		return new URL(sb.toString());
	}

	/** Builds a urlencoded query string from a param map. */
	private String buildParamsQuery(final Map<String, String> params)
			throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
		if (params.isEmpty()) {
			return builder.toString();
		}

		String sep = "";
		for (Map.Entry<String, String> entry : params.entrySet()) {
			builder.append(sep);
			builder.append(urlencode(entry.getKey()));
			builder.append("=");
			builder.append(urlencode(entry.getValue()));
			sep = "&";
		}

		return builder.toString();
	}

	/** Opens a connection and sets its HTTP method. */
	private HttpURLConnection openConnection(final URL url, final PdefRequest request)
			throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		if (request.isPost()) {
			connection.setRequestMethod(POST);
			connection.setRequestProperty(CONTENT_TYPE_HEADER, APPLICATION_X_WWW_FORM_URLENCODED);
			connection.setDoOutput(true);
		} else {
			connection.setRequestMethod(GET);
		}
		return connection;
	}

	/** Sets the connection content-type and content-length and sends the post data. */
	private void sendPostData(final HttpURLConnection connection, final PdefRequest request)
			throws IOException {
		String post = buildParamsQuery(request.getPost());
		byte[] data = post.getBytes(UTF8);

		connection.setRequestProperty(CONTENT_TYPE_HEADER, APPLICATION_X_WWW_FORM_URLENCODED);
		connection.setRequestProperty(CONTENT_LENGTH_HEADER, String.valueOf(data.length));
		OutputStream out = new BufferedOutputStream(connection.getOutputStream());
		try {
			out.write(data);
		} finally {
			closeLogExc(out);
		}
	}

	/** Reads a string from an input stream, gets the charset from the content-type header. */
	private String readString(final HttpURLConnection connection, final InputStream input)
			throws IOException {
		Charset charset = guessContentTypeCharset(connection);

		StringBuilder sb = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset));
		try {
			boolean first = true;
			for (String line; (line = reader.readLine()) != null; ) {
				if (!first) {
					sb.append('\n');
				}
				sb.append(line);
				first = false;
			}
		} finally {
			closeLogExc(reader);
		}

		return sb.toString();
	}

	/** Returns a charset from the content type header or UTF8. */
	static Charset guessContentTypeCharset(final HttpURLConnection connection) {
		String contentType = connection.getHeaderField(CONTENT_TYPE_HEADER);
		if (contentType == null) {
			return UTF8;
		}

		String charset = null;
		for (String param : contentType.replace(" ", "").split(";")) {
			if (param.startsWith("charset=")) {
				charset = param.split("=", 2)[1];
				break;
			}
		}

		try {
			return Charset.forName(charset);
		} catch (Exception e) {
			return UTF8;
		}
	}

	static PdefRequest serializeInvocations(final List<PdefInvocation> invocations) {
		if (invocations == null) throw new NullPointerException("invocations");
		if (invocations.isEmpty()) throw new IllegalArgumentException("empty invocations");

		String httpMethod = GET;
		List<String> path = new ArrayList<String>();
		Map<String, String> params = new LinkedHashMap<String, String>();

		for (int i = 0; i < invocations.size(); i++) {
			PdefInvocation invocation = invocations.get(i);
			Method method = invocation.getMethod();
			path.add(method.getName());
			if (isPost(method)) {
				httpMethod = POST;
			}

			Object[] args = invocation.getArgs();
			Annotation[][] annotations = method.getParameterAnnotations();
			if (args.length == 0) {
				continue;
			}

			boolean isLast = i == invocations.size() - 1;
			if (isRequestMethod(method)) {
				Map<String, String> requestParams = serializeRequestArg(args[0]);
				params.putAll(requestParams);

			} else if (isPost(method) || isLast) {
				for (int j = 0; j < args.length; j++) {
					Object arg = args[j];
					String name = getArgName(annotations, j);
					String value = serializeArg(arg);
					params.put(name, value);
				}

			} else {
				for (Object arg : args) {
					String value = serializeArg(arg);
					path.add(value);
				}
			}
		}

		PdefRequest request = new PdefRequest();
		request.setMethod(httpMethod);
		request.setRelativePath(join(path));
		if (httpMethod.equals(GET)) {
			request.setQuery(params);
		} else {
			request.setPost(params);
		}

		return request;
	}

	private static Map<String, String> serializeRequestArg(final Object request) {
		if (request == null) return new HashMap<String, String>();

		Map<String, String> params = new HashMap<String, String>();
		Field[] fields = request.getClass().getFields();

		for (Field field : fields) {
			String name = field.getName();
			Object value;
			try {
				value = field.get(request);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			String param = serializeArg(value);
			params.put(name, param);
		}

		return params;
	}

	private static String serializeArg(final Object arg) {
		if (arg == null) return "";
		else if (arg instanceof String) return (String) arg;
		else if (arg instanceof Boolean) return ((Boolean) arg) ? "1" : "0";
		else if (arg instanceof Number) return arg.toString();
		else if (arg instanceof Date) return DATE_FORMAT.get().format((Date) arg);
		else if (arg instanceof Enum<?>) return ((Enum<?>) arg).name().toLowerCase();
		else return PdefJson.serialize(arg);
	}

	private static String join(final List<String> path) {
		StringBuilder sb = new StringBuilder();
		for (String s : path) {
			sb.append("/");
			sb.append(s);
		}
		return sb.toString();
	}

	private static boolean isRequestMethod(final Method method) {
		return method.isAnnotationPresent(Request.class);
	}

	private static boolean isPost(final Method method) {
		return method.isAnnotationPresent(POST.class);
	}

	private static String getArgName(final Annotation[][] annotations, final int index) {
		Annotation[] argAnns = annotations[index];
		for (int i = 0; i < argAnns.length; i++) {
			Annotation ann = argAnns[i];
			if (ann instanceof Name) {
				return ((Name) ann).value();
			}
		}

		throw new IllegalArgumentException("No method argument name, "
				+ "pdef method arguments must be annotated with @io.pdef.Name"
		);
	}

	/** Closes a closeable and logs an exception if any. */
	static void closeLogExc(@Nullable final Closeable closeable) {
		if (closeable == null) {
			return;
		}

		try {
			closeable.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static String urlencode(final String s) throws UnsupportedEncodingException {
		return URLEncoder.encode(s, UTF8_NAME);
	}

	static class DefaultSession implements PdefClientSession {
		@Override
		public void connectionOpened(final HttpURLConnection connection) throws IOException {}

		@Override
		public void responseReceived(final HttpURLConnection connection) throws IOException {}

		@Override
		public void handleError(final HttpURLConnection connection) throws IOException {}
	}
}
