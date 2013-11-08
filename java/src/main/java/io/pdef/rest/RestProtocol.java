package io.pdef.rest;

import io.pdef.*;
import io.pdef.descriptors.*;
import io.pdef.formats.JsonFormat;
import io.pdef.invoke.Invocation;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

public class RestProtocol {
	public static final String CHARSET_NAME = "UTF-8";
	private final JsonFormat jsonFormat = JsonFormat.getInstance();

	public RestProtocol() {}

	/** Converts an invocation into a rest request. */
	public RestRequest getRequest(final Invocation invocation) {
		if (invocation == null) throw new NullPointerException("invocation");

		// Set the HTTP method.
		MethodDescriptor<?, ?> method = invocation.getMethod();
		RestRequest request = new RestRequest();
		if (method.isPost()) {
			request.setMethod(RestRequest.POST);
		} else {
			request.setMethod(RestRequest.GET);
		}

		for (Invocation invocation1 : invocation.toChain()) {
			writeInvocation(request, invocation1);
		}

		return request;
	}

	void writeInvocation(final RestRequest request, final Invocation invocation) {
		MethodDescriptor<?, ?> method = invocation.getMethod();

		Object[] args = invocation.getArgs();
		List<ArgumentDescriptor<?>> argds = method.getArgs();

		Map<String, String> pathArgs = new HashMap<String, String>();
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			ArgumentDescriptor argd = argds.get(i);

			if (argd.isPost()) {
				writeParam(argd, arg, request.getPost());
			} else if (argd.isQuery()) {
				writeParam(argd, arg, request.getQuery());
			} else {
				writeParam(argd, arg, pathArgs);
			}
		}

