package io.pdef.formats;

import io.pdef.*;
import io.pdef.descriptors.*;

import java.util.*;

public class NativeFormat {
	private static final NativeFormat INSTANCE = new NativeFormat();

	public static NativeFormat getInstance() {
		return INSTANCE;
	}

	private NativeFormat() {}

	// Serializing.

	public <T> Object serialize(final T object, final DataTypeDescriptor<T> descriptor) throws FormatException {
		if (descriptor == null) throw new NullPointerException("descriptor");

		try {
			return doSerialize(object, descriptor);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Object doSerialize(final T object, final DataTypeDescriptor<T> descriptor) throws Exception {
		if (object == null) {
			return null;
		}

		TypeEnum typeEnum = descriptor.getType();
		switch (typeEnum) {
			case BOOL:
			case INT16:
			case INT32:
			case INT64:
			case FLOAT:
			case DOUBLE:
			case STRING:
				return object;
			case LIST:
				return serializeList((List) object, (ListDescriptor) descriptor);
			case SET:
				return serializeSet((Set) object, (SetDescriptor) descriptor);
			case MAP:
				return serializeMap((Map) object, (MapDescriptor) descriptor);
			case ENUM:
				return serializeEnum((Enum) object);
			case MESSAGE:
			case EXCEPTION:
				return serializeMessage((Message) object);
			case VOID:
				return null;
			default:
				throw new IllegalArgumentException("Unsupported descriptor " + descriptor);
		}
	}

	private <E> List<Object> serializeList(final List<E> list, final ListDescriptor<E> descriptor)
			throws Exception {
		if (list == null) {
			return null;
		}

		DataTypeDescriptor<E> element = descriptor.getElement();
		List<Object> result = new ArrayList<Object>();

		for (E elem : list) {
			Object serialized = doSerialize(elem, element);
			result.add(serialized);
		}

		return result;
	}

	private <E> Set<Object> serializeSet(final Set<E> set, final SetDescriptor<E> descriptor)
			throws Exception {
		if (set == null) {
			return null;
		}

		DataTypeDescriptor<E> element = descriptor.getElement();
		Set<Object> result = new HashSet<Object>();
		for (E elem : set) {
			Object serialized = doSerialize(elem, element);
			result.add(serialized);
		}

		return result;
	}

	private <K, V> Map<Object, Object> serializeMap(final Map<K, V> map,
			final MapDescriptor<K, V> descriptor) throws Exception {
		if (map == null) {
			return null;
		}

		DataTypeDescriptor<K> key = descriptor.getKey();
		DataTypeDescriptor<V> value = descriptor.getValue();
		Map<Object, Object> result = new HashMap<Object, Object>();

		for (Map.Entry<K, V> e : map.entrySet()) {
			Object k = doSerialize(e.getKey(), key);
			Object v = doSerialize(e.getValue(), value);
			result.put(k, v);
		}

		return result;
	}

	private <E extends Enum<E>> E serializeEnum(final E value) {
		return value;
	}

	private <M extends Message> Map<String, Object> serializeMessage(final M message)
			throws Exception {
		if (message == null) {
			return null;
		}

		// Mind polymorphic messages.
		@SuppressWarnings("unchecked")
		MessageDescriptor<M> polymorphicType = (MessageDescriptor<M>) message.descriptor();
		Map<String, Object> result = new LinkedHashMap<String, Object>();

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

	// Parsing.

	public <T> T parse(final Object input, final DataTypeDescriptor<T> descriptor) throws FormatException {
		if (descriptor == null) throw new NullPointerException("descriptor");

		try {
			return doParse(descriptor, input);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T doParse(final DataTypeDescriptor<T> descriptor, final Object input) throws Exception {
		if (input == null) {
			return null;
		}

		TypeEnum typeEnum = descriptor.getType();
		switch (typeEnum) {
			case BOOL:
				return (T) parseBoolean(input);
			case INT16:
				return (T) parseShort(input);
			case INT32:
				return (T) parseInt(input);
			case INT64:
				return (T) parseLong(input);
			case FLOAT:
				return (T) parseFloat(input);
			case DOUBLE:
				return (T) parseDouble(input);
			case STRING:
				return (T) parseString(input);
			case LIST:
				return (T) parseList(input, (ListDescriptor<?>) descriptor);
			case SET:
				return (T) parseSet(input, (SetDescriptor<?>) descriptor);
			case MAP:
				return (T) parseMap(input, (MapDescriptor<?, ?>) descriptor);
			case ENUM:
				return (T) parseEnum(input, (EnumDescriptor<?>) descriptor);
			case MESSAGE:
			case EXCEPTION:
				return (T) parseMessage(input, (MessageDescriptor<?>) descriptor);
			case VOID:
				return null;
			default:
				throw new IllegalArgumentException("Unsupported descriptor " + descriptor);
		}
	}

	private Boolean parseBoolean(final Object input) {
		if (input instanceof Boolean) {
			return (Boolean) input;
		} else if (input instanceof String) {
			return Boolean.parseBoolean((String) input);
		}
		throw new FormatException("Cannot fromJson a boolean from " + input);
	}

	private Short parseShort(final Object input) {
		if (input instanceof Number) {
			return ((Number) input).shortValue();
		} else if (input instanceof String) {
			return Short.parseShort((String) input);
		}
		throw new FormatException("Cannot fromJson a short from " + input);
	}

	private Integer parseInt(final Object input) {
		if (input instanceof Number) {
			return ((Number) input).intValue();
		} else if (input instanceof String) {
			return Integer.parseInt((String) input);
		}
		throw new FormatException("Cannot fromJson an int from " + input);
	}

	private Long parseLong(final Object input) {
		if (input instanceof Number) {
			return ((Number) input).longValue();
		} else if (input instanceof String) {
			return Long.parseLong((String) input);
		}
		throw new FormatException("Cannot fromJson a long from " + input);
	}

	private Float parseFloat(final Object input) {
		if (input instanceof Number) {
			return ((Number) input).floatValue();
		} else if (input instanceof String) {
			return Float.parseFloat((String) input);
		}
		throw new FormatException("Cannot fromJson a float from " + input);
	}

	private Double parseDouble(final Object input) {
		if (input instanceof Number) {
			return ((Number) input).doubleValue();
		} else if (input instanceof String) {
			return Double.parseDouble((String) input);
		}
		throw new FormatException("Cannot fromJson a double from " + input);
	}

	private String parseString(final Object input) {
		if (input == null) {
			return null;
		} else if (input instanceof String) {
			return (String) input;
		}
		throw new FormatException("Cannot fromJson a string from " + input);
	}

	private <E> List<E> parseList(final Object input, final ListDescriptor<E> descriptor)
			throws Exception {
		if (!(input instanceof Collection)) {
			throw new FormatException("Cannot fromJson a list from " + input);
		}

		Collection<?> collection = (Collection<?>) input;
		DataTypeDescriptor<E> element = descriptor.getElement();
		List<E> result = new ArrayList<E>();

		for (Object elem : collection) {
			E parsed = doParse(element, elem);
			result.add(parsed);
		}

		return result;
	}

	private <E> Set<E> parseSet(final Object input, final SetDescriptor<E> descriptor)
			throws Exception {
		if (!(input instanceof Collection)) {
			throw new FormatException("Cannot fromJson a set from " + input);
		}

		Collection<?> collection = (Collection<?>) input;
		Set<E> result = new HashSet<E>();
		DataTypeDescriptor<E> element = descriptor.getElement();

		for (Object elem : collection) {
			E parsed = doParse(element, elem);
			result.add(parsed);
		}

		return result;
	}

	private <K, V> Map<K, V> parseMap(final Object input, final MapDescriptor<K, V> descriptor)
			throws Exception {
		if (!(input instanceof Map)) {
			throw new FormatException("Cannot fromJson a map from " + input);
		}

		Map<?, ?> map = (Map<?, ?>) input;
		Map<K, V> result = new HashMap<K, V>();
		DataTypeDescriptor<K> key = descriptor.getKey();
		DataTypeDescriptor<V> value = descriptor.getValue();

		for (Map.Entry<?, ?> e : map.entrySet()) {
			K k = doParse(key, e.getKey());
			V v = doParse(value, e.getValue());
			result.put(k, v);
		}

		return result;
	}

	private <T extends Enum<T>> T parseEnum(final Object input,
			final EnumDescriptor<T> descriptor) {
		if (input instanceof Enum<?>) {
			return descriptor.getJavaClass().cast(input);
		} else if (input instanceof String) {
			return descriptor.getValue((String) input);
		}
		throw new FormatException("Cannot fromJson an enum from " + input);
	}

	private <M extends Message> M parseMessage(final Object input,
			MessageDescriptor<M> descriptor) throws Exception {
		if (!(input instanceof Map)) {
			throw new FormatException("Cannot fromJson a map from " + input);
		}

		Map<?, ?> map = (Map<?, ?>) input;
		FieldDescriptor<? super M, ?> discriminator = descriptor.getDiscriminator();

		// Mind polymorphic messages.
		if (discriminator != null) {
			Object fieldValue = map.get(discriminator.getName());
			if (fieldValue != null) {
				Enum<?> discriminatorValue = (Enum<?>) doParse(discriminator.getType(), fieldValue);
				@SuppressWarnings("unchecked")
				MessageDescriptor<M> subtype = (MessageDescriptor<M>) descriptor
						.getSubtype(discriminatorValue);

				descriptor = subtype != null ? subtype : descriptor;
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
}
