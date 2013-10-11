package io.pdef.format;

import static com.google.common.base.Preconditions.*;
import io.pdef.Message;
import io.pdef.descriptors.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractFormat<F> implements Format {

	public <T> F serialize(final DataDescriptor<T> descriptor, final T object) throws FormatException {
		checkNotNull(descriptor);

		try {
			return doSerialize(descriptor, object);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected <T> F doSerialize(final DataDescriptor<T> descriptor, final T object) throws Exception {
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
				return (F) serializeList((ListDescriptor) descriptor, (List) object);
			case SET:
				return (F) serializeSet((SetDescriptor) descriptor, (Set) object);
			case MAP:
				return (F) serializeMap((MapDescriptor) descriptor, (Map) object);
			case ENUM:
				return (F) serializeEnum((EnumDescriptor) descriptor, (Enum) object);
			case MESSAGE:
			case EXCEPTION:
				return (F) serializeMessage((MessageDescriptor) descriptor, (Message) object);
			case OBJECT:
				return serializeObject(object);
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

	protected abstract <E> F serializeList(final ListDescriptor<E> descriptor, final List<E> list)
			throws Exception;

	protected abstract <E> F serializeSet(final SetDescriptor<E> descriptor, final Set<E> set)
			throws Exception;

	protected abstract <K, V> F serializeMap(final MapDescriptor<K, V> descriptor, final Map<K, V> map)
			throws Exception;

	protected abstract <E extends Enum<E>> F serializeEnum(final EnumDescriptor<E> descriptor,
			final E value) throws Exception;

	protected abstract <M extends Message> F serializeMessage(final MessageDescriptor<M> descriptor,
			final M message) throws Exception;

	protected abstract F serializeObject(final Object object);

	public <T> T parse(final DataDescriptor<T> descriptor, final F input) throws FormatException {
		checkNotNull(descriptor);

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
				return (T) parseList((ListDescriptor<?>) descriptor, input);
			case SET:
				return (T) parseSet((SetDescriptor<?>) descriptor, input);
			case MAP:
				return (T) parseMap((MapDescriptor<?, ?>) descriptor, input);
			case ENUM:
				return (T) parseEnum((EnumDescriptor<?>) descriptor, input);
			case MESSAGE:
			case EXCEPTION:
				return (T) parseMessage((MessageDescriptor<?>) descriptor, input);
			case OBJECT:
				return (T) parseObject(input);
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

	protected abstract <E> List<E> parseList(final ListDescriptor<E> descriptor, final F input)
			throws Exception;

	protected abstract <E> Set<E> parseSet(final SetDescriptor<E> descriptor, final F input)
			throws Exception;

	protected abstract <K, V> Map<K, V> parseMap(final MapDescriptor<K, V> descriptor, final F input)
			throws Exception;

	protected abstract <T extends Enum<T>> T parseEnum(final EnumDescriptor<T> descriptor,
			final F input) throws Exception;

	protected abstract <M extends Message> M parseMessage(final MessageDescriptor<M> descriptor,
			final F input) throws Exception;

	protected abstract Object parseObject(final F input) throws Exception;
}