		String path = expandPath(method.getName(), pathArgs);
		request.appendPath(path);
	}

	// VisibleForTesting
	String expandPath(final String path, final Map<String, String> args) {
		Map<String, String> encoded = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : args.entrySet()) {
			encoded.put(entry.getKey(), urlencode(entry.getValue()));
		}
		return path;
	}

	/** Serializes a query/post argument and puts it into a dst map. */
	// VisibleForTesting
	@SuppressWarnings("unchecked")
	<V> void writeParam(final ArgumentDescriptor<V> argd, final V arg,
			final Map<String, String> dst) {
		DataTypeDescriptor<V> descriptor = argd.getType();
		if (!argd.isForm()) {
			// Serialize as a single json param.
			String serialized = toJson(descriptor, arg);
			dst.put(argd.getName(), serialized);
			return;
		}

		// It's a form, serialize each its field into a json param.
		// Mind polymorphic messages.
		Message message = (Message) arg;
		MessageDescriptor<Message> mdescriptor = (MessageDescriptor<Message>) message.descriptor();

		for (FieldDescriptor<? super Message, ?> field : mdescriptor.getFields()) {
			Object value = field.get(message);
			DataTypeDescriptor<Object> type = (DataTypeDescriptor<Object>) field.getType();
			if (value == null) {
				continue;
			}

			String s = toJson(type, value);
			dst.put(field.getName(), s);
		}
	}

	/** Serializes an argument to JSON, strips the quotes. */
	// VisibleForTesting
	<V> String toJson(final DataTypeDescriptor<V> descriptor, final V arg) {
		String s = jsonFormat.toJson(arg, descriptor, false);
		if (descriptor.getType() != TypeEnum.STRING) {
			return s;
		}

		// Remove the quotes.
		return s.substring(1, s.length() - 1);
	}

	// Invocation parsing.

	/** Parses an invocation from a rest request. */
	public Invocation getInvocation(final RestRequest request, InterfaceDescriptor<?> descriptor) {
		if (request == null) throw new NullPointerException("request");
		if (descriptor == null) throw new NullPointerException("descriptor");

		String path = request.getPath();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		// Split the path into a list of parts (method names and positions arguments).
		String[] partsArray = path.split("/", -1); // -1 disables discarding trailing empty
		// strings.
		LinkedList<String> parts = new LinkedList<String>();
		Collections.addAll(parts, partsArray);

		// Parse the parts as method invocations.
		Invocation invocation = null;
		while (!parts.isEmpty()) {
			String part = parts.removeFirst();

			// Find a method by name .
			MethodDescriptor<?, ?> method = descriptor.getMethod(part);
			if (method == null) {
				// Try to get an index method, if it is not found by a name.
				method = descriptor.getIndexMethod();
			}

			if (method == null) {
				// Method is not found.
				throw RestException.methodNotFound("Method is not found: " + part);
			}

			if (method.isIndex() && !part.equals("")) {
				// It's an index method, and the part does not equal
				// the method name. Prepend the part back, it's an argument.
				parts.addFirst(part);
			}

			if (method.isPost() && !request.isPost()) {
				// The method requires a POST HTTP request.
				throw RestException.methodNotAllowed("Method not allowed, POST required");
			}

			// Parse method arguments.
			boolean isPost = method.isPost();
			boolean isRemote = method.isRemote();

			List<Object> args = new ArrayList<Object>();
			Map<String, String> post = request.getPost();
			Map<String, String> query = request.getQuery();

			for (ArgumentDescriptor<?> argd : method.getArgs()) {
				if (isPost) {
					// Parse a post param.
					args.add(readParam(argd, post));

				} else if (isRemote) {
					// Parse a query param.
					args.add(readParam(argd, query));

				} else {
					// Parse a path argument.
					if (parts.isEmpty()) {
						throw RestException.methodNotFound("Wrong number of method args");
					}

					args.add(readPathArgument(argd, parts.removeFirst()));
				}
			}

			// Create a next invocation in a chain with the parsed arguments.
			if (invocation == null) {
				invocation = Invocation.root(method, args.toArray());
			} else {
				invocation = invocation.next(method, args.toArray());
			}

			if (method.isRemote()) {
				// It's the last method which returns a data type.
				// Stop parsing.

				if (!parts.isEmpty()) {
					// Cannot have any more parts here, bad url.
					throw RestException.methodNotFound(
							"Reached a remote method which returns a data type or is void. "
									+ "Cannot have any more path parts. ");
				}

				return invocation;
			}

			// It's an interface method.
			// Get the next interface and proceed parsing the parts.
			descriptor = (InterfaceDescriptor<?>) method.getResult();
		}

		// The parts are empty, and we failed to fromJson a remote method invocation.
		throw RestException.methodNotFound("The last method must be a remote one. It must return "
				+ "a data type or be void.");
	}

	// VisibleForTesting
	<V> V readPathArgument(final ArgumentDescriptor<V> argd, final String s) {
		String value = urldecode(s);
		return fromJson(argd.getType(), value);
	}

	// VisibleForTesting
	<V> V readParam(final ArgumentDescriptor<V> argd, final Map<String, String> src) {
		DataTypeDescriptor<V> descriptor = argd.getType();
		boolean isForm = descriptor instanceof ImmutableMessageDescriptor
				&& ((MessageDescriptor) descriptor).isForm();

		if (!isForm) {
			// Parse a single json string param.
			String serialized = src.get(argd.getName());
			return fromJson(descriptor, serialized);

		} else {
			// It's a form. Parse each its field as a param.
			// Mind polymorphic messages.

			MessageDescriptor<Message> mdescriptor = (MessageDescriptor<Message>) descriptor;
			if (mdescriptor.isPolymorphic()) {
				// Parse the discriminator field and get the subtype descriptor.
				FieldDescriptor<? super Message, ?> field = mdescriptor.getDiscriminator();
				assert field != null;
				String serialized = src.get(field.getName());
				Enum<?> value = (Enum<?>) fromJson(field.getType(), serialized);

				@SuppressWarnings("unchecked")
				MessageDescriptor<Message> subtype = (MessageDescriptor<Message>) mdescriptor
						.getSubtype(value);
				mdescriptor = subtype != null ? subtype : mdescriptor;
			}

			Message message = mdescriptor.newInstance();
			for (FieldDescriptor<? super Message, ?> field : mdescriptor.getFields()) {
				String serialized = src.get(field.getName());
				readFormField(message, field, serialized);
			}

			@SuppressWarnings("unchecked")
			V result = (V) message;
			return result;
		}
	}

	private <M, V> void readFormField(final M message, final FieldDescriptor<M, V> field,
			final String serialized) {
		V value = fromJson(field.getType(), serialized);
		if (value == null) {
			// Skip null fields.
			return;
		}

		field.set(message, value);
	}

	// VisibleForTesting
	/** Parses an argument from an unquoted JSON string. */
	<V> V fromJson(final DataTypeDescriptor<V> descriptor, String value) {
		if (value == null) {
			return null;
		}

		if (descriptor.getType() == TypeEnum.STRING) {
			value = "\"" + value + "\"";
		}
		return jsonFormat.fromJson(value, descriptor);
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
