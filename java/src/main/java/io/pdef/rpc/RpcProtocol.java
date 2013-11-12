package io.pdef.rpc;

import io.pdef.*;
import io.pdef.descriptors.*;
import io.pdef.formats.JsonFormat;
import io.pdef.Invocation;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

public class RpcProtocol {
	public static final String CHARSET_NAME = "UTF-8";
	private final JsonFormat format;

	public RpcProtocol() {
		this(JsonFormat.getInstance());
	}

	public RpcProtocol(final JsonFormat format) {
		if (format == null) throw new NullPointerException("format");
		this.format = format;
	}

	/** Converts an invocation into an rpc request. */
	public RpcRequest getRequest(final Invocation invocation) {
		if (invocation == null) throw new NullPointerException("invocation");

		MethodDescriptor<?, ?> method = invocation.getMethod();
		if (!method.isRemote()) {
			throw new IllegalArgumentException("Last invocation method must be remote");
		}

		RpcRequest request = new RpcRequest(method.isPost() ? RpcRequest.POST : RpcRequest.GET);
		for (Invocation invocation1 : invocation.toChain()) {
			writeInvocation(request, invocation1);
		}

		return request;
	}

	private void writeInvocation(final RpcRequest request, final Invocation invocation) {
		MethodDescriptor<?, ?> method = invocation.getMethod();

		Object[] args = invocation.getArgs();
		List<ArgumentDescriptor<?>> argds = method.getArgs();

		StringBuilder path = new StringBuilder(request.getPath())
				.append("/")
				.append(method.getName());

		Map<String, String> post = request.getPost();
		Map<String, String> query = request.getQuery();

		for (int i = 0; i < args.length; i++) {
			ArgumentDescriptor argd = argds.get(i);
			Object arg = args[i];

			String name = argd.getName();
			String value = toJson(argd.getType(), arg);

			if (argd.isPost()) {
				post.put(name, value);
			} else if (argd.isQuery()) {
				query.put(name, value);
			} else {
				path.append("/").append(urlencode(value));
			}
		}

		request.setPath(path.toString());
	}

	// VisibleForTesting
	/** Serializes an argument to JSON, strips the quotes. */
	<V> String toJson(final ValueDescriptor<V> descriptor, final V arg) {
		String s = format.toJson(arg, descriptor, false);
		if (descriptor.getType() != TypeEnum.STRING) {
			return s;
		}

		// Remove the quotes.
		return s.substring(1, s.length() - 1);
	}

	/** Parses an invocation from an rpc request. */
	public Invocation getInvocation(final RpcRequest request, InterfaceDescriptor<?> descriptor) {
		if (request == null) throw new NullPointerException("request");
		if (descriptor == null) throw new NullPointerException("descriptor");

		Invocation invocation = null;
		LinkedList<String> parts = splitPath(request.getPath());

		while (!parts.isEmpty()) {
			String part = parts.removeFirst();

			// Find a method by name.
			MethodDescriptor<?, ?> method = descriptor.getMethod(part);
			if (method == null) {
				throw RpcException.methodNotFound("Method is not found: " + part);
			}

			// Check the required HTTP method.
			if (method.isPost() && !request.isPost()) {
				throw RpcException.methodNotAllowed("Method not allowed, POST required");
			}

			// Parse arguments.
			List<Object> args = readArgs(method, parts, request.getQuery(), request.getPost());

			// Create a root invocation,
			// or a next invocation in a chain.
			invocation = invocation != null ? invocation.next(method, args.toArray())
			                                : Invocation.root(method, args.toArray());
			if (method.isRemote()) {
				break;
			}

			// It's an interface method.
			// Get the next interface and proceed parsing the parts.
			descriptor = (InterfaceDescriptor<?>) method.getResult();
		}

		if (!parts.isEmpty()) {
			throw RpcException.methodNotFound("Failed to parse an invocation chain");
		}

		if (invocation == null) {
			throw RpcException.methodNotFound("No methods");
		}

		if (!invocation.getMethod().isRemote()) {
			throw RpcException.methodNotFound("The last method must be a remote one. "
					+ "It must return a value type or be void.");
		}

		return invocation;
	}

	private List<Object> readArgs(final MethodDescriptor<?, ?> method,
			final LinkedList<String> parts, final Map<String, String> query,
			final Map<String, String> post) {
		List<Object> args = new ArrayList<Object>();

		for (ArgumentDescriptor<?> argd : method.getArgs()) {
			String value;
			String name = argd.getName();

			if (argd.isPost()) {
				value = post.get(name);
			} else if (argd.isQuery()) {
				value = query.get(name);
			} else if (parts.isEmpty()) {
				throw RpcException.methodNotFound("Wrong number of method args");
			} else {
				value = urldecode(parts.removeFirst());
			}

			Object arg = fromJson(argd.getType(), value);
			args.add(arg);
		}

		return args;
	}

	private LinkedList<String> splitPath(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		// The split() method discards trailing empty strings (i.e. the last slash).
		String[] partsArray = path.split("/");
		LinkedList<String> parts = new LinkedList<String>();
		Collections.addAll(parts, partsArray);
		return parts;
	}

	// VisibleForTesting
	/** Parses an argument from an unquoted JSON string. */
	<V> V fromJson(final ValueDescriptor<V> descriptor, String value) {
		if (value == null) {
			return null;
		}

		if (descriptor.getType() == TypeEnum.STRING) {
			value = "\"" + value + "\"";
		}

		return format.fromJson(value, descriptor);
	}

	/** Url-encodes a string. */
	static String urlencode(final String s) {
		try {
			return URLEncoder.encode(s, CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/** Url-decodes a string. */
	static String urldecode(final String s) {
		try {
			return URLDecoder.decode(s, CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
