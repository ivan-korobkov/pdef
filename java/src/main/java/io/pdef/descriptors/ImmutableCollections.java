package io.pdef.descriptors;

import java.util.*;

class ImmutableCollections {
	private ImmutableCollections() {}

	static <T> List<T> list(final T... elements) {
		return list(Arrays.asList(elements));
	}

	static <T> List<T> list(final List<T> list) {
		return Collections.unmodifiableList(new ArrayList<T>(list));
	}

	static <T> Set<T> set(final Set<T> set) {
		return Collections.unmodifiableSet(new LinkedHashSet<T>(set));
	}

	public static <K, V> Map<K, V> map(final Map<K, V> map) {
		return Collections.unmodifiableMap(new LinkedHashMap<K, V>(map));
	}
}
