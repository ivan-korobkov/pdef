package io.pdef.rpc;

import io.pdef.descriptors.ValueDescriptor;
import io.pdef.formats.JsonFormat;

import javax.annotation.Nullable;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

public class HttpUrlConnectionRpcSession implements RpcSession {
	public static final String GET = "GET";
	public static final String POST = "POST";
	public static final String UTF8_NAME = "UTF-8";
	public static final String CONTENT_TYPE_HEADER = "Content-Type";
	public static final String CONTENT_LENGTH_HEADER = "Content-Length";
	public static final String APPLICATION_X_WWW_FORM_URLENCODED =
			"application/x-www-form-urlencoded;charset=utf-8";

	public static final int APPLICATION_EXC_STATUS = 422;
	public static final int MAX_RPC_EXCEPTION_MESSAGE_LEN = 256;
	public static final Charset UTF8 = Charset.forName(UTF8_NAME);

	private final String url;
	private final JsonFormat format;

	public HttpUrlConnectionRpcSession(final String url) {
		this(url, JsonFormat.getInstance());
	}

	public HttpUrlConnectionRpcSession(final String url, final JsonFormat format) {
		if (url == null) throw new NullPointerException("url");
		if (format == null) throw new NullPointerException("format");

		this.url = url;
		this.format = format;
	}

	@Override
	public <T, E> T send(final RpcRequest request, final ValueDescriptor<T> resultDescriptor,
			final ValueDescriptor<E> excDescriptor) throws Exception {
		if (request == null) throw new NullPointerException("request");
		if (resultDescriptor == null) throw new NullPointerException("resultDescriptor");

		URL url = buildUrl(this.url, request);
		HttpURLConnection connection = openConnection(url, request);
		try {
			if (request.isPost()) {
				sendPostData(connection, request);
			}

			return handleResponse(connection, resultDescriptor, excDescriptor);
		} finally {
			connection.disconnect();
		}
	}

	protected URL buildUrl(final String url, final RpcRequest request)
			throws MalformedURLException, UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder(url);
		builder.append(request.getPath());

		Map<String, String> query = request.getQuery();
		if (!query.isEmpty()) {
			builder.append("?");
			builder.append(buildParamsQuery(query));
		}

		return new URL(builder.toString());
	}

	/** Builds a urlencoded query string from a param map. */
	protected String buildParamsQuery(final Map<String, String> params)
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
	protected HttpURLConnection openConnection(final URL url, final RpcRequest request)
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
	protected void sendPostData(final HttpURLConnection connection, final RpcRequest request)
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

	/** Reads a response. */
	protected <T, E> T handleResponse(final HttpURLConnection connection,
			final ValueDescriptor<T> resultDescriptor, final ValueDescriptor<E> excDescriptor)
			throws IOException {
		connection.connect();

		int status = connection.getResponseCode();
		if (status == HttpURLConnection.HTTP_OK) {
			// It's a successful response, try to read the result.
			return readResult(connection, resultDescriptor);

		} else if (status == APPLICATION_EXC_STATUS) {
			// It's an expected application exception.
			throw (RuntimeException) readApplicationException(connection, excDescriptor);

		} else {
			// Something bad happened, try to read the error message.
			throw readError(connection);
		}
	}

	protected <T> T readResult(final HttpURLConnection connection,
			final ValueDescriptor<T> resultDescriptor) throws IOException {
		InputStream input = new BufferedInputStream(connection.getInputStream());
		return format.fromJson(input, resultDescriptor);
	}

	protected <E> E readApplicationException(final HttpURLConnection connection,
			final ValueDescriptor<E> excDescriptor) throws IOException {
		int status = connection.getResponseCode();
		InputStream input = connection.getErrorStream();

		try {
			if (input == null) {
				throw new RpcException(status, "The server returned no data");
			} else if (excDescriptor == null) {
				throw new RpcException(status, "Unsupported application exception");
			}

			return format.fromJson(input, excDescriptor);
		} finally {
			closeLogExc(input);
		}
	}

	/** Reads an unexpected exception, throws RpcException. */
	protected IOException readError(final HttpURLConnection connection) throws IOException {
		int status = connection.getResponseCode();
		InputStream input = connection.getErrorStream();

		try {
			String message = input == null ? "No content" : readString(connection, input);
			if (message.length() > MAX_RPC_EXCEPTION_MESSAGE_LEN) {
				message = message.substring(0, 256) + "...";
			}
			message = message.replace("\n", " ");
			message = message.replace("\r", " ");

			throw new RpcException(status, message);
		} finally {
			closeLogExc(input);
		}
	}

	/** Reads a string from an input stream, gets the charset from the content-type header.*/
	protected String readString(final HttpURLConnection connection, final InputStream input)
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
	protected Charset guessContentTypeCharset(final HttpURLConnection connection) {
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

	/** Closes a closeable and logs an exception if any. */
	protected void closeLogExc(@Nullable final Closeable closeable) {
		if (closeable == null) {
			return;
		}

		try {
			closeable.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected String urlencode(final String s) throws UnsupportedEncodingException {
		return URLEncoder.encode(s, UTF8_NAME);
	}
}
