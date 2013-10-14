package io.pdef.format;

import io.pdef.Message;
import io.pdef.descriptors.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractFormat<F> implements Format {

	public <T> F serialize(final T object, final DataDescriptor<T> descriptor) throws FormatException {
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
	protected <T> F doSerialize(final T object, final DataDescriptor<T> descriptor) throws Exception {
		TypeEnum typeEnum = descriptor.getType();
		switch (typeEnum) {
			case BOOL:
				return serializeBoolean((Boolean) object);
			case INT16:
				return serializeShort((Short) object);
			case INT32:
				return serializeInt((Integer) object);
			case INT64:
				return serializeLong((Long) object);
			case FLOAT:
				return serializeFloat((Float) object);
			case DOUBLE:
				return serializeDouble((Double) object);
			case STRING:
				return serializeString((String) object);
			case LIST:
				return (F) serializeList((List) object, (ListDescriptor) descriptor);
			case SET:
				return (F) serializeSet((Set) object, (SetDescriptor) descriptor);
			case MAP:
				return (F) serializeMap((Map) object, (MapDescriptor) descriptor);
			case ENUM:
				return (F) serializeEnum((Enum) object, (EnumDescriptor) descriptor);
			case MESSAGE:
			case EXCEPTION:
				return (F) serializeMessage((Message) object, (MessageDescriptor) descriptor);
			case VOID:
				return null;
			default:
				throw new IllegalArgumentException("Unsupported descriptor " + descriptor);
		}
	}

	protected abstract F serializeBoolean(final Boolean value) throws Exception;

	protected abstract F serializeShort(final Short value) throws Exception;

	protected abstract F serializeInt(final Integer value) throws Exception;

	protected abstract F serializeLong(final Long value) throws Exception;

	protected abstract F serializeFloat(final Float value) throws Exception;

	protected abstract F serializeDouble(final Double value) throws Exception;

	protected abstract F serializeString(final String value) throws Exception;

	protected abstract <E> F serializeList(final List<E> list, final ListDescriptor<E> descriptor)
			throws Exception;

	protected abstract <E> F serializeSet(final Set<E> set, final SetDescriptor<E> descriptor)
			throws Exception;

	protected abstract <K, V> F serializeMap(final Map<K, V> map,
			final MapDescriptor<K, V> descriptor) throws Exception;

	protected abstract <E extends Enum<E>> F serializeEnum(final E value,
			final EnumDescriptor<E> descriptor) throws Exception;

	protected abstract <M extends Message> F serializeMessage(final M message,
			final MessageDescriptor<M> descriptor) throws Exception;

	public <T> T parse(final F input, final DataDescriptor<T> descriptor) throws FormatException {
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
	protected <T> T doParse(final DataDescriptor<T> descriptor, final F input) throws Exception {
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

	protected abstract Boolean parseBoolean(final F input) throws Exception;

	protected abstract Short parseShort(final F input) throws Exception;

	protected abstract Integer parseInt(final F input) throws Exception;

	protected abstract Long parseLong(final F input) throws Exception;

	protected abstract Float parseFloat(final F input) throws Exception;

	protected abstract Double parseDouble(final F input) throws Exception;

	protected abstract Object parseString(final Object input) throws Exception;

	protected abstract <E> List<E> parseList(final F input, final ListDescriptor<E> descriptor)
			throws Exception;

	protected abstract <E> Set<E> parseSet(final F input, final SetDescriptor<E> descriptor)
			throws Exception;

	protected abstract <K, V> Map<K, V> parseMap(final F input,
			final MapDescriptor<K, V> descriptor) throws Exception;

	protected abstract <T extends Enum<T>> T parseEnum(final F input,
			final EnumDescriptor<T> descriptor) throws Exception;

	protected abstract <M extends Message> M parseMessage(final F input,
			final MessageDescriptor<M> descriptor) throws Exception;

	protected abstract Object parseObject(final F input) throws Exception;
}
