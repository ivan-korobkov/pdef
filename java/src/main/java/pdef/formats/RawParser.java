package pdef.formats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import pdef.*;

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
			Object pval = type.parse(val);
			// Even-though the field is read-only we still parse it to validate the data.
			if (field.isTypeField()) continue;
			field.set(builder, pval);
		}

		return builder.build();
	}

	private MessageDescriptor parseDescriptorType(final MessageDescriptor descriptor,
			final Map<?, ?> map) {
		MessageTree tree = descriptor.getTree();
		if (tree == null) return descriptor;

		FieldDescriptor field = tree.getField();
		String name = field.getName();
		if (!map.containsKey(name)) return descriptor;

		TypeDescriptor type = field.getType();
		Object val = map.get(name);
		Object pval = type.parse(val);
		MessageDescriptor subd = tree.getMap().get(pval);

		// TODO: Log if a subtype is not found.
		if (subd == null || subd == this) return descriptor;
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
			Object p = parse(elementd, o);
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
			Object p = parse(elementd, o);
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
			Object pkey = parse(keyd, e.getKey());
			Object pval = parse(vald, e.getValue());
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
}
