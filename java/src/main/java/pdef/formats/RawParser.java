package pdef.formats;

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.*;
import pdef.*;
import pdef.rpc.Call;
import pdef.rpc.DispatcherException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RawParser extends AbstractParser {
	@Override
	protected Message parseMessage(final MessageDescriptor descriptor, final Object object) {
		if (object == null) return null;
		Map<?, ?> map = (Map<?, ?>) object;
		MessageDescriptor polymorphic = parseDescriptorType(descriptor, map);
		SymbolTable<FieldDescriptor> fields = polymorphic.getFields();

		Message.Builder builder = polymorphic.newBuilder();
		for (FieldDescriptor field : fields) {
			String name = field.getName();
			if (!map.containsKey(name)) continue;

			TypeDescriptor type = field.getType();
			Object val = map.get(name);
			Object pval = doParse(type, val);
			// Even-though the field is read-only we still parse it to validate the data.
			if (field.isTypeField()) continue;
			field.set(builder, pval);
		}

		return builder.build();
	}

	private MessageDescriptor parseDescriptorType(final MessageDescriptor descriptor,
			final Map<?, ?> map) {
		Subtypes tree = descriptor.getSubtypes();
		if (tree == null) return descriptor;

		FieldDescriptor field = tree.getField();
		String name = field.getName();
		if (!map.containsKey(name)) return descriptor;

		TypeDescriptor type = field.getType();
		Object val = map.get(name);
		Object pval = doParse(type, val);
		MessageDescriptor subd = tree.getMap().get(pval);

		// TODO: Log if a subtype is not found.
		if (subd == null || subd == descriptor) return descriptor;
		return parseDescriptorType(subd, map);
	}

	@Override
	protected Enum<?> parseEnum(final EnumDescriptor descriptor, final Object object) {
		if (object == null) return null;
		String value = ((String) object).toUpperCase();
		return descriptor.getValues().get(value);
	}

	@Override
	protected List<Object> parseList(final ListDescriptor descriptor, final Object object) {
		if (object == null) return null;
		List<?> list = (List<?>) object;
		TypeDescriptor elementd = descriptor.getElement();

		ImmutableList.Builder<Object> builder = ImmutableList.builder();
		for (Object o : list) {
			Object p = doParse(elementd, o);
			builder.add(p);
		}

		return builder.build();
	}

	@Override
	protected Set<Object> parseSet(final SetDescriptor descriptor, final Object object) {
		if (object == null) return null;
		Set<?> set = (Set<?>) object;
		TypeDescriptor elementd = descriptor.getElement();

		ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
		for (Object o : set) {
			Object p = doParse(elementd, o);
			builder.add(p);
		}

		return builder.build();
	}

	@Override
	protected Map<Object, Object> parseMap(final MapDescriptor descriptor, final Object object) {
		if (object == null) return null;
		Map<?, ?> map = (Map<?, ?>) object;
		TypeDescriptor keyd = descriptor.getKey();
		TypeDescriptor vald = descriptor.getValue();

		ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			Object pkey = doParse(keyd, e.getKey());
			Object pval = doParse(vald, e.getValue());
			builder.put(pkey, pval);
		}

		return builder.build();
	}

	@Override
	protected Boolean parseBoolean(final Object value) {
		if (value == null) return false;
		if (value instanceof Boolean) return (Boolean) value;
		if (value instanceof Number) {
			int v = ((Number) value).intValue();
			if (v == 1) return true;
			if (v == 0) return false;
			throw new FormatException("Unexpected boolean value " + value);
		}
		String s = ((String) value).toLowerCase();
		if (s.equals("true")) return true;
		if (s.equals("false")) return false;
		throw new FormatException("Unexpected boolean value " + value);
	}

	@Override
	protected Short parseShort(final Object value) {
		if (value == null) return (short) 0;
		if (value instanceof Number) return ((Number) value).shortValue();
		String s = (String) value;
		return Short.parseShort(s);
	}

	@Override
	protected Integer parseInt(final Object value) {
		if (value == null) return 0;
		if (value instanceof Number) return ((Number) value).intValue();
		String s = (String) value;
		return Integer.parseInt(s);
	}

	@Override
	protected Long parseLong(final Object value) {
		if (value == null) return 0L;
		if (value instanceof Number) return ((Number) value).longValue();
		String s = (String) value;
		return Long.parseLong(s);
	}

	@Override
	protected Float parseFloat(final Object value) {
		if (value == null) return 0f;
		if (value instanceof Number) return ((Number) value).floatValue();
		String s = (String) value;
		return Float.parseFloat(s);
	}

	@Override
	protected Double parseDouble(final Object value) {
		if (value == null) return 0d;
		if (value instanceof Number) return ((Number) value).doubleValue();
		String s = (String) value;
		return Double.parseDouble(s);
	}

	@Override
	protected String parseString(final Object value) {
		return value == null ? null : (String) value;
	}

	@Override
	public List<Call> parseCalls(final InterfaceDescriptor descriptor, final Object object) {
		checkNotNull(descriptor);
		checkNotNull(object);

		Map<?, ?> map = (Map<?, ?>) object;
		List <Call> rawCalls = parseMethodNames(descriptor, map);
		return parseMethodArgs(rawCalls);
	}

	@VisibleForTesting
	List<Call> parseMethodNames(final InterfaceDescriptor descriptor, final Map<?, ?> map) {
		List<Call> calls = Lists.newArrayList();

		InterfaceDescriptor d = descriptor;
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object methodName = entry.getKey();
			if (sb.length() > 0) sb.append(".");
			sb.append(methodName);

			MethodDescriptor method = d.getMethods().map().get(methodName);
			if (method == null) {
				throw new DispatcherException("Method not found " + sb.toString());
			}

			Map<?, ?> args = (Map<?, ?>) entry.getValue();
			Call call = new Call(method, args);
			calls.add(call);
			TypeDescriptor result = method.getResult();
			if (result instanceof InterfaceDescriptor) {
				d = (InterfaceDescriptor) result;
			} else {
				break;
			}
		}

		return calls;
	}

	@VisibleForTesting
	List<Call> parseMethodArgs(final List<Call> rawCalls) {
		List<Call> calls = Lists.newArrayListWithCapacity(rawCalls.size());

		for (Call rawCall : rawCalls) {
			MethodDescriptor method = rawCall.getMethod();
			Object rawArgs = rawCall.getArgs();

			Map<String, Object> args = parseArgs(method.getArgs(), rawArgs);
			calls.add(new Call(method, args));
		}

		return calls;
	}

	@VisibleForTesting
	Map<String, Object> parseArgs(final Map<String, TypeDescriptor> descriptors,
			final Object rawArgs) {
		Map<?, ?> argMap = (Map<?, ?>) rawArgs;

		Map<String, Object> args = Maps.newLinkedHashMap();
		for (Map.Entry<String, TypeDescriptor> entry : descriptors.entrySet()) {
			String name = entry.getKey();
			TypeDescriptor descriptor = entry.getValue();
			Object rawValue = argMap.get(name);
			Object value = parse(descriptor, rawValue);
			args.put(name, value);
		}

		return args;
	}
}
