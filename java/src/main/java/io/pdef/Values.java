package io.pdef;

import java.util.*;

/**
 * Pdef values utility methods.
 */
public class Values {
	private Values() {}

	/**
	 * Returns a deep copy of a pdef value (a primitive, a collection, or a message).
	 */
	public static <T> T copy(final T object) {
		if (object == null) return null;
		TypeEnum type = TypeEnum.valueTypeOf(object.getClass());
		return copy(type, object);
	}

	/**
	 * Returns a deep copy of a pdef message.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Message> T copy(final T message) {
		return message == null ? null : (T) message.copy();
	}

	/**
	 * Returns a deep copy of a pdef list.
	 */
	public static <T> List<T> copy(final List<T> list) {
		if (list == null) {
			return null;
		}

		TypeEnum elementType = null;
		List<T> copy = new ArrayList<T>();
		for (T element : list) {
			T elementCopy = null;
			if (element != null) {
				if (elementType == null) {
					elementType = TypeEnum.valueTypeOf(element.getClass());
				}
				elementCopy = copy(elementType, element);
			}

			copy.add(elementCopy);
		}

		return copy;
	}

	/**
	 * Returns a deep copy of a pdef set.
	 */
	public static <T> Set<T> copy(final Set<T> set) {
		if (set == null) {
			return null;
		}

		TypeEnum elementType = null;
		Set<T> copy = new HashSet<T>();
		for (T element : set) {
			T elementCopy = null;
			if (element != null) {
				if (elementType == null) {
					elementType = TypeEnum.valueTypeOf(element.getClass());
				}
				elementCopy = copy(elementType, element);
			}

			copy.add(elementCopy);
		}

		return copy;
	}

	/**
	 * Returns a deep copy of a pdef map.
	 */
	public static <K, V> Map<K, V> copy(final Map<K, V> map) {
		if (map == null) {
			return null;
		}

		TypeEnum keyType = null;
		TypeEnum valueType = null;

		Map<K, V> copy = new HashMap<K, V>();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			K key = entry.getKey();
			V value = entry.getValue();

			K keyCopy = null;
			if (key != null) {
				if (keyType == null) {
					keyType = TypeEnum.valueTypeOf(key.getClass());
				}
				keyCopy = copy(keyType, key);
			}

			V valueCopy = null;
			if (value != null) {
				if (valueType == null) {
					valueType = TypeEnum.valueTypeOf(value.getClass());
				}
				valueCopy = copy(valueType, value);
			}

			copy.put(keyCopy, valueCopy);
		}

		return copy;
	}

	@SuppressWarnings("unchecked")
	private static <T> T copy(final TypeEnum type, final T object) {
		if (type == null) {
			throw new NullPointerException("type");
		}
		if (object == null) {
			return null;
		}

		switch (type) {
			case LIST: return (T) copy((List<?>) object);
			case SET: return (T) copy((Set<?>) object);
			case MAP: return (T) copy((Map<?, ?>) object);
			case MESSAGE: return (T) ((Message) object).copy();
			default: return object;
		}
	}
}
