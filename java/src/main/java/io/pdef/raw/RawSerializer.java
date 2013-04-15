package io.pdef.raw;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.pdef.AbstractSerializer;
import io.pdef.Message;
import io.pdef.descriptors.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RawSerializer extends AbstractSerializer {
	public RawSerializer() {
		super(new DefaultDescriptorPool());
	}

	public RawSerializer(final DescriptorPool pool) {
		super(pool);
	}

	@Override
	protected Map<String, Object> serializeMessage(final MessageDescriptor descriptor,
			final Message message) {
		if (message == null) return null;
		Collection<FieldDescriptor> fields = descriptor.getFields().values();

		Map<String, Object> result = Maps.newLinkedHashMap();
		for (FieldDescriptor field : fields) {
			String name = field.getName();
			Descriptor type = field.getType();
			Object value = field.get(message);
			Object s = serialize(type, value);
			result.put(name, s);
		}

		return result;
	}

	@Override
	protected String serializeEnum(final EnumDescriptor descriptor, final Enum<?> object) {
		if (object == null) return null;
		return object.name().toLowerCase();
	}

	@Override
	protected List<Object> serializeList(final ListDescriptor descriptor, final List<?> object) {
		if (object == null) return null;
		Descriptor element = descriptor.getElement();

		List<Object> result = Lists.newArrayList();
		for (Object o : object) {
			Object s = serialize(element, o);
			result.add(s);
		}

		return result;
	}

	@Override
	protected Set<Object> serializeSet(final SetDescriptor descriptor, final Set<?> object) {
		if (object == null) return null;
		Descriptor element = descriptor.getElement();

		Set<Object> result = Sets.newLinkedHashSet();
		for (Object o : object) {
			Object s = serialize(element, o);
			result.add(s);
		}

		return result;
	}

	@Override
	protected Map<Object, Object> serializeMap(final MapDescriptor descriptor,
			final Map<?, ?> object) {
		if (object == null) return null;
		Descriptor key = descriptor.getKey();
		Descriptor val = descriptor.getValue();

		Map<Object, Object> result = Maps.newLinkedHashMap();
		for (Map.Entry<?, ?> e : object.entrySet()) {
			Object k = serialize(key, e.getKey());
			Object v = serialize(val, e.getValue());
			result.put(k, v);
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
