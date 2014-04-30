/*
 * Copyright: 2013 Pdef <http://pdef.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pdef;

import java.util.*;

/** Returns deep copies of pdef types. */
public class PdefCopy {
	private PdefCopy() {}

	@SuppressWarnings("unchecked")
	public static <T> T copy(T object) {
		if (object == null) return null;
		else if (object instanceof Struct) return (T) copy((Struct) object);
		else if (object instanceof List) return (T) copy((List<?>) object);
		else if (object instanceof Set) return (T) copy((Set<?>) object);
		else if (object instanceof Map) return (T) copy((Map<?, ?>) object);
		else if (object instanceof Date) return (T) copy((Date) object);
		return object;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Struct> T copy(T struct) {
		return struct == null ? null : (T) struct.copy();
	}

	public static <T> List<T> copy(List<T> list) {
		if (list == null) {
			return null;
		}

		List<T> copy = new ArrayList<T>();
		for (T element : list) {
			T elementCopy = copy(element);
			copy.add(elementCopy);
		}

		return copy;
	}

	public static <T> Set<T> copy(Set<T> set) {
		if (set == null) {
			return null;
		}

		Set<T> copy = new HashSet<T>();
		for (T element : set) {
			T elementCopy = copy(element);
			copy.add(elementCopy);
		}

		return copy;
	}

	public static <K, V> Map<K, V> copy(Map<K, V> map) {
		if (map == null) {
			return null;
		}

		Map<K, V> copy = new HashMap<K, V>();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			K keyCopy = copy(entry.getKey());
			V valueCopy = copy(entry.getValue());
			copy.put(keyCopy, valueCopy);
		}

		return copy;
	}

	public static Date copy(Date date) {
		if (date == null) {
			return null;
		}

		return new Date(date.getTime());
	}
}
