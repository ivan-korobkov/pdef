package io.pdef.format;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.pdef.Message;
import io.pdef.descriptors.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NativeFormat extends AbstractFormat<Object> {
	private static final NativeFormat INSTANCE = new NativeFormat();

	public static NativeFormat instance() {
		return INSTANCE;
	}

	private NativeFormat() {}

	// Serializing.

	@Override
	protected Boolean serializeBoolean(final Boolean object) {
		return object;
	}

	@Override
	protected Short serializeShort(final Short object) {
		return object;
	}

	@Override
	protected Integer serializeInt(final Integer object) {
		return object;
	}

	@Override
	protected Long serializeLong(final Long object) {
		return object;
	}

	@Override
	protected Float serializeFloat(final Float object) {
		return object;
	}

	@Override
	protected Double serializeDouble(final Double object) {
		return object;
	}

	@Override
	protected String serializeString(final String object) {
		return object;
	}

	@Override
	protected <E> List<Object> serializeList(final List<E> list, final ListDescriptor<E> descriptor)
			throws Exception {
		if (list == null) {
			return null;
		}

		DataDescriptor<E> element = descriptor.getElement();
		List<Object> result = Lists.newArrayList();

		for (E elem : list) {
			Object serialized = doSerialize(elem, element);
			result.add(serialized);
		}

		return result;
	}

	@Override
	protected <E> Set<Object> serializeSet(final Set<E> set, final SetDescriptor<E> descriptor)
			throws Exception {
		if (set == null) {
			return null;
		}

		DataDescriptor<E> element = descriptor.getElement();
		Set<Object> result = Sets.newHashSet();
		for (E elem : set) {
			Object serialized = doSerialize(elem, element);
			result.add(serialized);
		}

		return result;
	}

	@Override
	protected <K, V> Map<Object, Object> serializeMap(final Map<K, V> map,
			final MapDescriptor<K, V> descriptor) throws Exception {
		if (map == null) {
			return null;
		}

		DataDescriptor<K> key = descriptor.getKey();
		DataDescriptor<V> value = descriptor.getValue();
		Map<Object, Object> result = Maps.newHashMap();

		for (Map.Entry<K, V> e : map.entrySet()) {
			Object k = doSerialize(e.getKey(), key);
			Object v = doSerialize(e.getValue(), value);
			result.put(k, v);
		}

		return result;
	}

	@Override
	protected <E extends Enum<E>> E serializeEnum(final E value, final EnumDescriptor<E> descriptxr) {
		return value;
	}

	@Override
	protected <M extends Message> Map<String, Object> serializeMessage(
			final M message, final MessageDescriptor<M> descriptor) throws Exception {
		if (message == null) {
			return null;
		}

		// Mind polymorphic messages.
		@SuppressWarnings("unchecked")
		MessageDescriptor<M> polymorphicType = (MessageDescriptor<M>) message.descriptor();
		Map<String, Object> result = Maps.newLinkedHashMap();

		for (FieldDescriptor<? super M, ?> field : polymorphicType.getFields()) {
			@SuppressWarnings("unchecked")
			FieldDescriptor<M, ?> uncheckedField = (FieldDescriptor<M, ?>) field;
			serializeField(uncheckedField, message, result);
		}

		return result;
	}

	private <M extends Message, V> void serializeField(final FieldDescriptor<M, V> field,
			final M message, final Map<String, Object> map) throws Exception {
		V value = field.get(message);
		if (value == null) {
			return;
		}

		Object serialized = doSerialize(value, field.getType());
		map.put(field.getName(), serialized);
	}

	@Override
	protected Object serializeObject(final Object object) {
		return object;
	}

	// Parsing.

	@Override
	protected Boolean parseBoolean(final Object input) {
		if (input == null) {
			return null;
		} else if (input instanceof Boolean) {
			return (Boolean) input;
		} else if (input instanceof String) {
			return Boolean.parseBoolean((String) input);
		}
		throw new FormatException("Cannot parse a boolean from " + input);
	}

	@Override
	protected Short parseShort(final Object input) {
		if (input == null) {
			return null;
		} else if (input instanceof Number) {
			return ((Number) input).shortValue();
		} else if (input instanceof String) {
			return Short.parseShort((String) input);
		}
		throw new FormatException("Cannot parse a short from " + input);
	}

	@Override
	protected Integer parseInt(final Object input) {
		if (input == null) {
			return null;
		} else if (input instanceof Number) {
			return ((Number) input).intValue();
		} else if (input instanceof String) {
			return Integer.parseInt((String) input);
		}
		throw new FormatException("Cannot parse an int from " + input);
	}

	@Override
	protected Long parseLong(final Object input) {
		if (input == null) {
			return null;
		} else if (input instanceof Number) {
			return ((Number) input).longValue();
		} else if (input instanceof String) {
			return Long.parseLong((String) input);
		}
		throw new FormatException("Cannot parse a long from " + input);
	}

	@Override
	protected Float parseFloat(final Object input) {
		if (input == null) {
			return null;
		} else if (input instanceof Number) {
			return ((Number) input).floatValue();
		} else if (input instanceof String) {
			return Float.parseFloat((String) input);
		}
		throw new FormatException("Cannot parse a float from " + input);
	}

	@Override
	protected Double parseDouble(final Object input) {
		if (input == null) {
			return null;
		} else if (input instanceof Number) {
			return ((Number) input).doubleValue();
		} else if (input instanceof String) {
			return Double.parseDouble((String) input);
		}
		throw new FormatException("Cannot parse a double from " + input);
	}

	@Override
	protected String parseString(final Object input) {
		if (input == null) {
			return null;
		} else if (input instanceof String) {
			return (String) input;
		}
		throw new FormatException("Cannot parse a string from " + input);
	}

	@Override
	protected <E> List<E> parseList(final Object input, final ListDescriptor<E> descriptor)
			throws Exception {
		if (input == null) {
			return null;
		} else if (!(input instanceof Collection)) {
			throw new FormatException("Cannot parse a list from " + input);
		}

		Collection<?> collection = (Collection<?>) input;
		DataDescriptor<E> element = descriptor.getElement();
		List<E> result = Lists.newArrayList();

		for (Object elem : collection) {
			E parsed = doParse(element, elem);
			result.add(parsed);
		}

		return result;
	}

	@Override
	protected <E> Set<E> parseSet(final Object input, final SetDescriptor<E> descriptor) throws Exception {
		if (input == null) {
			return null;
		} else if (!(input instanceof Collection)) {
			throw new FormatException("Cannot parse a set from " + input);
		}

		Collection<?> collection = (Collection<?>) input;
		Set<E> result = Sets.newHashSet();
		DataDescriptor<E> element = descriptor.getElement();

		for (Object elem : collection) {
			E parsed = doParse(element, elem);
			result.add(parsed);
		}

		return result;
	}

	@Override
	protected <K, V> Map<K, V> parseMap(final Object input, final MapDescriptor<K, V> descriptor)
			throws Exception {
		if (input == null) {
			return null;
		} else if (!(input instanceof Map)) {
			throw new FormatException("Cannot parse a map from " + input);
		}

		Map<?, ?> map = (Map<?, ?>) input;
		Map<K, V> result = Maps.newHashMap();
		DataDescriptor<K> key = descriptor.getKey();
		DataDescriptor<V> value = descriptor.getValue();

		for (Map.Entry<?, ?> e : map.entrySet()) {
			K k = doParse(key, e.getKey());
			V v = doParse(value, e.getValue());
			result.put(k, v);
		}

		return result;
	}

	@Override
	protected <T extends Enum<T>> T parseEnum(final Object input,
			final EnumDescriptor<T> descriptor) {
		if (input == null) {
			return null;
		} else if (input instanceof Enum<?>) {
			return descriptor.getJavaClass().cast(input);
		} else if (input instanceof String) {
			return descriptor.getNamesToValues().get(((String) input).toUpperCase());
		}
		throw new FormatException("Cannot parse an enum from " + input);
	}

	@Override
	protected <M extends Message> M parseMessage(final Object input,
			MessageDescriptor<M> descriptor)
			throws Exception {
		if (input == null) {
			return null;
		} else if (!(input instanceof Map)) {
			throw new FormatException("Cannot parse a map from " + input);
		}

		Map<?, ?> map = (Map<?, ?>) input;
		FieldDescriptor<? super M, ?> discriminator = descriptor.getDiscriminator();

		// Mind polymorphic messages.
		if (discriminator != null) {
			Object fieldValue = map.get(discriminator.getName());
			if (fieldValue != null) {
				Object discriminatorValue = doParse(discriminator.getType(), fieldValue);
				descriptor = descriptor.findSubtypeOrThis(discriminatorValue);
			}
		}

		M message = descriptor.newInstance();
		for (FieldDescriptor<? super M, ?> field : descriptor.getFields()) {
			@SuppressWarnings("unchecked")
			FieldDescriptor<M, ?> uncheckedField = (FieldDescriptor<M, ?>) field;
			parseField(uncheckedField, message, map);
		}

		return message;
	}

	private <M extends Message, V> void parseField(final FieldDescriptor<M, V> field, final M message,
			final Map<?, ?> map) throws Exception {
		Object fieldInput = map.get(field.getName());
		V value = doParse(field.getType(), fieldInput);
		field.set(message, value);
	}

	@Override
	protected Object parseObject(final Object input) {
		return input;
	}
}
