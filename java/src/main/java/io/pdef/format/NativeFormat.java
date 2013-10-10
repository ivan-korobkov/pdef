package io.pdef.format;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.pdef.Message;
import io.pdef.meta.*;

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
	protected <E> List<Object> serializeList(final ListType<E> metaType, final List<E> list)
			throws Exception {
		if (list == null) {
			return null;
		}

		DataType<E> element = metaType.getElement();
		List<Object> result = Lists.newArrayList();

		for (E elem : list) {
			Object serialized = doSerialize(element, elem);
			result.add(serialized);
		}

		return result;
	}

	@Override
	protected <E> Set<Object> serializeSet(final SetType<E> metaType, final Set<E> set)
			throws Exception {
		if (set == null) {
			return null;
		}

		DataType<E> element = metaType.getElement();
		Set<Object> result = Sets.newHashSet();
		for (E elem : set) {
			Object serialized = doSerialize(element, elem);
			result.add(serialized);
		}

		return result;
	}

	@Override
	protected <K, V> Map<Object, Object> serializeMap(final MapType<K, V> metaType,
			final Map<K, V> map) throws Exception {
		if (map == null) {
			return null;
		}

		DataType<K> key = metaType.getKey();
		DataType<V> value = metaType.getValue();
		Map<Object, Object> result = Maps.newHashMap();

		for (Map.Entry<K, V> e : map.entrySet()) {
			Object k = doSerialize(key, e.getKey());
			Object v = doSerialize(value, e.getValue());
			result.put(k, v);
		}

		return result;
	}

	@Override
	protected <E extends Enum<E>> E serializeEnum(final EnumType<E> metaType, final E value) {
		return value;
	}

	@Override
	protected <M extends Message> Map<String, Object> serializeMessage(
			final MessageType<M> metaType, final M message) throws Exception {
		if (message == null) {
			return null;
		}

		// Mind polymorphic messages.
		@SuppressWarnings("unchecked")
		MessageType<M> polymorphicType = (MessageType<M>) message.metaType();
		Map<String, Object> result = Maps.newLinkedHashMap();

		for (MessageField<? super M, ?> field : polymorphicType.getFields()) {
			serializeField(field, message, result);
		}

		return result;
	}

	private <M extends Message, V> void serializeField(final MessageField<M, V> field,
			final M message, final Map<String, Object> map) throws Exception {
		V value = field.get(message);
		if (value == null) {
			return;
		}

		Object serialized = doSerialize(field.getType(), value);
		map.put(field.getName(), serialized);
	}

	@Override
	protected Object serializeObject(final Object object) {
		return object;
	}

	// Parsing.

	@Override
	protected Boolean parseBoolean(final Object input) {
		return input instanceof String ? Boolean.parseBoolean((String) input) : (Boolean) input;
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
	protected <E> List<E> parseList(final ListType<E> metaType, final Object input)
			throws Exception {
		if (input == null) {
			return null;
		} else if (!(input instanceof Collection)) {
			throw new FormatException("Cannot parse a list from " + input);
		}

		Collection<?> collection = (Collection<?>) input;
		DataType<E> element = metaType.getElement();
		List<E> result = Lists.newArrayList();

		for (Object elem : collection) {
			E parsed = doParse(element, elem);
			result.add(parsed);
		}

		return result;
	}

	@Override
	protected <E> Set<E> parseSet(final SetType<E> metaType, final Object input) throws Exception {
		if (input == null) {
			return null;
		} else if (!(input instanceof Collection)) {
			throw new FormatException("Cannot parse a set from " + input);
		}

		Collection<?> collection = (Collection<?>) input;
		Set<E> result = Sets.newHashSet();
		DataType<E> element = metaType.getElement();

		for (Object elem : collection) {
			E parsed = doParse(element, elem);
			result.add(parsed);
		}

		return result;
	}

	@Override
	protected <K, V> Map<K, V> parseMap(final MapType<K, V> metaType, final Object input)
			throws Exception {
		if (input == null) {
			return null;
		} else if (!(input instanceof Map)) {
			throw new FormatException("Cannot parse a map from " + input);
		}

		Map<?, ?> map = (Map<?, ?>) input;
		Map<K, V> result = Maps.newHashMap();
		DataType<K> key = metaType.getKey();
		DataType<V> value = metaType.getValue();

		for (Map.Entry<?, ?> e : map.entrySet()) {
			K k = doParse(key, e.getKey());
			V v = doParse(value, e.getValue());
			result.put(k, v);
		}

		return result;
	}

	@Override
	protected <T extends Enum<T>> T parseEnum(final EnumType<T> metaType, final Object input) {
		if (input == null) {
			return null;
		} else if (input instanceof Enum<?>) {
			return metaType.getJavaClass().cast(input);
		} else if (input instanceof String) {
			return metaType.getNamesToValues().get(input);
		}
		throw new FormatException("Cannot parse an enum from " + input);
	}

	@Override
	protected <M extends Message> M parseMessage(MessageType<M> metaType, final Object input)
			throws Exception {
		if (input == null) {
			return null;
		} else if (!(input instanceof Map)) {
			throw new FormatException("Cannot parse a map from " + input);
		}

		Map<?, ?> map = (Map<?, ?>) input;
		MessageField<? super M, ?> discriminator = metaType.getDiscriminator();

		// Mind polymorphic messages.
		if (discriminator != null) {
			Object fieldValue = map.get(discriminator.getName());
			if (fieldValue != null) {
				Object discriminatorValue = doParse(discriminator.getType(), fieldValue);
				metaType = metaType.findSubtypeOrThis(discriminatorValue);
			}
		}

		M message = metaType.newInstance();
		for (MessageField<? super M, ?> field : metaType.getFields()) {
			parseField(field, message, map);
		}

		return message;
	}

	private <M extends Message, V> void parseField(final MessageField<M, V> field, final M message,
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
