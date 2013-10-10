package io.pdef.format;

import static com.google.common.base.Preconditions.*;
import io.pdef.Message;
import io.pdef.meta.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractFormat<F> implements Format {

	public <T> F serialize(final DataType<T> metaType, final T object) throws FormatException {
		checkNotNull(metaType);

		try {
			return doSerialize(metaType, object);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected <T> F doSerialize(final DataType<T> metaType, final T object) throws Exception {
		TypeEnum typeEnum = metaType.getType();
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
				return (F) serializeList((ListType) metaType, (List) object);
			case SET:
				return (F) serializeSet((SetType) metaType, (Set) object);
			case MAP:
				return (F) serializeMap((MapType) metaType, (Map) object);
			case ENUM:
				return (F) serializeEnum((EnumType) metaType, (Enum) object);
			case MESSAGE:
			case EXCEPTION:
				return (F) serializeMessage((MessageType) metaType, (Message) object);
			case OBJECT:
				return serializeObject(object);
			default:
				throw new IllegalArgumentException("Unsupported meta type " + metaType);
		}
	}

	protected abstract F serializeBoolean(final Boolean value) throws Exception;

	protected abstract F serializeShort(final Short value) throws Exception;

	protected abstract F serializeInt(final Integer value) throws Exception;

	protected abstract F serializeLong(final Long value) throws Exception;

	protected abstract F serializeFloat(final Float value) throws Exception;

	protected abstract F serializeDouble(final Double value) throws Exception;

	protected abstract F serializeString(final String value) throws Exception;

	protected abstract <E> F serializeList(final ListType<E> metaType, final List<E> list)
			throws Exception;

	protected abstract <E> F serializeSet(final SetType<E> metaType, final Set<E> set)
			throws Exception;

	protected abstract <K, V> F serializeMap(final MapType<K, V> metaType, final Map<K, V> map)
			throws Exception;

	protected abstract <E extends Enum<E>> F serializeEnum(final EnumType<E> metaType,
			final E value) throws Exception;

	protected abstract <M extends Message> F serializeMessage(final MessageType<M> metaType,
			final M message) throws Exception;

	protected abstract F serializeObject(final Object object);

	public <T> T parse(final DataType<T> metaType, final F input) throws FormatException {
		checkNotNull(metaType);

		try {
			return doParse(metaType, input);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected <T> T doParse(final DataType<T> metaType, final F input) throws Exception {
		TypeEnum typeEnum = metaType.getType();
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
				return (T) parseList((ListType<?>) metaType, input);
			case SET:
				return (T) parseSet((SetType<?>) metaType, input);
			case MAP:
				return (T) parseMap((MapType<?, ?>) metaType, input);
			case ENUM:
				return (T) parseEnum((EnumType<?>) metaType, input);
			case MESSAGE:
			case EXCEPTION:
				return (T) parseMessage((MessageType<?>) metaType, input);
			case OBJECT:
				return (T) parseObject(input);
			default:
				throw new IllegalArgumentException("Unsupported meta type " + metaType);
		}
	}

	protected abstract Boolean parseBoolean(final F input) throws Exception;

	protected abstract Short parseShort(final F input) throws Exception;

	protected abstract Integer parseInt(final F input) throws Exception;

	protected abstract Long parseLong(final F input) throws Exception;

	protected abstract Float parseFloat(final F input) throws Exception;

	protected abstract Double parseDouble(final F input) throws Exception;

	protected abstract Object parseString(final Object input) throws Exception;

	protected abstract <E> List<E> parseList(final ListType<E> metaType, final F input)
			throws Exception;

	protected abstract <E> Set<E> parseSet(final SetType<E> metaType, final F input)
			throws Exception;

	protected abstract <K, V> Map<K, V> parseMap(final MapType<K, V> metaType, final F input)
			throws Exception;

	protected abstract <T extends Enum<T>> T parseEnum(final EnumType<T> metaType,
			final F input) throws Exception;

	protected abstract <M extends Message> M parseMessage(final MessageType<M> metaType,
			final F input) throws Exception;

	protected abstract Object parseObject(final F input) throws Exception;
}
