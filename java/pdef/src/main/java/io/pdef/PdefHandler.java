package io.pdef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PdefHandler<T> {
	private static final String CHARSET_NAME = "UTF-8";
	private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			return dateFormat;
		}
	};

	private final T server;
	private final Class<T> iface;

	public PdefHandler(final Class<T> iface, final T server) {
		if (iface == null) throw new NullPointerException("iface");
		if (server == null) throw new NullPointerException("object");

		this.iface = iface;
		this.server = server;
	}

	public PdefResponse<Object> handle(final PdefRequest request) {
		PdefInvocation invocation = parseRequest(request, iface);
		List<PdefInvocation> chain = invocation.toChain();

		Object result = server;
		for (PdefInvocation inv : chain) {
			result = inv.invoke(result);
		}
		
		return new PdefResponse<Object>().setData(result);
	}

	@Nonnull
	static PdefInvocation parseRequest(final PdefRequest request, final Class<?> iface) {
		try {
			return doParseRequest(request, iface);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Nonnull
	private static PdefInvocation doParseRequest(final PdefRequest request, Class<?> iface)
			throws Exception {
		if (request == null) throw new NullPointerException("request");
		if (iface == null) throw new NullPointerException("iface");

		PdefInvocation invocation = null;
		LinkedList<String> path = splitPath(request.getRelativePath());
		Map<String, String> params = request.isPost() ? request.getPost() : request.getQuery();

		while (!path.isEmpty()) {

			// Find a method by a name.
			String name = path.removeFirst();
			Method method = getMethod(iface, name);
			assertThat(method != null, "Method is not found %s", name);

			// Check the required HTTP method.
			if (isPost(method)) {
				assertThat(request.isPost(), "Method not allowed, POST required");
			}

			// Parse arguments and create a next invocation.
			Object[] args = hasRequestArg(method) ? parseArgRequest(method, params)
			                                      : parseArgs(method, path, params);
			invocation = invocation == null ? new PdefInvocation(method, args)
			                                : invocation.next(method, args);

			// Stop on a terminal method, otherwise, proceed parsing the request.
			if (hasDataTypeResult(method)) {
				break;
			} else {
				assert method != null;
				iface = method.getReturnType();
			}
		}

		assertThat(path.isEmpty(), "Failed to parse an invocation chain");
		assertThat(invocation != null, "Method invocation required");
		assert invocation != null;
		assertThat(hasDataTypeResult(invocation.getMethod()),
				"The last method must be void or return a data type.");

		return invocation;
	}

	private static Object[] parseArgRequest(final Method method, final Map<String, String> params)
			throws Exception {
		Class<?> cls = method.getParameterTypes()[0];
		Object request = cls.newInstance();
		Field[] fields = cls.getDeclaredFields();

		for (Field field : fields) {
			String name = field.getName();
			String value = params.get(name);
			if (value == null) {
				continue;
			}

			Type type = field.getGenericType();
			Object arg = parseArg(type, value, name);
			field.setAccessible(true);
			field.set(request, arg);
		}

		return new Object[]{request};
	}

	private static Object[] parseArgs(final Method method,
			final LinkedList<String> path, final Map<String, String> params) throws Exception {
		Type[] types = method.getGenericParameterTypes();
		Annotation[][] annotations = method.getParameterAnnotations();

		Object[] args = new Object[types.length];
		if (args.length == 0) {
			return args;
		}

		for (int i = 0; i < types.length; i++) {
			Type type = types[i];
			String name = getArgName(annotations, i);
			String value;

			if (hasInterfaceResult(method)) {
				assertThat(!path.isEmpty(), "Wrong number of arguments for method \"%s\"",
						method.getName());
				value = urldecode(path.removeFirst());
			} else {
				value = params.get(name);
			}

			args[i] = value == null ? null : parseArg(type, value, name);
		}

		return args;
	}

	static Object parseArg(@Nonnull final Type type, @Nonnull final String value,
			final String name) throws Exception {
		try {
			if (type == String.class) return value;

			if (type == boolean.class || type == Boolean.class) return parseBoolean(value);
			if (type == short.class || type == Short.class) return Short.parseShort(value);
			if (type == int.class || type == Integer.class) return Integer.parseInt(value);
			if (type == long.class || type == Long.class) return Long.parseLong(value);
			if (type == float.class || type == Float.class) return Float.parseFloat(value);
			if (type == double.class || type == Double.class) return Double.parseDouble(value);
			if (type == Date.class) return parseDate(value);
			if (type instanceof Class<?> && ((Class) type).isEnum()) return parseEnum(type, value);

			return PdefJson.parse(value, type);
		} catch (Exception e) {
			throw new PdefException("Failed to parse an argument \"" + name + "\"", e);
		}
	}

	private static boolean parseBoolean(final String value) {
		if ("1".equals(value)) return true;
		if ("0".equals(value)) return false;
		return Boolean.parseBoolean(value);
	}

	private static Date parseDate(final String value) throws ParseException {
		DateFormat format = DATE_FORMAT.get();
		return format.parse(value);
	}

	@SuppressWarnings("unchecked")
	private static Enum<?> parseEnum(final Type type, final String value) {
		if (value == null) return null;
		String name = value.toUpperCase();

		try {
			return Enum.valueOf((Class<? extends Enum>) type, name);
		} catch (IllegalArgumentException e) {
			// Parse unknown enums as null.
			return null;
		}
	}

	private static LinkedList<String> splitPath(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (path.isEmpty()) {
			return new LinkedList<String>();
		}

		// The split() method discards trailing empty strings (i.e. the last slash).
		String[] partsArray = path.split("/");
		LinkedList<String> parts = new LinkedList<String>();
		Collections.addAll(parts, partsArray);
		return parts;
	}

	private static String urldecode(final String s) {
		try {
			return URLDecoder.decode(s, CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isPost(final Method method) {
		return method.isAnnotationPresent(POST.class);
	}

	private static boolean hasRequestArg(final Method method) {
		if (!method.isAnnotationPresent(Request.class)) {
			return false;
		}

		String name = method.getName();
		Class<?>[] params = method.getParameterTypes();
		assertThat(params.length == 1,
				"Method \"%s\" must have one struct request argument", name);

		Class<?> param = params[0];
		assertThat(!param.isPrimitive() && !param.isInterface() && !param.isEnum(),
				"Method \"%s\" must have one struct request argument", name);
		return true;
	}

	private static boolean hasDataTypeResult(final Method method) {
		return isDataType(method.getReturnType());
	}

	private static boolean hasInterfaceResult(final Method method) {
		return !hasDataTypeResult(method);
	}

	private static boolean isDataType(final Class<?> cls) {
		return !cls.isInterface() || cls == List.class || cls == Set.class || cls == Map.class;
	}

	@Nullable
	static Method getMethod(Class<?> iface, String name) {
		Method[] methods = iface.getMethods();
		for (Method method : methods) {
			String name1 = method.getName();
			if (name1.equals(name)) {
				return method;
			}
		}

		return null;
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

	private static void assertThat(final boolean expr, final String msg, final Object... objects) {
		if (expr) return;

		String message = String.format(msg, objects);
		throw new PdefException(message);
	}
}
