package pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.*;

public class ImmutableSymbolTable<T extends Symbol> implements SymbolTable<T> {
	private final ImmutableList<T> list;
	private final ImmutableSet<T> set;
	private final ImmutableMap<String, T> map;

	public static <T extends Symbol> ImmutableSymbolTable<T> of(final T... elements) {
		return new ImmutableSymbolTable<T>(Arrays.asList(elements));
	}

	public static <T extends Symbol> ImmutableSymbolTable<T> copyOf(final Iterable<T> iterable) {
		return new ImmutableSymbolTable<T>(iterable);
	}

	private ImmutableSymbolTable(final Iterable<T> iterable) {
		list = ImmutableList.copyOf(iterable);
		set = ImmutableSet.copyOf(list);

		ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
		for (T symbol : list) {
			builder.put(symbol.getName(), symbol);
		}
		map = builder.build();
	}

	@Override
	public List<T> list() {
		return list;
	}

	@Override
	public Set<T> set() {
		return set;
	}

	@Override
	public Map<String, T> map() {
		return map;
	}

	@Override
	public Iterator<T> iterator() {
		return list.iterator();
	}
}
