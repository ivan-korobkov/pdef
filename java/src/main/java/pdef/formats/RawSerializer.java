package pdef.formats;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import pdef.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RawSerializer extends AbstractSerializer {
	@Override
	public Map<String, Object> serialize(final Message message) {
		if (message == null) return null;
		return serializeMessage(message.getDescriptor(), message);
	}

	@Override
	protected Map<String, Object> serializeMessage(final MessageDescriptor descriptor,
			final Message message) {
		if (message == null) return null;
		MessageDescriptor polymorphicOrParameterized = getDescriptorForType(descriptor, message);
		SymbolTable<FieldDescriptor> fields = polymorphicOrParameterized.getFields();

		Map<String, Object> result = Maps.newLinkedHashMap();
		for (FieldDescriptor field : fields) {
			if (!field.isSet(message)) continue;

			String name = field.getName();
			TypeDescriptor type = field.getType();
			Object value = field.get(message);
			Object s = serialize(type, value);
			result.put(name, s);
		}

		return result;
	}

	private MessageDescriptor getDescriptorForType(final MessageDescriptor descriptor,
			final Message message) {
		MessageTree tree = descriptor.getTree();
		if (tree == null) return descriptor;
		return message.getDescriptor();
	}

	@Override
	protected String serializeEnum(final EnumDescriptor descriptor, final Enum<?> object) {
		if (object == null) return null;
		return object.name().toLowerCase();
	}

	@Override
	protected List<Object> serializeList(final ListDescriptor descriptor, final List<?> object) {
		if (object == null) return null;
		TypeDescriptor elementd = descriptor.getElement();

		List<Object> result = Lists.newArrayList();
		for (Object element : object) {
			Object s = serialize(elementd, element);
			result.add(s);
		}

		return result;
	}

	@Override
	protected Set<Object> serializeSet(final SetDescriptor descriptor, final Set<?> object) {
		if (object == null) return null;
		TypeDescriptor elementd = descriptor.getElement();

		Set<Object> result = Sets.newLinkedHashSet();
		for (Object element : object) {
			Object s = serialize(elementd, element);
			result.add(s);
		}

		return result;
	}

	@Override
	protected Map<Object, Object> serializeMap(final MapDescriptor descriptor,
			final Map<?, ?> object) {
		if (object == null) return null;
		TypeDescriptor keyd = descriptor.getKey();
		TypeDescriptor vald = descriptor.getValue();

		Map<Object, Object> result = Maps.newLinkedHashMap();
		for (Map.Entry<?, ?> e : object.entrySet()) {
			Object key = serialize(keyd, e.getKey());
			Object val = serialize(vald, e.getValue());
			result.put(key, val);
		}

		return result;
	}

	@Override
	protected Boolean serializeBoolean(final Boolean value) {
		return value == null ? false : value;
	}

	@Override
	protected Short serializeShort(final Short value) {
		return value == null ? (short) 0 : value;
	}

	@Override
	protected Integer serializeInt(final Integer value) {
		return value == null ? 0 : value;
	}

	@Override
	protected Long serializeLong(final Long value) {
		return value == null ? 0L : value;
	}

	@Override
	protected Float serializeFloat(final Float value) {
		return value == null ? 0f : value;
	}

	@Override
	protected Double serializeDouble(final Double value) {
		return value == null ? 0d : value;
	}

	@Override
	protected String serializeString(final String value) {
		return value == null ? null : value;
	}
}
